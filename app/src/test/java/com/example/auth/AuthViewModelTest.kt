package com.example.auth

import android.content.Intent
import com.example.auth.data.AuthRepository
import com.example.auth.model.AuthSessionState
import com.example.auth.model.AuthUserProfile
import com.example.auth.viewmodel.AuthViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Testy jednostkowe dla AuthViewModel i AuthUserProfile (Supabase PKCE Google Auth).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AuthViewModelTest {

    private val testDispatcher: TestDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository(initialState = AuthSessionState.GuestMode())
        viewModel = AuthViewModel(authRepository = fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state defaults to GuestMode and app is unblocked`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Domyślnie aplikacja powinna być w trybie gościa", state.isGuestMode)
        assertFalse("Nie powinna wymagać logowania na start", state.isAuthenticated)
        assertNull("Brak profilu zalogowanego użytkownika", state.currentUser)
        assertFalse("Ekran logowania domyślnie ukryty", state.isAuthScreenVisible)
    }

    @Test
    fun `openAuthScreen and closeAuthScreen toggle visibility`() = runTest(testDispatcher) {
        viewModel.openAuthScreen()
        assertTrue(viewModel.uiState.value.isAuthScreenVisible)

        viewModel.closeAuthScreen()
        assertFalse(viewModel.uiState.value.isAuthScreenVisible)
    }

    @Test
    fun `continueAsGuest closes auth screen and leaves user in GuestMode`() = runTest(testDispatcher) {
        viewModel.openAuthScreen()
        assertTrue(viewModel.uiState.value.isAuthScreenVisible)

        viewModel.continueAsGuest()
        advanceUntilIdle()

        assertTrue(fakeRepository.continueAsGuestCalled)
        assertFalse(viewModel.uiState.value.isAuthScreenVisible)
        assertTrue(viewModel.uiState.value.isGuestMode)
    }

    @Test
    fun `when repository emits Authenticated, viewModel reflects user profile and closes auth screen`() = runTest(testDispatcher) {
        viewModel.openAuthScreen()
        assertTrue(viewModel.uiState.value.isAuthScreenVisible)

        val testUser = AuthUserProfile(
            id = "google-uid-12345",
            email = "tester@gmail.com",
            displayName = "Jan Kowalski",
            avatarUrl = "https://lh3.googleusercontent.com/a/photo.jpg",
            provider = "google"
        )

        fakeRepository.emitSession(AuthSessionState.Authenticated(testUser))
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Użytkownik powinien być oznaczony jako zalogowany", state.isAuthenticated)
        assertFalse("Użytkownik nie jest już w trybie gościa", state.isGuestMode)
        assertNotNull(state.currentUser)
        assertEquals("Jan Kowalski", state.currentUser?.displayName)
        assertEquals("tester@gmail.com", state.currentUser?.email)
        assertEquals("https://lh3.googleusercontent.com/a/photo.jpg", state.currentUser?.avatarUrl)
        assertEquals("JK", state.currentUser?.initials)
        assertFalse("Ekran logowania powinien zamknąć się po udanym zalogowaniu", state.isAuthScreenVisible)
    }

    @Test
    fun `when repository emits Error, viewModel shows error message and can clear it`() = runTest(testDispatcher) {
        fakeRepository.emitSession(
            AuthSessionState.Error(
                userFriendlyMessage = "Brak połączenia z siecią",
                technicalDetail = "UnknownHostException"
            )
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Brak połączenia z siecią", state.errorMessage)

        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }

    @Test
    fun `signInWithGoogle sets loading state and delegates to repository`() = runTest(testDispatcher) {
        viewModel.signInWithGoogle()
        assertTrue("Próba logowania powinna aktywować wskaźnik ładowania", viewModel.uiState.value.isLoading)

        advanceUntilIdle()
        assertTrue("Powinno wywołać signInWithGoogle w repozytorium", fakeRepository.signInWithGoogleCalled)
    }

    @Test
    fun `signOut delegates to repository and resets to guest state`() = runTest(testDispatcher) {
        val testUser = AuthUserProfile(
            id = "user-1",
            email = "user@gmail.com",
            displayName = "Użytkownik Test",
            avatarUrl = null
        )
        fakeRepository.emitSession(AuthSessionState.Authenticated(testUser))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isAuthenticated)

        viewModel.signOut()
        advanceUntilIdle()

        assertTrue(fakeRepository.signOutCalled)
        assertTrue(viewModel.uiState.value.isGuestMode)
        assertFalse(viewModel.uiState.value.isAuthenticated)
    }

    @Test
    fun `handleDeeplink delegates to repository and hides auth screen on success`() = runTest(testDispatcher) {
        viewModel.openAuthScreen()
        val intent = Intent()

        viewModel.handleDeeplink(intent)
        advanceUntilIdle()

        assertEquals(intent, fakeRepository.handleDeeplinkCalledWith)
        assertFalse(viewModel.uiState.value.isAuthScreenVisible)
    }

    @Test
    fun `AuthUserProfile initials generation works correctly across various names`() {
        val user1 = AuthUserProfile("1", "a@b.com", "Jan Kowalski", null)
        assertEquals("JK", user1.initials)

        val user2 = AuthUserProfile("2", "a@b.com", "Adam", null)
        assertEquals("A", user2.initials)

        val user3 = AuthUserProfile("3", "andrzej.nowak@example.com", "Andrzej Michał Nowak", null)
        assertEquals("AM", user3.initials)

        val user4 = AuthUserProfile("4", "test@domain.com", "", null)
        assertEquals("T", user4.initials)
    }
}

/**
 * Fake implementation of AuthRepository for unit testing.
 */
class FakeAuthRepository(
    initialState: AuthSessionState = AuthSessionState.GuestMode()
) : AuthRepository {

    private val _sessionState = MutableStateFlow<AuthSessionState>(initialState)
    override val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    override val currentUser: AuthUserProfile?
        get() = (_sessionState.value as? AuthSessionState.Authenticated)?.user

    var signInWithGoogleCalled = false
    var signOutCalled = false
    var continueAsGuestCalled = false
    var handleDeeplinkCalledWith: Intent? = null
    var deeplinkResultToReturn = true

    override suspend fun signInWithGoogle() {
        signInWithGoogleCalled = true
    }

    override suspend fun signOut() {
        signOutCalled = true
        _sessionState.value = AuthSessionState.GuestMode()
    }

    override suspend fun continueAsGuest() {
        continueAsGuestCalled = true
        _sessionState.value = AuthSessionState.GuestMode()
    }

    override suspend fun handleDeeplink(intent: Intent): Boolean {
        handleDeeplinkCalledWith = intent
        return deeplinkResultToReturn
    }

    override suspend fun initialize() {}

    override suspend fun refreshSessionIfNeeded() {}

    override suspend fun fetchRemoteProfile(): com.example.auth.model.ProfileDto? = null

    override suspend fun syncLocalDataToAccount() {}

    fun emitSession(state: AuthSessionState) {
        _sessionState.value = state
    }
}
