package com.talkie.app.presentation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.talkie.app.data.LocalSessionPreferences
import com.talkie.app.data.local.SecurityUtils
import com.talkie.app.data.local.TalkieDatabase
import com.talkie.app.data.local.UserEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val role: String, val fullName: String, val designation: String) : AuthState()
    object Registered : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TalkieDatabase.getDatabase(application)
    private val talkieDao = db.talkieDao()
    private val prefs = LocalSessionPreferences(application)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() {
        if (prefs.isLoggedIn) {
            val username = prefs.loggedInUsername.orEmpty()
            val roleStr = prefs.loggedInRole.orEmpty()
            if (username.isNotEmpty()) {
                viewModelScope.launch {
                    val user = talkieDao.getUserByUsername(username)
                    if (user != null) {
                        _authState.value = AuthState.Success(user.role, user.fullName, user.designation)
                    } else {
                        _authState.value = AuthState.Success(roleStr, username, "Field Operator")
                    }
                }
            } else {
                _authState.value = AuthState.Success(roleStr, "User", "Field Operator")
            }
        }
    }

    fun login(username: String, passwordPlain: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val user = talkieDao.getUserByUsername(username)
            if (user == null) {
                _authState.value = AuthState.Error("User not found")
                return@launch
            }
            
            val hashedInput = SecurityUtils.hashPassword(passwordPlain)
            if (user.passwordHash == hashedInput) {
                prefs.isLoggedIn = true
                prefs.loggedInUsername = user.username
                prefs.loggedInRole = user.role
                _authState.value = AuthState.Success(user.role, user.fullName, user.designation)
            } else {
                _authState.value = AuthState.Error("Invalid credentials")
            }
        }
    }

    fun register(username: String, passwordPlain: String, fullName: String, designation: String, role: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            val existing = talkieDao.getUserByUsername(username)
            if (existing != null) {
                _authState.value = AuthState.Error("Username already exists")
                return@launch
            }
            
            val hashedPassword = SecurityUtils.hashPassword(passwordPlain)
            val newUser = UserEntity(
                username = username,
                passwordHash = hashedPassword,
                fullName = fullName,
                designation = designation,
                role = role
            )
            
            try {
                talkieDao.registerUser(newUser)
                _authState.value = AuthState.Registered
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Registration failed: ${e.message}")
            }
        }
    }
    
    fun logout() {
        prefs.clearSession()
        _authState.value = AuthState.Idle
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
