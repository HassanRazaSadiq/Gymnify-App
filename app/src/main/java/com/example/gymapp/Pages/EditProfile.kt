package com.example.gymapp.Pages

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
//import androidx.wear.compose.foundation.weight
import com.example.gymapp.R
import kotlinx.coroutines.launch
import com.example.gymapp.AppPreferences

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfile(modifier: Modifier = Modifier, navController: NavController) {
    val context = LocalContext.current

    // States for input fields
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var weightString by remember { mutableStateOf("") } // Keep as String for TextField
    var heightString by remember { mutableStateOf("") } // Keep as String for TextField
    var ageString by remember { mutableStateOf("") }    // Keep as String for TextField

    val genderOptions = listOf("Male", "Female", "Other", "Prefer not to say") // Expanded options
    var selectedGender by remember { mutableStateOf(genderOptions[0]) } // Default to first option
    var isGenderDropdownExpanded by remember { mutableStateOf(false) }

    var selectedWeightUnit by remember { mutableStateOf("KG") }
    var selectedHeightUnit by remember { mutableStateOf("CM") }

    var currentProfile by remember { mutableStateOf<AppPreferences.UserProfile?>(null) }

    // Load initial data from per-user profile
    LaunchedEffect(Unit) {
        val profile = AppPreferences.getUserProfile(context)
        currentProfile = profile
        fullName = profile.fullName ?: profile.username ?: ""
        email = profile.email ?: ""
        selectedGender = profile.gender ?: genderOptions[0]
        ageString = if (profile.age > 0) profile.age.toString() else ""
        heightString = if (profile.heightValue > 0f) profile.heightValue.toString().removeSuffix(".0") else ""
        selectedHeightUnit = profile.heightUnit ?: "CM"
        weightString = if (profile.weightValue > 0f) profile.weightValue.toString().removeSuffix(".0") else ""
        selectedWeightUnit = profile.weightUnit ?: "KG"
    }

    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val greenColor = Color(0xFFB0C929)
    val inputFieldBackgroundColor = Color(0xFFF0F0F0)

    fun saveProfileData() {
        if (fullName.isBlank()) {
            Toast.makeText(context, "Full Name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }
        val profile = currentProfile
        if (profile == null) {
            Toast.makeText(context, "Profile not loaded. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }
        val userProfile = AppPreferences.UserProfile(
            username = profile.username, // keep username unchanged
            fullName = fullName,
            email = email,
            gender = selectedGender,
            age = ageString.toIntOrNull() ?: 0,
            heightValue = heightString.toFloatOrNull() ?: 0f,
            heightUnit = selectedHeightUnit,
            weightValue = weightString.toFloatOrNull() ?: 0f,
            weightUnit = selectedWeightUnit,
            profileImageUri = profile.profileImageUri,
            exerciseRecords = profile.exerciseRecords
        )
        AppPreferences.saveUserProfile(context, userProfile)
        Log.d("EditProfile", "Profile Data Saved")
        scope.launch {
            snackbarHostState.showSnackbar(
                message = "Profile Updated Successfully",
                duration = SnackbarDuration.Short
            )
        }
        navController.navigate("Profile") {
            popUpTo("Profile") { inclusive = true }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { navController.popBackStack() }
                )
                Text(
                    text = "EDIT PROFILE",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.size(24.dp)) // To balance the title
            }
        },
        bottomBar = {
            Button(
                onClick = { saveProfileData() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = greenColor)
            ) {
                Text(
                    text = "SAVE",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Profile Picture (Static for now)
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5A80B)), // Example background
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.profile_icon),
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Form Fields
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                // Full Name
                Text(text = "Full Name", color = Color.Gray, fontSize = 14.sp)
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Full Name") },
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.Black,
                        unfocusedTextColor = Color.Black,
                        focusedBorderColor = greenColor,
                        unfocusedBorderColor = Color.LightGray,
                        focusedLabelColor = greenColor,
                        unfocusedLabelColor = Color.Gray,
                        cursorColor = Color.Black
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Email
                Text(text = "Email address", color = Color.Gray, fontSize = 14.sp)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputFieldBackgroundColor,
                        unfocusedContainerColor = inputFieldBackgroundColor,
                        disabledContainerColor = inputFieldBackgroundColor,
                        focusedBorderColor = greenColor,
                        unfocusedBorderColor = Color.LightGray,
                        cursorColor = greenColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Weight
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom // Align text field and selector
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Weight", color = Color.Gray, fontSize = 14.sp)
                        OutlinedTextField(
                            value = weightString,
                            onValueChange = { weightString = it.filter { char -> char.isDigit() || char == '.' } },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputFieldBackgroundColor,
                                unfocusedContainerColor = inputFieldBackgroundColor,
                                disabledContainerColor = inputFieldBackgroundColor,
                                focusedBorderColor = greenColor,
                                unfocusedBorderColor = Color.LightGray,
                                cursorColor = greenColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    UnitSelector( // Using your existing UnitSelector
                        units = listOf("LBS", "KG"),
                        selectedUnit = selectedWeightUnit,
                        onUnitSelected = { selectedWeightUnit = it },
                        selectedColor = greenColor
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Height
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Height", color = Color.Gray, fontSize = 14.sp)
                        OutlinedTextField(
                            value = heightString,
                            onValueChange = { heightString = it.filter { char -> char.isDigit() || char == '.' } },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = inputFieldBackgroundColor,
                                unfocusedContainerColor = inputFieldBackgroundColor,
                                disabledContainerColor = inputFieldBackgroundColor,
                                focusedBorderColor = greenColor,
                                unfocusedBorderColor = Color.LightGray,
                                cursorColor = greenColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    UnitSelector(
                        units = listOf("FEET", "CM"),
                        selectedUnit = selectedHeightUnit,
                        onUnitSelected = { selectedHeightUnit = it },
                        selectedColor = greenColor
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Gender
                Text(text = "Gender", color = Color.Gray, fontSize = 14.sp)
                ExposedDropdownMenuBox(
                    expanded = isGenderDropdownExpanded,
                    onExpandedChange = { isGenderDropdownExpanded = !isGenderDropdownExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField( // Changed to OutlinedTextField for consistency
                        value = selectedGender,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenderDropdownExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(
                            focusedTextColor = Color.Black,
                            unfocusedTextColor = Color.Black,
                            focusedContainerColor = inputFieldBackgroundColor,
                            unfocusedContainerColor = inputFieldBackgroundColor,
                            disabledContainerColor = inputFieldBackgroundColor,
                            focusedBorderColor = greenColor,
                            unfocusedBorderColor = Color.LightGray,
                            // focusedTrailingIconColor = Color.Black, // Default is usually fine
                            // unfocusedTrailingIconColor = Color.Black
                        ),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp) // Consistent shape
                    )
                    ExposedDropdownMenu(
                        expanded = isGenderDropdownExpanded,
                        onDismissRequest = { isGenderDropdownExpanded = false },
                        modifier = Modifier.background(inputFieldBackgroundColor) // Match background
                    ) {
                        genderOptions.forEach { genderOption ->
                            DropdownMenuItem(
                                text = { Text(genderOption, color = Color.Black) },
                                onClick = {
                                    selectedGender = genderOption
                                    isGenderDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Age
                Text(text = "Age", color = Color.Gray, fontSize = 14.sp)
                OutlinedTextField(
                    value = ageString,
                    onValueChange = { ageString = it.filter { char -> char.isDigit() } },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = inputFieldBackgroundColor,
                        unfocusedContainerColor = inputFieldBackgroundColor,
                        disabledContainerColor = inputFieldBackgroundColor,
                        focusedBorderColor = greenColor,
                        unfocusedBorderColor = Color.LightGray,
                        cursorColor = greenColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(color = Color.Black)
                )
                Spacer(modifier = Modifier.height(24.dp)) // Spacer before bottom bar
            }
        }
    }
}

// UnitSelector Composable (keep as is if it works for you)
@Composable
fun UnitSelector(
    units: List<String>,
    selectedUnit: String,
    onUnitSelected: (String) -> Unit,
    selectedColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.LightGray.copy(alpha = 0.3f)) // Slightly softer background
    ) {
        units.forEach { unit ->
            TextButton(
                onClick = { onUnitSelected(unit) },
                shape = RoundedCornerShape(8.dp), // Apply shape to button for better click area
                colors = ButtonDefaults.textButtonColors(
                    containerColor = if (selectedUnit == unit) selectedColor else Color.Transparent,
                    contentColor = if (selectedUnit == unit) Color.White else Color.DarkGray // Better contrast
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = unit,
                    fontWeight = FontWeight.Medium, // Slightly less bold for better balance
                    fontSize = 14.sp
                )
            }
        }
    }
}
