package com.example.gymapp.Pages

import android.util.Patterns // For basic email validation
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
// import androidx.compose.runtime.getValue // Not explicitly needed if using 'by'
import androidx.compose.runtime.livedata.observeAsState // To observe LiveData from ViewModel
import androidx.compose.runtime.setValue
// import androidx.compose.runtime.setValue // Not explicitly needed if using 'by'
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymapp.AuthViewModel // Your ViewModel
import com.example.gymapp.AuthState    // Your AuthState sealed class
import androidx.compose.ui.text.font.FontWeight

@Composable
fun LoginPage(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var attemptedLogin by remember { mutableStateOf(false) } // Track if a login attempt was made

    val authStateValue by authViewModel.authState.observeAsState()
    val context = LocalContext.current

    // Frontend validation for enabling the button
    val isEmailFieldValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isPasswordNotEmpty = password.isNotBlank()
    // You can add more specific password rules here if needed (e.g., length)
    // val isPasswordLengthValid = password.length >= 6

    // Button is enabled if fields meet basic criteria AND authState is not Loading
    val isButtonEnabled = isEmailFieldValid && isPasswordNotEmpty && authStateValue !is AuthState.Loading

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFFE7FAFE), Color(0xFFB0C929))
    )

    val primaryColor = Color(0xFFF5F5F5)
    val buttonColor = Color(0xFF000000)
    val headerTextColor = Color(0xFF191919)
    val inputTextColor = Color.Black
    val accentColor = Color(0xFFFF6079)
    val unfocusedBorderColor = Color.Gray
    val cursorColor = Color.Black

    // Handle results from the ViewModel after a login attempt
    LaunchedEffect(authStateValue, attemptedLogin) {
        // Only react if a login attempt was made from this screen
        // and the state is relevant to that attempt.
        if (attemptedLogin) {
            when (val state = authStateValue) {
                is AuthState.Authenticated -> {
                    // Save login state in SharedPreferences
                    val sharedPref = context.getSharedPreferences("gymapp_prefs", android.content.Context.MODE_PRIVATE)
                    sharedPref.edit().putBoolean("isLoggedIn", true).apply()
                    Toast.makeText(context, "Welcome to App!", Toast.LENGTH_LONG).show()
                    navController.navigate("Home") {
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                        launchSingleTop = true
                    }
                    // Reset attemptedLogin after successful navigation or handling
                    attemptedLogin = false
                }
                is AuthState.Error -> {
                    Toast.makeText(context, state.message, Toast.LENGTH_LONG).show()
                    // Reset attemptedLogin so error toast doesn't reappear on unrelated state changes
                    attemptedLogin = false
                    // Optionally, reset the authState in ViewModel to Unauthenticated
                    // to allow for immediate new attempts without state sticking to Error
                    // authViewModel.resetToUnauthenticatedAfterError() // You'd need this in AuthViewModel
                }
                is AuthState.Loading -> {
                    // Button is already showing a loading indicator
                }
                is AuthState.Unauthenticated -> {
                    // If state becomes Unauthenticated AFTER an attempted login,
                    // it might mean the error was handled and reset by VM.
                    attemptedLogin = false
                }
                else -> {
                    // Null or Initial state while AuthViewModel might be initializing
                    // or if not related to a login attempt from this screen.
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBackground),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "WELCOME TO GYMNIFY!",
            fontSize = 28.sp,
            color = headerTextColor,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 32.dp, end = 32.dp, bottom = 16.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = {
                Text(
                    text = "Email",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            textStyle = TextStyle(color = inputTextColor),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            singleLine = true,
            isError = attemptedLogin && !isEmailFieldValid && email.isNotEmpty(), // Show error if attempted and invalid
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = if (attemptedLogin && !isEmailFieldValid && email.isNotEmpty()) MaterialTheme.colorScheme.error else unfocusedBorderColor,
                focusedLabelColor = primaryColor,
                cursorColor = cursorColor,
                focusedTextColor = inputTextColor,
                unfocusedTextColor = inputTextColor
            )
        )
        if (attemptedLogin && !isEmailFieldValid && email.isNotEmpty()) {
            Text(
                text = "Invalid email format",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 34.dp, end = 32.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = {
                Text(
                    text = "Password",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            textStyle = TextStyle(color = inputTextColor),
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                attemptedLogin = true // Mark that a login attempt is being made
                if (isButtonEnabled) {
                    authViewModel.login(email, password)
                } else {
                    // Frontend validation failed, show toast if not already shown by field errors
                    if (!isEmailFieldValid && email.isNotEmpty()) Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
                    else if (!isPasswordNotEmpty) Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                    // else if (!isPasswordLengthValid) Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                }
            }),
            singleLine = true,
            isError = attemptedLogin && !isPasswordNotEmpty, // Show error if attempted and invalid
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = if (attemptedLogin && !isPasswordNotEmpty) MaterialTheme.colorScheme.error else unfocusedBorderColor,
                focusedLabelColor = primaryColor,
                cursorColor = cursorColor,
                focusedTextColor = inputTextColor,
                unfocusedTextColor = inputTextColor
            )
        )
        if (attemptedLogin && !isPasswordNotEmpty) {
            Text(
                text = "Password cannot be empty",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 34.dp, end = 32.dp)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                attemptedLogin = true // Mark that a login attempt is being made
                // Frontend validation should already be handled by isButtonEnabled,
                // but this call to authViewModel.login will now only happen if isButtonEnabled is true.
                // The AuthViewModel will handle the backend validation.
                if(isButtonEnabled) {
                    authViewModel.login(email, password)
                } else {
                    // Show specific frontend validation errors if button was clicked despite being "disabled"
                    // (e.g. if state updated slowly or for clarity)
                    if (!isEmailFieldValid && email.isNotEmpty()) Toast.makeText(context, "Invalid email format", Toast.LENGTH_SHORT).show()
                    else if (!isPasswordNotEmpty) Toast.makeText(context, "Password cannot be empty", Toast.LENGTH_SHORT).show()
                    else Toast.makeText(context, "Please fill all fields correctly.", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = isButtonEnabled,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .padding(horizontal = 32.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = buttonColor,
                disabledContainerColor = buttonColor.copy(alpha = 0.5f)
            )
        ) {
            if (authStateValue is AuthState.Loading && attemptedLogin) { // Show loading only if it's due to current attempt
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    text = "Login",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = {
            if (authStateValue !is AuthState.Loading) {
                navController.navigate("SignUp")
            }
        }) {
            Text(text = "Don't have an account? Sign up", color = accentColor, fontSize = 14.sp)
        }
    }
}
