package com.example.gymapp.Pages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import androidx.camera.view.PreviewView
import java.util.Locale
import androidx.navigation.NavController
import com.example.gymapp.AppPreferences

@Composable
fun StandingSideBends(
    modifier: Modifier = Modifier,
    navController: NavController,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    blueTextColor: Color = Color(0xFF1976D2)
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()
    var debugMode by remember { mutableStateOf(false) }
    var ttsInstance: TextToSpeech? by remember { mutableStateOf(null) }
    val tts = remember {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
            }
        }.also { ttsInstance = it }
    }
    DisposableEffect(Unit) {
        onDispose {
            tts.stop()
            tts.shutdown()
        }
    }
    var showInstructions by remember { mutableStateOf(true) }
    var feedback by remember { mutableStateOf("Please stand in frame for calibration") }
    var debugInfo by remember { mutableStateOf("") }
    var accurateReps by remember { mutableStateOf(0) }
    var isBending by remember { mutableStateOf(false) }
    var wasGoodFormDuringRep by remember { mutableStateOf(true) }
    var calibrationComplete by remember { mutableStateOf(false) }
    var exerciseTimer by remember { mutableStateOf(0) }
    var lastSide by remember { mutableStateOf("") }
    var lastCompletedSide by remember { mutableStateOf("") }
    var isExerciseStarted by remember { mutableStateOf(false) }
    var calibrationFrames by remember { mutableStateOf(0) }
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var showSettingsMessage by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted && ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            showSettingsMessage = true
        }
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            cameraPermissionGranted = true
        }
    }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(cameraPermissionGranted, showInstructions) {
        if (cameraPermissionGranted && !showInstructions) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                    val preview = Preview.Builder().build()
                    val cameraSelector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build()
                    preview.setSurfaceProvider(previewView.surfaceProvider)
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    Log.e("SideBends", "Camera initialization failed", e)
                }
            }, ContextCompat.getMainExecutor(context))
        }
        onDispose {
            // Optionally unbind camera here if needed
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Standing Side Bends",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (showInstructions) {
            val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "How to Perform Standing Side Bends:",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "1. Stand with feet shoulder-width apart and arms at your sides.\n" +
                            "3. Return to center, then bend to the right.\n" +
                            "4. Keep your hips stable and avoid leaning forward or backward.\n" +
                            "5. Repeat, alternating sides.",
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Tip: Move slowly and keep your core engaged for best results.",
                    style = MaterialTheme.typography.bodySmall,
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showInstructions = false },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor,
                    contentColor = Color.Black
                )
            ) {
                Text("Start Exercise")
            }
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Debug Mode: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Black
                )
                Switch(
                    checked = debugMode,
                    onCheckedChange = { debugMode = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = primaryColor,
                        checkedTrackColor = primaryColor.copy(alpha = 0.5f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (cameraPermissionGranted) {

                LaunchedEffect(Unit) {
                    while (true) {
                        delay(1000)
                        if (isExerciseStarted) exerciseTimer++
                    }
                }

                Text(
                    "Camera active. Follow the instructions for accurate counting.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = blueTextColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                AndroidView(
                    factory = { previewView },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Accurate reps should be shown here, under camera view and above reset button
                Text(
                    "Accurate reps: $accurateReps",
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.Black,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center
                )

                if (debugMode && debugInfo.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Text(
                            debugInfo,
                            style = MaterialTheme.typography.bodySmall,
                            color = blueTextColor,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            feedback.contains("Perfect") -> primaryColor.copy(alpha = 0.2f)
                            feedback.contains("avoid twisting") -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                            feedback.contains("calibrat", ignoreCase = true) -> Color.LightGray
                            else -> Color.White
                        }
                    )
                ) {
                    Text(
                        feedback,
                        style = MaterialTheme.typography.bodyLarge,
                        color = blueTextColor,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        // Reset all variables
                        accurateReps = 0
                        isBending = false
                        wasGoodFormDuringRep = true
                        calibrationComplete = false
                        exerciseTimer = 0
                        lastSide = ""
                        lastCompletedSide = ""
                        isExerciseStarted = false
                        calibrationFrames = 0
                        feedback = "Please stand in frame for calibration"
                        debugInfo = ""
                        // Restart the camera analysis
                        val cameraProviderFuture =
                            androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                cameraProvider.unbindAll()
                                // Re-bind the camera with the same use cases
                                val preview = androidx.camera.core.Preview.Builder().build()
                                val cameraSelector = androidx.camera.core.CameraSelector.Builder()
                                    .requireLensFacing(androidx.camera.core.CameraSelector.LENS_FACING_FRONT)
                                    .build()
                                preview.setSurfaceProvider(previewView.surfaceProvider)

                                val imageAnalysis = androidx.camera.core.ImageAnalysis.Builder()
                                    .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                                    // Same analysis code as above
                                }

                                cameraProvider.bindToLifecycle(
                                    context as androidx.lifecycle.LifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                Log.e("SideBends", "Camera reset failed", e)
                            }
                        }, ContextCompat.getMainExecutor(context))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Reset Exercise")
                }
            } else {
                Text(
                    "Camera permission is required to use this feature. Please grant permission in settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Grant Camera Permission")
                }

                if (showSettingsMessage) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Permission denied. You can enable camera permission in app settings.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }

    // Save exercise record when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            if (accurateReps > 0) {
                AppPreferences.addExerciseRecord(
                    context,
                    AppPreferences.ExerciseRecord(
                        name = "Standing Side Bends",
                        timestamp = System.currentTimeMillis(),
                        reps = accurateReps,
                        durationSeconds = exerciseTimer
                    )
                )
            }
        }
    }
}
