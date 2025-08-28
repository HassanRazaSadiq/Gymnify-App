package com.example.gymapp.Pages

import android.util.Log // For debugging
import android.widget.Toast // For user feedback
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // For Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymapp.AppPreferences
@Composable
fun Height(modifier: Modifier = Modifier, navController: NavController) { // Added default modifier
    var heightValueString by remember { mutableStateOf("") } // Keep as String for input
    var unit by remember { mutableStateOf("CM") } // Default unit
    var isError by remember { mutableStateOf(false) }
    val context = LocalContext.current // Get context

    // Load previously saved height and unit when the screen is first composed
    LaunchedEffect(Unit) {
        val profile = AppPreferences.getUserProfile(context)
        if (profile.heightValue > 0f) {
            heightValueString = profile.heightValue.toString().removeSuffix(".0")
        }
        profile.heightUnit?.let { unit = it }
    }

    Column(
        modifier = modifier // Use the parameter
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        HeightTopBar(navController)

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Step 4 of 5",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "HOW MUCH DO YOU HEIGHT?",
                fontSize = 24.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFFB0C929),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(64.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                UnitButton(label = "FEET", isSelected = unit == "FEET") {
                    unit = "FEET"
                    // Optional: You might want to convert heightValueString if unit changes
                    // For simplicity, we'll just save whatever is currently entered.
                }
                UnitButton(label = "CM", isSelected = unit == "CM") {
                    unit = "CM"
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(Color(0xFFF0F0F0), RoundedCornerShape(12.dp))
                    .border(
                        2.dp,
                        if (isError) Color.Red else Color.LightGray, // Highlight border on error
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = heightValueString.ifEmpty { "0" },
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = unit,
                        fontSize = 24.sp,
                        color = Color.Gray
                    )
                }
            }

            if (isError) {
                Text(
                    text = "Please enter a valid height value.",
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        NumericKeypad(onKeyClick = { key ->
            isError = false // Clear error when user types
            if (key == "delete") {
                heightValueString = heightValueString.dropLast(1)
            } else {
                // Basic validation: allow only numbers and one decimal point
                if (key == "." && heightValueString.contains(".")) return@NumericKeypad
                if (heightValueString.length < 5) { // Limit length
                    heightValueString += key
                }
            }
        })
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val heightToSave = heightValueString.toFloatOrNull()
                if (heightToSave != null && heightToSave > 0) {
                    isError = false
                    // Load current profile, update heightValue and heightUnit, and save per-user
                    val profile = AppPreferences.getUserProfile(context)
                    val updatedProfile = profile.copy(heightValue = heightToSave, heightUnit = unit)
                    AppPreferences.saveUserProfile(context, updatedProfile)
                    Log.d("HeightPage", "Saved Height: $heightToSave $unit") // Debug log
                    Toast.makeText(context, "Height ($heightToSave $unit) saved!", Toast.LENGTH_SHORT).show()
                    navController.navigate("Weight") // Navigate to Weight page
                } else {
                    isError = true
                    Toast.makeText(context, "Please enter a valid height.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0C929))
        ) {
            Text(
                text = "NEXT",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// HeightTopBar, UnitButton, NumericKeypad, KeypadButton composables remain the same
// ... (paste them here from your original code if they are not in separate files) ...
@Composable
fun HeightTopBar(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = Color.Black,
            modifier = Modifier.size(24.dp).clickable {
                navController.popBackStack() // Navigate to previous screen (e.g., Age)
            }
        )
        Text(
            text = "Skip",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.clickable {
                navController.navigate("LoginPage") // Or WeightPage if skipping only this step
            }
        )
    }
}

@Composable
fun UnitButton(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (isSelected) Color(0xFFB0C929) else Color.Transparent)
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp)), // Ensure clip is applied
        color = if (isSelected) Color.White else Color.Black,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
fun NumericKeypad(onKeyClick: (String) -> Unit) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "delete") // Added decimal point
    )

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                row.forEach { key ->
                    KeypadButton(
                        key = key,
                        onClick = { onKeyClick(key) }
                    )
                }
            }
        }
    }
}

@Composable
fun KeypadButton(key: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 60.dp) // Adjusted size for better touch
            .clickable(onClick = onClick)
            .padding(4.dp), // Added padding around button
        contentAlignment = Alignment.Center
    ) {
        if (key == "delete") {
            Icon(
                imageVector = Icons.Default.Close, // Using Close for delete icon
                contentDescription = "Delete",
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        } else if (key.isNotEmpty()){ // Don't display empty string keys
            Text(
                text = key,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
