package com.reflekt.journal.ui.screens.auth

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

sealed interface AuthState {
    object Idle : AuthState
    object Success : AuthState
    data class Error(val message: String) : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor() : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState = _authState.asStateFlow()

    fun onAuthSuccess() {
        _authState.value = AuthState.Success
    }

    fun onAuthError(message: String) {
        _authState.value = AuthState.Error(message)
    }

    fun resetError() {
        _authState.value = AuthState.Idle
    }
}
