package com.example.gymapp.Pages

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.gymapp.AppPreferences
import com.example.gymapp.R

@Composable
fun Profile(modifier: Modifier = Modifier, navController: NavController) {
    val context = LocalContext.current

    var displayedUsername by remember { mutableStateOf("User") }
    var displayedFullName by remember { mutableStateOf("") } // For full name
    var displayedGender by remember { mutableStateOf("") }   // For gender
    var userAge by remember { mutableStateOf<Int?>(null) }
    var userHeightValue by remember { mutableStateOf<Float?>(null) }
    var userHeightUnit by remember { mutableStateOf<String?>("cm") }
    var userWeightValue by remember { mutableStateOf<Float?>(null) }
    var userWeightUnit by remember { mutableStateOf<String?>("kg") }

    // Profile picture URI state
    var profileImageUri by remember { mutableStateOf<String?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        profileImageUri = uri?.toString()
        if (profileImageUri != null) {
            AppPreferences.saveProfileImageUri(context, profileImageUri)
        }
    }

    var exerciseRecords by remember { mutableStateOf(listOf<AppPreferences.ExerciseRecord>()) }

    // Load user profile data from AppPreferences
    LaunchedEffect(Unit) {
        val profile = AppPreferences.getUserProfile(context)
        displayedUsername = profile.username ?: "User"
        displayedFullName = profile.fullName ?: ""
        displayedGender = profile.gender ?: ""
        userAge = profile.age
        userHeightValue = profile.heightValue
        userHeightUnit = profile.heightUnit ?: "cm"
        userWeightValue = profile.weightValue
        userWeightUnit = profile.weightUnit ?: "kg"
        profileImageUri = AppPreferences.getProfileImageUri(context)?.toString()
        exerciseRecords = profile.exerciseRecords
        Log.d("ProfileScreen", "Loaded profileImageUri: $profileImageUri")
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { navController.popBackStack() }
                )
                Text(
                    text = "PROFILE",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_edit),
                    contentDescription = "Edit Profile",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { navController.navigate("EditProfile") }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Profile Picture with clickable overlay
            Box(contentAlignment = Alignment.BottomEnd) {
                val uri = profileImageUri
                val isValidProfileImage = uri != null && uri.isNotBlank() && (uri.startsWith("content://") || uri.startsWith("file://") || uri.startsWith("http"))
                Image(
                    painter = if (isValidProfileImage) rememberAsyncImagePainter(uri) else painterResource(id = R.drawable.profile_icon),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color(0xFFB0C929), CircleShape)
                )
                IconButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFB0C929))
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_edit),
                        contentDescription = "Edit Profile Picture",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // Display Full Name (or Username if full name is not set)
            Text(
                text = if (displayedFullName.isNotBlank() && displayedFullName != displayedUsername) displayedFullName.uppercase() else displayedUsername.uppercase(),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            // Display Gender
            Text(
                text = displayedGender,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            // Profile Details (Weight, Height, Age) - compact row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProfileDetailItem(
                    label = "Weight",
                    value = userWeightValue?.toString()?.removeSuffix(".0") ?: "--",
                    unit = userWeightUnit ?: ""
                )
                ProfileDetailItem(
                    label = "Height",
                    value = userHeightValue?.toString()?.removeSuffix(".0") ?: "--",
                    unit = userHeightUnit ?: ""
                )
                ProfileDetailItem(
                    label = "Age",
                    value = userAge?.toString() ?: "--",
                    unit = ""
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color.LightGray, thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Exercise History", style = MaterialTheme.typography.titleMedium.copy(color = Color(0xFFB0C929), fontWeight = FontWeight.Bold))
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (exerciseRecords.isEmpty()) {
            item {
                Text("No history yet.", style = MaterialTheme.typography.bodySmall)
            }
        } else {
            items(exerciseRecords.sortedByDescending { it.timestamp }) { record ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F7F7))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = record.name, style = MaterialTheme.typography.bodyMedium.copy(color = Color(0xFF2196F3), fontWeight = FontWeight.Bold))
                            Text(text = "Reps: ${record.reps}", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                            Text(text = "Date: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(record.timestamp))}", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                            if (record.durationSeconds > 0) {
                                val m = record.durationSeconds / 60
                                val s = record.durationSeconds % 60
                                Text(text = "Time: ${"%d:%02d".format(m, s)}", style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray))
                            }
                        }
                        IconButton(onClick = {
                            val updatedRecords = exerciseRecords.filter { it.timestamp != record.timestamp }
                            exerciseRecords = updatedRecords
                            AppPreferences.setCurrentUserExerciseRecords(context, updatedRecords)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Record", tint = Color.Red, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0C929), contentColor = Color.White)
            ) {
                Text("BACK TO HOME", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ProfileDetailItem(label: String, value: String, unit: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 8.dp) // Add some padding for items
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically // Align value and unit
        ) {
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(4.dp))
            if (unit.isNotEmpty()) {
                Text(
                    text = unit,
                    fontSize = 16.sp, // Slightly smaller for unit
                    color = Color.Black,
                    modifier = Modifier.align(Alignment.Bottom).padding(bottom = 2.dp) // Align unit text nicely
                )
            }
        }
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
