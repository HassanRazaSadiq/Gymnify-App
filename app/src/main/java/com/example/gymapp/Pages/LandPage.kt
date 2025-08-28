package com.example.gymapp.Pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import android.widget.Toast
import androidx.compose.foundation.background
import com.example.gymapp.R
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.Brush
import com.example.gymapp.AuthViewModel
import com.example.gymapp.AppPreferences
import androidx.navigation.compose.rememberNavController
@Composable
fun LandPage(modifier: Modifier = Modifier, navController: NavController,isLoggedIn: Boolean) {
    // Image resource (replace this with your own image resource or asset)
    val logo: Painter = painterResource(id = R.drawable.logo)  // Replace with your actual image path

    // Color Scheme
    val buttonColor = Color(0xFF000000) // google_yellow_FBBC05

    // Button Hover effect
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered = interactionSource.collectIsPressedAsState().value

    // Gradient background from light green to dark green
    val gradientBackground = Brush.verticalGradient(
        colors = listOf(Color(0xFFE7FAFE), Color(0xFFB0C929)) // Gradient from light blue to green
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(gradientBackground), // Applying the gradient background
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Logo Image
        Image(
            painter = logo,
            contentDescription = "Gymnify Logo",
            modifier = Modifier
                .size(500.dp)
                .padding(bottom = 32.dp)
        )

        // Button: "Let's Start"
        Button(
            onClick = {
                if (isLoggedIn){
                    navController.navigate("Home")
                } else {
                    navController.navigate("LoginPage")
                }

            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isHovered) buttonColor.copy(alpha = 0.8f) else buttonColor
            ),
            interactionSource = interactionSource,
            elevation = ButtonDefaults.buttonElevation(8.dp)
        ) {
            Text(
                text = "LET'S START",
                color = Color.White,
                fontSize = 18.sp
            )
        }
    }
}
