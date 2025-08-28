package com.example.gymapp.Pages

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // For Toast and SharedPreferences context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.gymapp.AppPreferences
import android.util.Log // Import your AppPreferences

@Composable
fun Age(modifier: Modifier = Modifier, navController: NavController) {

    var selectedAgeString by remember { mutableStateOf("27") } // Initial default selection
    val context = LocalContext.current // Get context for SharedPreferences and Toast

    val ageList = (17..60).map { it.toString() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, start = 16.dp, end = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.Black,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = {
                        navController.popBackStack() // Go to previous screen
                    })
            )
            Text(
                text = "Skip",
                fontSize = 18.sp,
                color = Color.Black,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.clickable(onClick = {
                    navController.navigate("Home") // Navigate to Home or next main section
                })
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Step 3 of 5", // This is example text
            fontSize = 18.sp,
            color = Color.Black,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "HOW OLD ARE YOU?",
            fontSize = 28.sp,
            color = Color(0xFFB0C929),
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(vertical = 80.dp), // Adjust for visual centering
        ) {
            items(ageList.size) { index ->
                val ageItem = ageList[index]
                val isSelected = selectedAgeString == ageItem
                Text(
                    text = ageItem,
                    fontSize = if (isSelected) 30.sp else 24.sp,
                    color = if (isSelected) Color.White else Color(0xFF6A6A6A),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .fillMaxWidth(0.25f)
                        .padding(vertical = 8.dp)
                        .clickable(
                            onClick = { selectedAgeString = ageItem },
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        )
                        .background(
                            color = if (isSelected) Color.Black else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(vertical = 12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                val ageToSave = selectedAgeString.toIntOrNull()
                if (ageToSave != null) {
                    // Load current profile, update age, and save per-user
                    val profile = AppPreferences.getUserProfile(context)
                    val updatedProfile = profile.copy(age = ageToSave)
                    AppPreferences.saveUserProfile(context, updatedProfile)
                    Log.d("Age", "Saved Age: $ageToSave")
                    Toast.makeText(context, "Age ($ageToSave) saved!", Toast.LENGTH_SHORT).show()
                    navController.navigate("Gender") // Navigate to Gender selection after age
                } else {
                    Toast.makeText(context, "Invalid age selected.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(60.dp)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0C929)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "NEXT STEPS",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
