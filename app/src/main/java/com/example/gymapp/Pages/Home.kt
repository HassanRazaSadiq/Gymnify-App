package com.example.gymapp.Pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.gymapp.R
import com.example.gymapp.AuthViewModel

data class ExerciseCategory(val name: String, val iconResId: Int)

val exerciseCategories = listOf(
    ExerciseCategory("Overhead Press", R.drawable.overheadpress),
    ExerciseCategory("Standing Side Bends", R.drawable.yoga_icon),
    ExerciseCategory("Standing Knee To Elbow", R.drawable.kneetoelbow),
    ExerciseCategory("Standing Toe Touches", R.drawable.toe),
    ExerciseCategory("Gentle Jumping Jacks", R.drawable.jumping),
    ExerciseCategory("Knee Lifting", R.drawable.kneeliftingtuck),
    ExerciseCategory("Lunges", R.drawable.lunges)
)

@Composable
fun Home(modifier: Modifier, navController: NavController, authViewModel: AuthViewModel) {
    var selectedDestination by remember { mutableStateOf("Home") }
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            BottomBar(
                navController = navController,
                selectedDestination = selectedDestination,
                onDestinationSelected = { destination ->
                    selectedDestination = destination
                    navController.navigate(destination)
                }
            )
        },
        containerColor = Color(0xFFFFFFFF),
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(8.dp)
        ) {
            // Top Bar with Menu and Title
            TopAppBar()

            // Search Bar
            SearchBar(
                searchText = searchText,
                onTextChange = { searchText = it }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable grid of exercise categories
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(32.dp)
            ) {
                val filteredCategories = exerciseCategories.filter {
                    it.name.contains(searchText, ignoreCase = true)
                }
                items(filteredCategories.size) { index ->
                    val category = filteredCategories[index]
                    ExerciseItem(category = category, onClick = {
                        when (category.name) {
                            "Overhead Press" -> navController.navigate("OverheadPress")
                            "Standing Side Bends" -> navController.navigate("StandingSideBends")
                            "Standing Knee To Elbow" -> navController.navigate("StandingKneeToElbowInstructions")
                            "Standing Toe Touches" -> navController.navigate("StandingToeTouches")
                            "Gentle Jumping Jacks" -> navController.navigate("GentleJumpingJacks")
                            "Knee Lifting" -> navController.navigate("KneeLifting")
                            "Lunges" -> navController.navigate("Lunges")
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun TopAppBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Menu,
            contentDescription = "Menu",
            modifier = Modifier.size(24.dp),
            tint = Color.Black // Set icon tint to black
        )
        Text(
            text = "FULL EXERCISE",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black // Set text color to black
        )
        Spacer(modifier = Modifier.size(24.dp)) // Placeholder for balance
    }
}

@Composable
fun SearchBar(searchText: String, onTextChange: (String) -> Unit) {
    OutlinedTextField(
        value = searchText,
        onValueChange = onTextChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        placeholder = { Text("Search", color = Color.Gray) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.Gray
            )
        },
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            // Container colors
            focusedContainerColor = Color(0xFFF0F0F0),
            unfocusedContainerColor = Color(0xFFF0F0F0),

            // Border colors
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,

            // Text colors
            focusedTextColor = Color.Black,      // Text color when focused
            unfocusedTextColor = Color.Black,    // Text color when not focused
            cursorColor = Color.Black,           // Cursor color
            focusedLabelColor = Color.Black.copy(alpha = 0.6f),  // Label color when focused
            unfocusedLabelColor = Color.Black.copy(alpha = 0.6f) // Label color when not focused
        ),
        shape = RoundedCornerShape(0.dp)
    )
}

@Composable
fun ExerciseItem(category: ExerciseCategory, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() }
            .background(Color.White, shape = RoundedCornerShape(8.dp)),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = category.iconResId),
            contentDescription = category.name,
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = category.name,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.Black
        )
    }
}

@Composable
fun BottomBar(
    navController: NavController,
    selectedDestination: String,
    onDestinationSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = Color.White,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            )
            .height(72.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BottomBarItem(
            iconResId = R.drawable.home_icon,
            label = "Home",
            navController = navController,
            destination = "Home",
            isSelected = selectedDestination == "Home"
        )
        BottomBarItem(
            iconResId = R.drawable.settings_icon,
            label = "Settings",
            navController = navController,
            destination = "Setting",
            isSelected = selectedDestination == "Setting"
        )
        BottomBarItem(
            iconResId = R.drawable.profile_icon,
            label = "Profile",
            navController = navController,
            destination = "Profile",
            isSelected = selectedDestination == "Profile"
        )
    }
}

@Composable
fun BottomBarItem(
    iconResId: Int,
    label: String,
    navController: NavController,
    destination: String,
    isSelected: Boolean
) {
    val iconTint = if (isSelected) Color(0xFF191919) else Color(0xFFBFBFC3)
    val labelColor = if (isSelected) Color(0xFF191919) else Color(0xFFBFBFC3)
    Column(
        modifier = Modifier
            .clickable { navController.navigate(destination) }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(28.dp)
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 12.sp
        )
    }
}
