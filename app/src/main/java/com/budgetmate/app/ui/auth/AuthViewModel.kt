package com.budgetmate.app.ui.auth

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.budgetmate.app.BudgetMateApp
import com.budgetmate.app.data.entity.UserEntity
import com.budgetmate.app.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * ViewModel for login and registration.
 * Exposes a sealed AuthState so the UI never contains business logic.
 * All database operations are performed via AuthRepository on a coroutine.
 */
class AuthViewModel(application: Application) : AndroidViewModel(application) {

    companion object { private const val TAG = "AuthViewModel" }

    private val app = application as BudgetMateApp
    private val repo = AuthRepository(app.database.userDao(), app.database.categoryDao())

    /**
     * Represents all possible states during authentication.
     * The UI observes this and updates accordingly.
     */
    sealed class AuthState {
        object Idle : AuthState()                          // Initial/reset state
        object Loading : AuthState()                       // Network/DB operation in progress
        data class Success(val user: UserEntity) : AuthState() // Login or register succeeded
        object EmptyFields : AuthState()                   // Username or password was blank
        object PasswordTooShort : AuthState()              // Password does not meet minimum length
        object UsernameTaken : AuthState()                 // Username already exists in the database
        object InvalidCredentials : AuthState()            // Username/password combination not found
        data class Error(val message: String) : AuthState() // Unexpected error
    }

    private val _state = MutableLiveData<AuthState>(AuthState.Idle)
    val state: LiveData<AuthState> = _state

    /**
     * Attempts to log in with the given credentials.
     * Sets state to Loading immediately, then updates based on the repository result.
     */
    fun login(username: String, password: String) {
        Log.d(TAG, "Login attempt for username: $username")
        _state.value = AuthState.Loading
        viewModelScope.launch {
            _state.value = when (val r = repo.login(username, password)) {
                is AuthRepository.AuthResult.Success -> {
                    // Persist the logged-in user ID so other screens can access it
                    app.sessionManager.setLoggedInUser(r.user.userId)
                    Log.i(TAG, "Login successful for userId=${r.user.userId}")
                    AuthState.Success(r.user)
                }
                AuthRepository.AuthResult.EmptyFields -> {
                    Log.w(TAG, "Login failed: empty fields")
                    AuthState.EmptyFields
                }
                AuthRepository.AuthResult.InvalidCredentials -> {
                    Log.w(TAG, "Login failed: invalid credentials for $username")
                    AuthState.InvalidCredentials
                }
                else -> {
                    Log.e(TAG, "Login failed: unexpected result $r")
                    AuthState.Error("Unexpected error")
                }
            }
        }
    }

    /**
     * Registers a new user account.
     * On success, automatically logs in and saves the session.
     */
    fun register(username: String, password: String, displayName: String) {
        Log.d(TAG, "Register attempt for username: $username")
        _state.value = AuthState.Loading
        viewModelScope.launch {
            _state.value = when (val r = repo.register(username, password, displayName)) {
                is AuthRepository.AuthResult.Success -> {
                    app.sessionManager.setLoggedInUser(r.user.userId)
                    Log.i(TAG, "Registration successful for userId=${r.user.userId}")
                    AuthState.Success(r.user)
                }
                AuthRepository.AuthResult.EmptyFields -> {
                    Log.w(TAG, "Registration failed: empty fields")
                    AuthState.EmptyFields
                }
                AuthRepository.AuthResult.PasswordTooShort -> {
                    Log.w(TAG, "Registration failed: password too short")
                    AuthState.PasswordTooShort
                }
                AuthRepository.AuthResult.UsernameTaken -> {
                    Log.w(TAG, "Registration failed: username '$username' already taken")
                    AuthState.UsernameTaken
                }
                else -> {
                    Log.e(TAG, "Registration failed: unexpected result $r")
                    AuthState.Error("Unexpected error")
                }
            }
        }
    }

    /** Resets the state back to Idle, e.g. when the user switches between Login and Register tabs. */
    fun reset() {
        Log.d(TAG, "AuthState reset to Idle")
        _state.value = AuthState.Idle
    }
}
