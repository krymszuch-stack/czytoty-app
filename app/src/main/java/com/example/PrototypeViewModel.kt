package com.example

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

data class PrototypeState(
    val snapshot: CertSnapshot? = null,
    val analysis: MessageAnalysis? = null,
    val notes: List<PhoneNote> = emptyList(),
    val events: List<ScreeningEvent> = emptyList(),
    val cloudNotes: List<CloudNote> = emptyList(),
    val protectionEnabled: Boolean = true,
    val refreshing: Boolean = false,
    val busy: Boolean = false,
    val email: String? = null,
    val cloudStatus: String = "Nie sprawdzono połączenia",
    val notice: String? = null,
    val sharedText: String = "",
    val openSms: Boolean = false,
    val dialNumber: String? = null
)

class PrototypeViewModel(application: Application) : AndroidViewModel(application) {
    private val store = LocalStore.get(application)
    private val cert = CertRepository(application)
    private val cloud = CloudRepository()
    private var lastRefreshAttempt = 0L
    private var analyzedText = ""
    private var analysisJob: Job? = null
    private val _state = MutableStateFlow(PrototypeState())
    val state = _state.asStateFlow()

    init {
        reloadLocal()
        viewModelScope.launch {
            val saved = cert.load()
            _state.update { it.copy(snapshot = saved) }
            if (saved?.isFresh() != true) refreshCert()
        }
        viewModelScope.launch {
            try { _state.update { it.copy(email = cloud.email()) } }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(cloudStatus = "Nie udało się odczytać sesji") } }
        }
    }
    fun dismissNotice() { _state.update { it.copy(notice = null) } }
    fun acceptSharedText(text: String) { clearAnalysis(); _state.update { it.copy(sharedText = text.take(20_000)) } }
    fun consumeSharedText() { _state.update { it.copy(sharedText = "") } }
    fun requestSms() { _state.update { it.copy(openSms = true) } }
    fun consumeSmsRequest() { _state.update { it.copy(openSms = false) } }
    fun acceptDial(number: String) { _state.update { it.copy(dialNumber = number.take(40)) } }
    fun consumeDial() { _state.update { it.copy(dialNumber = null) } }
    fun reloadLocal() = viewModelScope.launch(Dispatchers.IO) {
        try { _state.update { it.copy(notes = store.notes(), events = store.events(), protectionEnabled = store.protectionEnabled) } }
        catch (_: Exception) { notify("Nie udało się odczytać lokalnej bazy.") }
    }
    fun refreshCert() {
        if (_state.value.refreshing) return
        _state.update { it.copy(refreshing = true) }
        lastRefreshAttempt = System.currentTimeMillis()
        viewModelScope.launch {
            try {
                val snapshot = cert.refresh()
                _state.update { it.copy(snapshot = snapshot, analysis = null) }
                if (analyzedText.isNotBlank()) analyze(analyzedText)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { notify("Nie udało się pobrać listy CERT. Zachowano poprzednią kopię, jeśli była dostępna.") }
            finally { _state.update { it.copy(refreshing = false) } }
        }
    }
    fun refreshIfNeeded() {
        if (_state.value.snapshot?.isFresh() != true && System.currentTimeMillis() - lastRefreshAttempt >= CERT_MAX_AGE_MS)
            refreshCert()
    }
    fun analyze(text: String) {
        if (text.isBlank()) { notify("Wklej treść wiadomości lub adres strony."); return }
        analyzedText = text.take(20_000)
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            val snapshot = _state.value.snapshot
            val result = withContext(Dispatchers.Default) { DomainChecker.analyze(text.take(20_000), snapshot) }
            _state.update { it.copy(analysis = result) }
        }
    }
    fun clearAnalysis() { analysisJob?.cancel(); analyzedText = ""; _state.update { it.copy(analysis = null) } }
    fun saveLocal(note: PhoneNote) = localAction {
        store.save(note)
        notify("Zapisano na tym urządzeniu.")
    }
    fun removeLocal(phone: String) = localAction { store.delete(phone) }
    fun blockLocal(phone: String) = localAction {
        val existing = store.notes().firstOrNull { it.phone == phone } ?: PhoneNote(phone)
        store.save(existing.copy(blocked = true))
        notify("Dodano do Twojej listy blokad.")
    }
    fun setProtection(enabled: Boolean) = localAction { store.protectionEnabled = enabled }
    fun clearEvents() = localAction { store.clearEvents() }
    private fun localAction(action: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        try {
            action()
            _state.update { it.copy(notes = store.notes(), events = store.events(), protectionEnabled = store.protectionEnabled) }
        } catch (_: Exception) { notify("Nie udało się zapisać zmiany na urządzeniu.") }
    }
    fun checkCloud() = cloudAction {
        try {
            val sources = cloud.checkConnection()
            check(sources.any { it.id == "cert_pl" && it.url == CERT_SOURCE })
            _state.update { it.copy(cloudStatus = "Połączono z bazą czytoty-app") }
        } catch (e: Exception) {
            _state.update { it.copy(cloudStatus = "Połączenie z bazą nie powiodło się") }
            throw e
        }
    }
    fun authenticate(email: String, password: String, register: Boolean) = cloudAction {
        require(email.contains('@') && password.length >= 8) { "Wpisz e-mail i hasło (minimum 8 znaków)." }
        if (register) {
            val signedIn = cloud.signUp(email.trim(), password)
            notify(if (signedIn) "Utworzono konto." else "Sprawdź pocztę i potwierdź adres e-mail przed logowaniem.")
        } else cloud.signIn(email.trim(), password)
        val signedInEmail = cloud.email()
        _state.update { it.copy(email = signedInEmail, cloudNotes = emptyList()) }
        if (signedInEmail != null) notify("Zalogowano. Możesz teraz pobrać lub zapisać swoje notatki.")
    }
    fun signOut() = cloudAction {
        cloud.signOut()
        _state.update { it.copy(email = null, cloudNotes = emptyList()) }
    }
    fun saveCloud(note: PhoneNote) = cloudAction {
        val saved = cloud.save(note)
        _state.update { it.copy(cloudNotes = (it.cloudNotes.filterNot { old -> old.phone == saved.phone } + saved).take(100)) }
        notify("Notatka została zapisana na Twoim koncie Supabase. Blokada pozostaje lokalna.")
    }
    fun loadCloud() = cloudAction {
        val notes = cloud.notes()
        _state.update { it.copy(cloudNotes = notes) }
    }
    fun removeCloud(phone: String) = cloudAction {
        cloud.delete(phone)
        _state.update { it.copy(cloudNotes = it.cloudNotes.filterNot { note -> note.phone == phone }) }
    }
    fun restoreLocal(note: CloudNote) = localAction {
        val blocked = store.isBlocked(note.phone)
        store.save(PhoneNote(note.phone, note.label, note.note, blocked))
        notify("Skopiowano notatkę na urządzenie.")
    }
    private fun cloudAction(action: suspend () -> Unit) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try { withTimeout(30_000) { action() } }
            catch (e: kotlinx.coroutines.TimeoutCancellationException) { notify("Serwer nie odpowiedział na czas. Sprawdź połączenie i spróbuj ponownie.") }
            catch (e: CancellationException) { throw e }
            catch (e: IllegalArgumentException) { notify(e.message ?: "Sprawdź wpisane dane.") }
            catch (_: IOException) { notify("Brak połączenia z Supabase. Dane lokalne są dostępne.") }
            catch (_: Exception) { notify("Operacja Supabase nie powiodła się. Sprawdź połączenie, dane logowania i potwierdzenie adresu e-mail. Zmiana nie została potwierdzona.") }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    private fun notify(text: String) { _state.update { it.copy(notice = text) } }
}
