package com.example.auth.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.auth.data.AuthRepository
import com.example.auth.data.DefaultAuthRepository
import com.example.auth.model.AuthSessionState
import com.example.auth.model.AuthUserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val sessionState: AuthSessionState = AuthSessionState.Bootstrapping,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isAuthScreenVisible: Boolean = false
) {
    val isAuthenticated: Boolean
        get() = sessionState is AuthSessionState.Authenticated

    val currentUser: AuthUserProfile?
        get() = (sessionState as? AuthSessionState.Authenticated)?.user

    val isGuestMode: Boolean
        get() = sessionState is AuthSessionState.GuestMode
}

class AuthViewModel(
    private val authRepository: AuthRepository = DefaultAuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.sessionState.collect { session ->
                _uiState.update { current ->
                    current.copy(
                        sessionState = session,
                        isLoading = false,
                        isAuthScreenVisible = if (session is AuthSessionState.Authenticated) false else current.isAuthScreenVisible,
                        errorMessage = (session as? AuthSessionState.Error)?.userFriendlyMessage
                    )
                }
            }
        }
    }

    fun signInWithGoogle() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            authRepository.signInWithGoogle()
        }
    }

    fun signOut() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            authRepository.signOut()
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun continueAsGuest() {
        viewModelScope.launch {
            authRepository.continueAsGuest()
            _uiState.update { it.copy(isAuthScreenVisible = false, errorMessage = null) }
        }
    }

    fun openAuthScreen() {
        _uiState.update { it.copy(isAuthScreenVisible = true) }
    }

    fun closeAuthScreen() {
        _uiState.update { it.copy(isAuthScreenVisible = false) }
    }

    fun handleDeeplink(intent: Intent) {
        viewModelScope.launch {
            val handled = authRepository.handleDeeplink(intent)
            if (handled) {
                _uiState.update { it.copy(isAuthScreenVisible = false, isLoading = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
