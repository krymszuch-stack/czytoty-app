package com.example.sms

import android.app.Application
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SmsState(
    val messages: List<DeviceSms> = emptyList(),
    val conversation: List<DeviceSms> = emptyList(),
    val sends: List<SmsSend> = emptyList(),
    val selectedThread: Long? = null,
    val canRead: Boolean = false,
    val loading: Boolean = false,
    val sending: Boolean = false,
    val error: String? = null,
    val sendAccepted: Int = 0,
    val limit: Int = 500,
    val contacts: Map<String, String> = emptyMap()
)

data class ConversationMetadata(val ready: Boolean = false, val drafts: Map<String, SmsDraft> = emptyMap(),
    val pins: Set<Long> = emptySet(), val error: String? = null)

class SmsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SmsRepository(application)
    private val _state = MutableStateFlow(SmsState())
    val state = _state.asStateFlow()
    private val _metadata = MutableStateFlow(ConversationMetadata())
    val metadata = _metadata.asStateFlow()
    private val writes = kotlinx.coroutines.channels.Channel<(ConversationStore) -> Unit>(kotlinx.coroutines.channels.Channel.UNLIMITED)
    private var readJob: Job? = null
    private var observed = false
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) { reload() }
    }
    init {
        // Closing the screen drains queued local writes instead of cancelling the last keystrokes.
        CoroutineScope(Dispatchers.IO).launch {
            ConversationStore(application).use { store ->
                try { _metadata.value = ConversationMetadata(true, store.drafts(), store.pins()) }
                catch (_: Exception) { _metadata.value = ConversationMetadata(error = "Nie udało się odczytać szkiców. Uruchom aplikację ponownie.") }
                for (write in writes) {
                    try { write(store); _metadata.update { it.copy(error = null) } }
                    catch (_: Exception) { _metadata.update { it.copy(error = "Nie udało się zapisać zmian na telefonie. Zachowaj treść przed zamknięciem aplikacji.") } }
                }
            }
        }
        viewModelScope.launch { SmsChanges.revision.collect { reload() } }
    }
    fun draft(key: String, value: SmsDraft) {
        if (!_metadata.value.ready) return
        _metadata.update { it.copy(drafts = it.drafts + (key to value)) }
        writes.trySend { it.saveDraft(key, value) }
    }
    fun togglePin(thread: Long) {
        if (!_metadata.value.ready) return
        val pinned = thread !in _metadata.value.pins
        _metadata.update { it.copy(pins = if (pinned) it.pins + thread else it.pins - thread) }
        writes.trySend { it.pin(thread, pinned) }
    }
    fun reload() {
        readJob?.cancel()
        val permitted = repository.canRead()
        _state.update { it.copy(canRead = permitted) }
        if (!permitted) {
            if (observed) { getApplication<Application>().contentResolver.unregisterContentObserver(observer); observed = false }
            _state.update { it.copy(messages = emptyList(), conversation = emptyList(), loading = false) }
        }
        if (permitted && !observed) {
            try { getApplication<Application>().contentResolver.registerContentObserver(Telephony.Sms.CONTENT_URI, true, observer); observed = true }
            catch (_: SecurityException) { _state.update { it.copy(error = "Android odmówił obserwowania zmian SMS.") } }
        }
        readJob?.cancel()
        readJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            try {
                val thread = _state.value.selectedThread
                val limit = _state.value.limit
                val result = if (permitted) withContext(Dispatchers.IO) { repository.read(limit) } else emptyList()
                val conversation = if (permitted && thread != null) withContext(Dispatchers.IO) { repository.read(500, thread).reversed() } else emptyList()
                val sends = withContext(Dispatchers.IO) { SmsSendStore(getApplication()).use { it.recent() } }
                val contacts = withContext(Dispatchers.IO) {
                    runCatching { com.example.data.DialerData.contacts(getApplication(), limit = 500).associate {
                        (com.example.data.PhoneNumbers.parse(it.phone)?.e164 ?: it.phone) to it.name
                    } }.getOrDefault(emptyMap())
                }
                _state.update { it.copy(messages = result, conversation = conversation, sends = sends, contacts = contacts, loading = false, error = null) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.update { it.copy(loading = false, error = "Nie udało się odczytać SMS-ów. Sprawdź uprawnienia w ustawieniach Androida.") } }
        }
    }
    fun select(thread: Long?) { _state.update { it.copy(selectedThread = thread, conversation = emptyList()) }; reload() }
    fun loadMore() { _state.update { it.copy(limit = (it.limit + 500).coerceAtMost(5000)) }; reload() }
    fun send(address: String, body: String, draftKey: String = "new") {
        if (_state.value.sending) return
        _state.update { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.send(address, body) }
                if (_metadata.value.drafts[draftKey] == SmsDraft(address, body)) draft(draftKey, SmsDraft(address))
                _state.update { it.copy(sending = false, sendAccepted = it.sendAccepted + 1) }
                reload()
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                _state.update { it.copy(sending = false, error = when(e) {
                    is IllegalArgumentException, is IllegalStateException -> e.message
                    else -> "Nie udało się zlecić wysyłki SMS. Sprawdź uprawnienia i kartę SIM."
                }) }
            }
        }
    }
    override fun onCleared() {
        writes.close()
        if (observed) getApplication<Application>().contentResolver.unregisterContentObserver(observer)
        super.onCleared()
    }
}
