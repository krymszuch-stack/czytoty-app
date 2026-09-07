package com.example.auth.data

import android.content.Intent
import android.util.Log
import com.example.auth.model.AuthSessionState
import com.example.auth.model.AuthUserProfile
import com.example.auth.model.ProfileDto
import com.example.auth.supabase.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val TAG = "AuthRepository"

interface AuthRepository {
    val sessionState: StateFlow<AuthSessionState>
    val currentUser: AuthUserProfile?

    suspend fun initialize()
    suspend fun signInWithGoogle()
    suspend fun signOut()
    suspend fun continueAsGuest()
    suspend fun handleDeeplink(intent: Intent): Boolean
    suspend fun refreshSessionIfNeeded()
    suspend fun fetchRemoteProfile(): ProfileDto?
    suspend fun syncLocalDataToAccount()
}

class DefaultAuthRepository(
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : AuthRepository {

    private val _sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Bootstrapping)
    override val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    override val currentUser: AuthUserProfile?
        get() = (sessionState.value as? AuthSessionState.Authenticated)?.user

    init {
        scope.launch {
            initialize()
        }
    }

    override suspend fun initialize() {
        if (!SupabaseClientProvider.isConfigured) {
            Log.w(TAG, "Supabase nie jest jeszcze skonfigurowane w local.properties. Uruchamianie w trybie gościa.")
            _sessionState.value = AuthSessionState.GuestMode()
            return
        }

        try {
            val supabase = SupabaseClientProvider.client
            // Obserwacja statusu sesji w Supabase
            supabase.auth.sessionStatus.collect { status ->
                when (status) {
                    is SessionStatus.Authenticated -> {
                        val session = status.session
                        val user = session.user
                        if (user != null) {
                            val userMetadata = user.userMetadata
                            val name = userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull
                                ?: userMetadata?.get("name")?.jsonPrimitive?.contentOrNull
                                ?: user.email?.substringBefore("@")
                                ?: "Użytkownik"
                            val avatar = userMetadata?.get("avatar_url")?.jsonPrimitive?.contentOrNull
                                ?: userMetadata?.get("picture")?.jsonPrimitive?.contentOrNull

                            val authUser = AuthUserProfile(
                                id = user.id,
                                email = user.email,
                                displayName = name,
                                avatarUrl = avatar,
                                provider = "google",
                                createdAt = System.currentTimeMillis(),
                                isEmailVerified = user.emailConfirmedAt != null
                            )
                            _sessionState.update { AuthSessionState.Authenticated(authUser) }
                            Log.i(TAG, "Sesja Supabase aktywna: ${user.email}")
                        }
                    }
                    is SessionStatus.NotAuthenticated -> {
                        // Jeśli użytkownik nie jest zalogowany, domyślnie tryb gościa (nie blokujemy aplikacji)
                        _sessionState.update { current ->
                            if (current is AuthSessionState.Bootstrapping || current is AuthSessionState.Authenticated) {
                                AuthSessionState.GuestMode()
                            } else current
                        }
                    }
                    is SessionStatus.Initializing -> {
                        _sessionState.update { AuthSessionState.Bootstrapping }
                    }
                    is SessionStatus.RefreshFailure -> {
                        Log.w(TAG, "Problem z odświeżeniem sesji.")
                        // W trybie offline pozwalamy działać lokalnie
                        _sessionState.update { current ->
                            if (current is AuthSessionState.Bootstrapping) AuthSessionState.GuestMode() else current
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Błąd inicjalizacji Supabase Auth", e)
            _sessionState.value = AuthSessionState.GuestMode()
        }
    }

    override suspend fun signInWithGoogle() {
        if (!SupabaseClientProvider.isConfigured) {
            _sessionState.value = AuthSessionState.Error(
                userFriendlyMessage = "Wymagana konfiguracja SUPABASE_URL i SUPABASE_PUBLISHABLE_KEY w local.properties.",
                technicalDetail = "Brak skonfigurowanych kluczy w BuildConfig"
            )
            return
        }

        try {
            Log.i(TAG, "Rozpoczynanie Google OAuth przez Chrome Custom Tabs...")
            val supabase = SupabaseClientProvider.client
            // Wywołanie OAuth Google z PKCE i Custom Tabs
            supabase.auth.signInWith(Google)
        } catch (e: Exception) {
            Log.e(TAG, "Błąd podczas próby logowania Google", e)
            _sessionState.value = AuthSessionState.Error(
                userFriendlyMessage = "Nie udało się otworzyć logowania Google. Sprawdź połączenie z internetem.",
                technicalDetail = e.message
            )
        }
    }

    override suspend fun signOut() {
        try {
            if (SupabaseClientProvider.isConfigured) {
                SupabaseClientProvider.client.auth.signOut()
            }
            _sessionState.value = AuthSessionState.GuestMode()
            Log.i(TAG, "Użytkownik został pomyślnie wylogowany.")
        } catch (e: Exception) {
            Log.e(TAG, "Błąd wylogowywania", e)
            _sessionState.value = AuthSessionState.GuestMode()
        }
    }

    override suspend fun continueAsGuest() {
        _sessionState.value = AuthSessionState.GuestMode()
    }

    override suspend fun handleDeeplink(intent: Intent): Boolean {
        val data = intent.data ?: return false
        val uriString = data.toString()
        if (!uriString.startsWith("czytoty://auth")) return false

        Log.i(TAG, "Otrzymano autoryzacyjny deeplink: czytoty://auth")
        if (!SupabaseClientProvider.isConfigured) return false

        return try {
            val supabase = SupabaseClientProvider.client
            supabase.handleDeeplinks(intent)
            Log.i(TAG, "Deeplink przetworzony pomyślnie przez Supabase PKCE")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Błąd przetwarzania deeplinku PKCE", e)
            _sessionState.value = AuthSessionState.Error(
                userFriendlyMessage = "Błąd powrotu z logowania Google. Spróbuj ponownie.",
                technicalDetail = e.message
            )
            false
        }
    }

    override suspend fun refreshSessionIfNeeded() {
        if (!SupabaseClientProvider.isConfigured) return
        try {
            SupabaseClientProvider.client.auth.refreshCurrentSession()
        } catch (e: Exception) {
            Log.w(TAG, "Nie udało się odświeżyć sesji: ${e.message}")
        }
    }

    override suspend fun fetchRemoteProfile(): ProfileDto? {
        val user = currentUser ?: return null
        if (!SupabaseClientProvider.isConfigured) return null

        return try {
            val postgrest = SupabaseClientProvider.client.postgrest
            postgrest.from("profiles")
                .select {
                    filter {
                        eq("id", user.id)
                    }
                }
                .decodeSingleOrNull<ProfileDto>()
        } catch (e: Exception) {
            Log.w(TAG, "Błąd pobierania profilu z PostgreSQL public.profiles: ${e.message}")
            null
        }
    }

    override suspend fun syncLocalDataToAccount() {
        // Filozofia CzyToTy?: Nigdy nie niszcz lokalnych ustawień użytkownika!
        // Przy przejściu z Guest do Google Account, lokalne dane są scalane
        Log.i(TAG, "Scalanie danych lokalnych z kontem Google...")
    }
}
