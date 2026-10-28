package com.suarezmillan.finanzas.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.suarezmillan.finanzas.data.repo.AuthRepository
import com.suarezmillan.finanzas.data.repo.AuthSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    val session: StateFlow<AuthSession> = auth.session
    val hasAccounts: StateFlow<Boolean> = auth.hasAccounts

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun login(email: String, password: String) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        viewModelScope.launch {
            auth.login(email, password).onFailure { _error.value = it.message }
            _busy.value = false
        }
    }

    fun register(name: String, email: String, password: String) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        viewModelScope.launch {
            auth.register(name, email, password).onFailure { _error.value = it.message }
            _busy.value = false
        }
    }

    fun logout() = auth.logout()

    fun clearError() {
        _error.value = null
    }
}
