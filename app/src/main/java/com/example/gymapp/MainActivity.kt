package com.example.gymapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.gymapp.ui.theme.GYMAppTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val authViewModel: AuthViewModel by viewModels()
        // Read login state from SharedPreferences
        val sharedPref = getSharedPreferences("gymapp_prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        val darkModePref = getSharedPreferences("user_profile_prefs", MODE_PRIVATE).getBoolean("isDarkMode", false)
        setContent {
            var isDarkMode by remember { mutableStateOf(darkModePref) }
            GYMAppTheme(darkTheme = isDarkMode) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Navigation(
                        modifier = Modifier.padding(innerPadding),
                        authViewModel = authViewModel,
                        isDarkMode = isDarkMode,
                        onDarkModeChange = { enabled ->
                            isDarkMode = enabled
                            getSharedPreferences("user_profile_prefs", MODE_PRIVATE)
                                .edit().putBoolean("isDarkMode", enabled).apply()
                        }
                    )

                }
            }
        }
    }
}
