package com.example.gymapp

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser

// Define AuthState outside the ViewModel
sealed class AuthState {
    object Authenticated : AuthState()
    object Unauthenticated : AuthState()
    object Loading : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel: ViewModel() {

    private val auth : FirebaseAuth = FirebaseAuth.getInstance()
    private val _authState = MutableLiveData<AuthState>() // Now it can find AuthState
    val authState: LiveData<AuthState> = _authState

    // You have a typo in your original code: _authStatus should be _authState
    private val _authStatus = _authState // Assuming this was a typo and you meant to use _authState

    init {
        checkAuthStatus()
    }

    fun checkAuthStatus() {
        if (auth.currentUser == null) {
            _authState.value = AuthState.Unauthenticated
        } else {
            _authState.value = AuthState.Authenticated
        }
    }

    fun login(email: String, password: String) {

        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }

        _authStatus.value = AuthState.Loading // Corrected to _authState if that was the intent
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authStatus.value = AuthState.Authenticated // Corrected
                } else {
                    _authStatus.value = AuthState.Error( // Corrected
                        task.exception?.message ?: "Something went wrong"
                    )
                }
            }
    }

    fun SignUp(email: String, password: String, username: String, onSuccess: (() -> Unit)? = null) {
        if (email.isEmpty() || password.isEmpty()) {
            _authState.value = AuthState.Error("Email or password can't be empty")
            return
        }

        _authState.value = AuthState.Loading
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _authState.value = AuthState.Unauthenticated // Do not authenticate after signup
                    onSuccess?.invoke() // Call the callback after successful signup
                } else {
                    _authState.value = AuthState.Error(
                        task.exception?.message ?: "Something went wrong"
                    )
                }
            }
    } // This curly brace was misplaced in your original code, it should be after signOut

    fun SignOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
    fun signOut() {
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
    private fun AuthViewModel.saveUserAge(
        age: Int,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit // Change this line
    ) {
        // TODO("Not yet implemented - implement your saving logic here")
        // For example, if you have a Firebase call that can fail:
        // viewModelScope.launch {
        //     try {
        //         // Your asynchronous operation to save age
        //         // ...
        //         onSuccess()
        //     } catch (e: Exception) {
        //         onFailure(e.message ?: "An unknown error occurred")
        //     }
        // }
    }

}
