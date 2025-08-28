package com.example.gymapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gymapp.AuthState
import com.example.gymapp.Pages.Age
import com.example.gymapp.Pages.ChangePassword
import com.example.gymapp.Pages.EditProfile
import com.example.gymapp.Pages.Gender
import com.example.gymapp.Pages.Height
import com.example.gymapp.Pages.Home
import com.example.gymapp.Pages.LandPage
import com.example.gymapp.Pages.LoginPage
import com.example.gymapp.Pages.Profile
import com.example.gymapp.Pages.Setting
import com.example.gymapp.Pages.Signup
import com.example.gymapp.Pages.StandingKneeToElbow
import com.example.gymapp.Pages.StandingKneeToElbowInstructions
import com.example.gymapp.Pages.StandingToeTouches
import com.example.gymapp.Pages.Weight
import com.example.gymapp.Pages.GentleJumpingJacks
import com.example.gymapp.Pages.KneeLifting
import com.example.gymapp.Pages.Lunges
import com.example.gymapp.Pages.OverheadPress
import com.example.gymapp.Pages.StandingSideBends

@Composable
fun Navigation(
    modifier: Modifier,
    authViewModel: AuthViewModel,
    isDarkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit
) {
    val localNavController = rememberNavController()
    val authState = authViewModel.authState.observeAsState().value
    val context = androidx.compose.ui.platform.LocalContext.current
    val needsOnboarding = com.example.gymapp.AppPreferences.getNeedsOnboarding(context)
    val startDestination = when {
        needsOnboarding -> "Age"
        authState is AuthState.Authenticated -> "LandPage"
        else -> "LoginPage"
    }
    NavHost(
        navController = localNavController,
        startDestination = startDestination
    ) {
        composable("LoginPage") {
            LoginPage(modifier, localNavController, authViewModel)
        }
        composable("LandPage") {
            LandPage(modifier, localNavController, authState is AuthState.Authenticated)
        }
        composable("SignUp") {
            Signup(modifier, localNavController, authViewModel)
        }
        composable("Gender") {
            Gender(modifier, localNavController)
        }
        composable("Age") {
            Age(modifier, localNavController)
        }
        composable("Height") {
            Height(modifier, localNavController)
        }
        composable("Weight") {
            Weight(modifier, localNavController)
        }
        composable("Home") {
            Home(modifier, localNavController, authViewModel)
        }
        composable("Setting") {
            Setting(modifier, localNavController, isDarkMode, onDarkModeChange)
        }
        composable("Profile") {
            Profile(modifier, localNavController)
        }
        composable("EditProfile") {
            EditProfile(modifier, localNavController)
        }

        composable("StandingKneeToElbow") {
            @androidx.camera.core.ExperimentalGetImage
            StandingKneeToElbow(modifier, localNavController)
        }
        composable("StandingKneeToElbowInstructions") {
            StandingKneeToElbowInstructions(modifier, localNavController)
        }
        composable("StandingToeTouches") {
            StandingToeTouches(modifier, localNavController)
        }
        composable("GentleJumpingJacks") {
            GentleJumpingJacks(modifier)
        }
        composable("KneeLifting") {
            @androidx.camera.core.ExperimentalGetImage
            KneeLifting(modifier)
        }
        composable("StandingSideBends") {
            @androidx.camera.core.ExperimentalGetImage
            StandingSideBends(modifier,localNavController)
        }
        composable("OverheadPress") {
            @androidx.camera.core.ExperimentalGetImage
            OverheadPress(modifier)
        }
        composable("Lunges") {
            @androidx.camera.core.ExperimentalGetImage
            Lunges(modifier, localNavController)
        }
        composable("ChangePassword") {
            ChangePassword(modifier, localNavController)
        }


    }
}