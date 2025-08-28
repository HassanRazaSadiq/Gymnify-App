package com.example.gymapp.Pages

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.text.font.FontWeight
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.tasks.await
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

val buttonColor = Color(0xFF000000)
val gradientBackground = Brush.verticalGradient(
    colors = listOf(Color(0xFFE7FAFE), Color(0xFFB0C929))
)

@Composable
fun ChangePassword(modifier: Modifier = Modifier, navController: NavController) {
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    val firebaseAuth = FirebaseAuth.getInstance()
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Change Password", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedTextField(
            value = currentPassword,
            onValueChange = { currentPassword = it },
            label = { Text("Current Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = newPassword,
            onValueChange = { newPassword = it },
            label = { Text("New Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm New Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = {
                errorMessage = ""
                successMessage = ""
                val user = firebaseAuth.currentUser
                when {
                    currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank() -> {
                        errorMessage = "All fields are required."
                    }
                    newPassword != confirmPassword -> {
                        errorMessage = "New passwords do not match."
                    }
                    newPassword.length < 6 -> {
                        errorMessage = "New password must be at least 6 characters."
                    }
                    user == null || user.email.isNullOrBlank() -> {
                        errorMessage = "No user is currently logged in."
                    }
                    else -> {
                        isLoading = true
                        scope.launch {
                            try {
                                val credential = EmailAuthProvider.getCredential(user!!.email!!, currentPassword)
                                user.reauthenticate(credential).await()
                                user.updatePassword(newPassword).await()
                                successMessage = "Password changed successfully!"
                                currentPassword = ""
                                newPassword = ""
                                confirmPassword = ""
                            } catch (ce: CancellationException) {
                                // User left the screen; ignore
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Failed to change password."
                            } finally {
                                isLoading = false
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor
            ),
            enabled = !isLoading,
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            } else {
                Text(
                    text = "Update Password",
                    color = Color.White,
                    fontSize = 18.sp
                )
            }
        }
        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(errorMessage, color = MaterialTheme.colorScheme.error)
        }
        if (successMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(successMessage, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.height(32.dp))
        TextButton(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}
