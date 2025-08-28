package com.example.gymapp.Pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymapp.R // Import your R file to access drawables
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast

@Composable
fun Setting(
    modifier: Modifier = Modifier,
    navController: NavController,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Using a drawable for the back arrow
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_back), // Replace with your back arrow drawable
                contentDescription = "Back",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { navController.popBackStack() }
            )
            Text(
                text = "APP SETTING",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Settings list
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Reminder toggle
            var isReminderEnabled by remember { mutableStateOf(false) }
            SettingItemWithToggle(
                iconResId = R.drawable.ic_notifications, // Replace with your notifications drawable
                label = "Workout Reminder",
                isEnabled = isReminderEnabled,
                onToggle = { isReminderEnabled = it }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clickable { navController.navigate("ChangePassword") },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_lock),
                    contentDescription = "Change Password",
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Change Password",
                    fontSize = 18.sp,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // "Upgrade Premium" button
        Button(
            onClick = {
                navController.navigate("Home")
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB0C929))
        ) {
            Text(
                text = "Save",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sign Out button
        OutlinedButton(
            onClick = {
                com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                navController.navigate("LoginPage") {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                    launchSingleTop = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
        ) {
            Text("Sign Out", color = Color.DarkGray, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Delete Account button with confirmation dialog
        var showDeleteDialog by remember { mutableStateOf(false) }
        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
        ) {
            Text("Delete Account", color = Color(0xFFD32F2F), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Account", fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete your account? This action cannot be undone.") },
                confirmButton = {
                    OutlinedButton(
                        onClick = {
                            val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                            if (user != null) {
                                user.delete()
                                    .addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            com.example.gymapp.AppPreferences.clearCurrentUserData(context)
                                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                                            showDeleteDialog = false
                                            navController.navigate("LoginPage") {
                                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                                launchSingleTop = true
                                            }
                                        } else {
                                            Toast.makeText(context, "Account deletion failed. Please re-authenticate and try again.", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            } else {
                                Toast.makeText(context, "No user signed in.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Text("Delete", color = Color(0xFFD32F2F))
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = { showDeleteDialog = false },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.DarkGray)
                    ) {
                        Text("Cancel", color = Color.DarkGray)
                    }
                }
            )
        }

        // Version number
        Text(
            text = "Version 1.0",
            fontSize = 14.sp,
            color = Color.Gray
        )
    }
}

@Composable
fun SettingItem(iconResId: Int, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = label,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            fontSize = 18.sp,
            color = Color.Black
        )
    }
}

@Composable
fun SettingItemWithToggle(iconResId: Int, label: String, isEnabled: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 18.sp,
                color = Color.Black
            )
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFB0C929),
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray
            )
        )
    }
}

@Composable
fun SettingItemWithText(iconResId: Int, label: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = iconResId),
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = label,
                fontSize = 18.sp,
                color = Color.Black
            )
        }
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.Gray
        )
    }
}
