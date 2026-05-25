package com.example.orthofinixai.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.orthofinixai.data.model.User
import com.example.orthofinixai.data.repository.AuthRepository
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AuthRepository(application.applicationContext)

    private val _uiState = MutableStateFlow<AuthState>(AuthState.Idle)
    val uiState: StateFlow<AuthState> = _uiState.asStateFlow()

    val googleSignInClient get() = repository.getGoogleSignInClient()

    init { checkExistingSession() }

    fun checkExistingSession() {
        val user = repository.restoreSession()
        if (user != null) {
            _uiState.value = AuthState.Authenticated(user)
        } else {
            _uiState.value = AuthState.Idle
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            repository.signInWithEmail(email, password)
                .onSuccess { _uiState.value = AuthState.Authenticated(it) }
                .onFailure { _uiState.value = AuthState.Error(it.message ?: "Login failed") }
        }
    }

    fun signInWithGoogle(account: GoogleSignInAccount) {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            repository.signInWithGoogle(account)
                .onSuccess { _uiState.value = AuthState.Authenticated(it) }
                .onFailure { _uiState.value = AuthState.Error(it.message ?: "Google sign-in failed") }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = AuthState.Loading
            repository.signUpWithEmail(email, password, displayName)
                .onSuccess { _uiState.value = AuthState.Authenticated(it) }
                .onFailure { _uiState.value = AuthState.Error(it.message ?: "Sign up failed") }
        }
    }

    fun logout() {
        repository.logout()
        _uiState.value = AuthState.Idle
    }

    fun resetPassword(email: String, onResult: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            repository.sendPasswordResetEmail(email)
                .onSuccess { onResult(true, null) }
                .onFailure { onResult(false, it.message ?: "Could not send reset email") }
        }
    }
}
