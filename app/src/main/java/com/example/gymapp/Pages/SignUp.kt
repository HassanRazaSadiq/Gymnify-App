package com.example.gymapp.Pages

import android.util.Log // For debugging
import android.util.Patterns // For email validation
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import com.example.gymapp.AuthViewModel
import com.example.gymapp.AppPreferences

@Composable
fun Signup(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }

    var isUsernameValid by remember { mutableStateOf(false) }
    var isEmailValid by remember { mutableStateOf(false) }
    var isPasswordValid by remember { mutableStateOf(false) }

    val isButtonEnabled = isUsernameValid && isEmailValid && isPasswordValid

    val context = LocalContext.current
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFFE7FAFE), Color(0xFFB0C929))
    )

    val primaryColor = Color(0xFFF5F5F5)
    val buttonColor = Color(0xFF000000)
    val inputTextColor = Color.Black
    val headerTextColor = Color(0xFF000000)
    val accentColor = Color(0xFFFF6079)
    val unfocusedBorderColor = Color.Gray
    val cursorColor = Color.Black

    fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun handleSignup() {
        if (isButtonEnabled) {
            authViewModel.SignUp(email, password, username) {
                // Save initial profile per user only after successful signup
                val userProfile = AppPreferences.UserProfile(
                    username = username,
                    fullName = "",
                    email = email,
                    gender = "",
                    age = 0,
                    heightValue = 0f,
                    heightUnit = "cm",
                    weightValue = 0f,
                    weightUnit = "kg",
                    profileImageUri = null,
                    exerciseRecords = emptyList()
                )
                AppPreferences.saveUserProfile(context, userProfile)
                AppPreferences.setNeedsOnboarding(context, true)
                Log.d("SignupPage", "Saved Username: $username, Email: $email")
                // Keep user signed in through onboarding; do NOT sign out here
                navController.navigate("Age")
                Toast.makeText(context, "Sign Up Processing...", Toast.LENGTH_SHORT).show()
            }
        } else {
            var errorMessage = "Please ensure all fields are filled correctly:\n"
            if (!isUsernameValid) errorMessage += "- Username is required.\n"
            if (!isEmailValid) errorMessage += "- A valid email is required.\n"
            if (!isPasswordValid) errorMessage += "- Password must be at least 6 characters.\n"
            Toast.makeText(context, errorMessage.trim(), Toast.LENGTH_LONG).show()
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
                .align(Alignment.CenterHorizontally)
                .padding(start = 32.dp, end = 32.dp)
        )

        Text(
            text = "Hello there, sign up to continue!",
            fontSize = 16.sp,
            color = headerTextColor,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        OutlinedTextField(
            value = username,
            onValueChange = {
                username = it
                isUsernameValid = it.isNotBlank()
            },
            label = { Text(text = "Username") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            textStyle = TextStyle(color = inputTextColor),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = unfocusedBorderColor,
                focusedLabelColor = primaryColor,
                cursorColor = cursorColor
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                isEmailValid = isValidEmail(it)
            },
            label = { Text(text = "Email address") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            textStyle = TextStyle(color = inputTextColor),
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = unfocusedBorderColor,
                focusedLabelColor = primaryColor,
                cursorColor = cursorColor
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                isPasswordValid = it.isNotBlank() && it.length >= 6
            },
            label = { Text(text = "Password") },
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
                handleSignup() // Call the extracted signup handler
            }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = primaryColor,
                unfocusedBorderColor = unfocusedBorderColor,
                focusedLabelColor = primaryColor,
                cursorColor = cursorColor
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                handleSignup() // Call the extracted signup handler
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
            Text(
                text = "Create an Account",
                color = Color.White,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = {
            navController.navigate("LoginPage")
        }) {
            Text(text = "Already have an account? Login", color = accentColor, fontSize = 14.sp)
        }
    }
}
