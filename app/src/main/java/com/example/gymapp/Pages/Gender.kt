package com.example.gymapp.Pages

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymapp.AppPreferences

@Composable
fun Gender(modifier: Modifier = Modifier, navController: NavController) {
    val context = LocalContext.current
    var selectedGender by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SELECT YOUR GENDER",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB0C929),
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            RadioButton(
                selected = selectedGender == "Male",
                onClick = { selectedGender = "Male" },
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFB0C929))
            )
            Text("Male", fontSize = 18.sp, modifier = Modifier.padding(end = 24.dp))
            RadioButton(
                selected = selectedGender == "Female",
                onClick = { selectedGender = "Female" },
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFB0C929))
            )
            Text("Female", fontSize = 18.sp)
        }
        // Optionally add "Other"
        /*
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            RadioButton(
                selected = selectedGender == "Other",
                onClick = { selectedGender = "Other" },
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFB0C929))
            )
            Text("Other", fontSize = 18.sp)
        }
        */
        if (showError) {
            Text("Please select a gender.", color = Color.Red, fontSize = 14.sp, modifier = Modifier.padding(bottom = 16.dp))
        }
        Button(
            onClick = {
                if (selectedGender.isEmpty()) {
                    showError = true
                } else {
                    // Load current profile, update gender, and save per-user
                    val profile = AppPreferences.getUserProfile(context)
                    val updatedProfile = profile.copy(gender = selectedGender)
                    AppPreferences.saveUserProfile(context, updatedProfile)
                    navController.navigate("Height")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0C929))
        ) {
            Text("NEXT", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
