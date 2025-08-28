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
fun Weight(modifier: Modifier = Modifier, navController: NavController) {
    var weightValueString by remember { mutableStateOf("") } // Keep as String for input
    var unit by remember { mutableStateOf("KG") } // Default unit
    var isError by remember { mutableStateOf(false) }
    val context = LocalContext.current // Get context

    // Load previously saved weight and unit when the screen is first composed
    LaunchedEffect(Unit) {
        val profile = AppPreferences.getUserProfile(context)
        if (profile.weightValue > 0f) {
            weightValueString = profile.weightValue.toString().removeSuffix(".0")
        }
        profile.weightUnit?.let { unit = it }
    }

    // Move helper composables outside or make them top-level if not already
    // For this example, I'll keep them nested but it's often better to extract them

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        WeightTopBarInternal(navController) // Changed to avoid conflict if you extract

        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Step 5 of 5",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "HOW MUCH DO YOU WEIGH?", // Corrected spelling
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold, // Made Bold to match others
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
                UnitButtonInternal(label = "KG", isSelected = unit == "KG") { unit = "KG" }
                UnitButtonInternal(label = "LBS", isSelected = unit == "LBS") { unit = "LBS" }
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
                        text = weightValueString.ifEmpty { "0" },
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
                    text = "Please enter a valid weight value.",
                    color = Color.Red,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        NumericKeypadInternal(onKeyClick = { key ->
            isError = false // Clear error when user types
            if (key == "delete") {
                weightValueString = weightValueString.dropLast(1)
            } else {
                // Basic validation: allow only numbers and one decimal point
                if (key == "." && weightValueString.contains(".")) return@NumericKeypadInternal
                if (weightValueString.length < 5) { // Limit length
                    weightValueString += key
                }
            }
        })
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val weightToSave = weightValueString.toFloatOrNull()
                if (weightToSave != null && weightToSave > 0) {
                    isError = false
                    // Load current profile, update weightValue and weightUnit, and save per-user
                    val profile = AppPreferences.getUserProfile(context)
                    val updatedProfile = profile.copy(weightValue = weightToSave, weightUnit = unit)
                    AppPreferences.saveUserProfile(context, updatedProfile)
                    AppPreferences.setNeedsOnboarding(context, false) // Onboarding complete
                    Log.d("WeightPage", "Saved Weight: $weightToSave $unit") // Debug log
                    Toast.makeText(context, "Weight ($weightToSave $unit) saved!", Toast.LENGTH_SHORT).show()
                    navController.navigate("LoginPage") { // Navigate to LoginPage and clear back stack
                        popUpTo(navController.graph.startDestinationId) {
                            inclusive = true
                        }
                    }
                } else {
                    isError = true
                    Toast.makeText(context, "Please enter a valid weight.", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0C929))
        ) {
            Text(
                text = "Finish",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

// It's generally better to move these helper composables to their own file
// or to the top level of this file if they are only used here.
// I've renamed them with "Internal" suffix to avoid potential conflicts
// if you decide to make them top-level later and want to reuse names.

@Composable
private fun WeightTopBarInternal(navController: NavController) { // Added "Internal"
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
                navController.popBackStack() // Go to previous screen (Height)
            }
        )
        Text(
            text = "Skip",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal, // Was Bold in Height, making consistent
            color = Color.Black,
            modifier = Modifier.clickable {
                navController.navigate("Home") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        )
    }
}

@Composable
private fun UnitButtonInternal(label: String, isSelected: Boolean, onClick: () -> Unit) { // Added "Internal"
    Text(
        text = label,
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(if (isSelected) Color(0xFFB0C929) else Color.Transparent)
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp)),
        color = if (isSelected) Color.White else Color.Black,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun NumericKeypadInternal(onKeyClick: (String) -> Unit) { // Added "Internal"
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "delete") // Added decimal point for weight too
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
                    KeypadButtonInternal( // Changed to KeypadButtonInternal
                        key = key,
                        onClick = { onKeyClick(key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun KeypadButtonInternal(key: String, onClick: () -> Unit) { // Added "Internal"
    Box(
        modifier = Modifier
            .size(width = 80.dp, height = 60.dp) // Consistent with Height page
            .clickable(onClick = onClick)
            .padding(4.dp),
        contentAlignment = Alignment.Center
    ) {
        if (key == "delete") {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Delete",
                tint = Color.Black,
                modifier = Modifier.size(32.dp)
            )
        } else if (key.isNotEmpty()){
            Text(
                text = key,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
