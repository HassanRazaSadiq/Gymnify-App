package com.example.gymapp.Pages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.gymapp.AppPreferences
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.delay
import java.util.*
import androidx.navigation.NavController
@Composable
fun Lunges(modifier: Modifier = Modifier, navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var showSettingsMessage by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(true) }
    var debugMode by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFFB0C929)
    val blueTextColor = Color(0xFF2196F3)

    // Permission launcher
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (!granted && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            showSettingsMessage = true
        }
    }

    // Check initial permission
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraPermissionGranted = true
        }
    }

    // TTS state
    var ttsInstance: TextToSpeech? by remember { mutableStateOf(null) }
    var lastSpokenFeedback by remember { mutableStateOf("") }
    var lastSpeechTime by remember { mutableStateOf(0L) }
    val minSpeechIntervalMs = 1200L

    // Initialize TTS
    LaunchedEffect(Unit) {
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
            }
        }
    }

    // Cleanup TTS
    DisposableEffect(Unit) {
        onDispose {
            ttsInstance?.stop()
            ttsInstance?.shutdown()
        }
    }

    val scrollState = rememberScrollState()

    // Session state variables
    var accurateReps by remember { mutableStateOf(0) }
    var calibrationComplete by remember { mutableStateOf(false) }
    var exerciseTimer by remember { mutableStateOf(0) }
    var neutralHipHeight by remember { mutableStateOf(0f) }
    var calibrationFrames by remember { mutableStateOf(0) }
    var exerciseState by remember { mutableStateOf("standing") } // "standing" or "lunge"
    var lungeStartTime by remember { mutableStateOf(0L) }
    val minLungeDurationMs = 600L // Minimum time in lunge position to count a rep
    var repCountedInCurrentLunge by remember { mutableStateOf(false) }

    // Exercise history is shown in Profile only; no in-screen list here.

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header with title and info icon
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Stationary Lunges",
                style = MaterialTheme.typography.headlineMedium,
                color = blueTextColor,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showInstructions = !showInstructions }) {
                Icon(Icons.Default.Info, contentDescription = "Instructions")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Instructions Section
        if (showInstructions) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "How to Perform Stationary Lunges:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "1. Stand straight with feet hip-width apart\n" +
                                "2. Step one foot forward, keeping the other foot in place\n" +
                                "3. Lower your hips until both knees are bent at 90-degree angles\n" +
                                "4. Keep your front knee directly above your ankle\n" +
                                "5. Push through your front heel to return to the starting position\n" +
                                "6. Alternate legs with each repetition",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tip: Keep your upper body straight and shoulders back.",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showInstructions = false },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
            ) {
                Text("Start Exercise")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Debug mode toggle
        if (!showInstructions) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Debug Mode: ", style = MaterialTheme.typography.bodySmall, color = blueTextColor)
                Switch(
                    checked = debugMode,
                    onCheckedChange = { debugMode = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.5f))
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Camera and exercise tracking
        if (!showInstructions && cameraPermissionGranted) {
            var feedback by remember { mutableStateOf("Please stand in frame for calibration") }
            var debugInfo by remember { mutableStateOf("") }

            // Timer for exercise duration
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1000)
                    if (calibrationComplete) exerciseTimer++
                }
            }

            // TTS for feedback
            LaunchedEffect(feedback) {
                val now = System.currentTimeMillis()
                val importantFeedback = listOf("Perfect form", "Rep counted", "Good lunge", "calibration complete")
                val shouldSpeak = importantFeedback.any { feedback.contains(it, ignoreCase = true) }
                if (shouldSpeak && feedback != lastSpokenFeedback && feedback.isNotBlank()) {
                    if (ttsInstance != null && now - lastSpeechTime > minSpeechIntervalMs) {
                        ttsInstance?.speak(feedback, TextToSpeech.QUEUE_FLUSH, null, null)
                        lastSpokenFeedback = feedback
                        lastSpeechTime = now
                    }
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
                factory = { ctx: Context ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build()
                        val cameraSelector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build()

                        preview.setSurfaceProvider(previewView.surfaceProvider)

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            @ExperimentalGetImage
                            val mediaImage = imageProxy.image
                            @ExperimentalGetImage
                            if (mediaImage != null) {
                                @ExperimentalGetImage
                                val inputImage = InputImage.fromMediaImage(
                                    mediaImage,
                                    imageProxy.imageInfo.rotationDegrees
                                )

                                val options = PoseDetectorOptions.Builder()
                                    .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                                    .build()
                                val poseDetector = PoseDetection.getClient(options)

                                @ExperimentalGetImage
                                poseDetector.process(inputImage)
                                    .addOnSuccessListener { pose ->
                                        // Get key landmarks
                                        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                                        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
                                        val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
                                        val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

                                        if (leftHip != null && rightHip != null &&
                                            leftKnee != null && rightKnee != null) {

                                            // Calculate average hip height
                                            val avgHipHeight = (leftHip.position.y + rightHip.position.y) / 2

                                            // Calculate knee angles
                                            val leftKneeAngle = calculateAngle(
                                                leftHip.position.x, leftHip.position.y,
                                                leftKnee.position.x, leftKnee.position.y,
                                                leftKnee.position.x, leftKnee.position.y - 100f // Create a vertical reference point
                                            )

                                            val rightKneeAngle = calculateAngle(
                                                rightHip.position.x, rightHip.position.y,
                                                rightKnee.position.x, rightKnee.position.y,
                                                rightKnee.position.x, rightKnee.position.y - 100f // Create a vertical reference point
                                            )

                                            // Calibration phase
                                            if (!calibrationComplete) {
                                                calibrationFrames++
                                                if (calibrationFrames >= 30) {
                                                    neutralHipHeight = avgHipHeight
                                                    calibrationComplete = true
                                                    feedback = "Calibration complete! Start lunges"
                                                } else {
                                                    val progress = (calibrationFrames * 100 / 30)
                                                    feedback = "Calibrating... $progress% complete. Stand straight."
                                                }
                                            }
                                            // Exercise phase
                                            else {
                                                // Calculate hip drop (should be lower than neutral)
                                                val hipDrop = (avgHipHeight - neutralHipHeight) / neutralHipHeight

                                                // Check if knees are bent (lunge position)
                                                val isKneesBent = leftKneeAngle < 160 || rightKneeAngle < 160

                                                // Check if hips are dropped (lunge position)
                                                val isHipsDropped = hipDrop > 0.08f // Hips should be at least 8% lower

                                                // Debug information
                                                if (debugMode) {
                                                    debugInfo = """
                                                    Hip Drop: ${String.format(Locale.US, "%.1f", hipDrop * 100)}%
                                                    Knee Angles: L=${String.format(Locale.US, "%.1f", leftKneeAngle)}° R=${String.format(Locale.US, "%.1f", rightKneeAngle)}°
                                                    State: $exerciseState
                                                    """.trimIndent()
                                                }

                                                val now = System.currentTimeMillis()
                                                val isProperLunge = isKneesBent && isHipsDropped

                                                when (exerciseState) {
                                                    "standing" -> {
                                                        if (isProperLunge) {
                                                            exerciseState = "lunge"
                                                            lungeStartTime = now
                                                            repCountedInCurrentLunge = false
                                                            feedback = "Good lunge! Hold the position"
                                                        } else {
                                                            feedback = "Perform a lunge: step one foot forward and bend both knees"
                                                        }
                                                    }
                                                    "lunge" -> {
                                                        if (isProperLunge) {
                                                            if (!repCountedInCurrentLunge && (now - lungeStartTime > minLungeDurationMs)) {
                                                                accurateReps++
                                                                repCountedInCurrentLunge = true
                                                                feedback = "Perfect form! Rep: $accurateReps"
                                                            } else if (!repCountedInCurrentLunge) {
                                                                feedback = "Hold the lunge to count rep"
                                                            } else {
                                                                feedback = "Release to count next rep"
                                                            }
                                                        } else {
                                                            exerciseState = "standing"
                                                            feedback = "Return to standing to start next rep"
                                                        }
                                                    }
                                                }
                                            }

                                        } else {
                                            feedback = "Please make sure your lower body is visible"
                                            calibrationComplete = false
                                            calibrationFrames = 0
                                        }
                                    }
                                    .addOnFailureListener {
                                        feedback = "Adjust position - can't detect your pose"
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (_: Exception) {
                            // Handle camera binding exception
                        }

                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Debug information
            if (debugMode && debugInfo.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        debugInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Feedback card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        feedback.contains("Perfect") -> primaryColor.copy(alpha = 0.2f)
                        feedback.contains("Good lunge") -> primaryColor.copy(alpha = 0.2f)
                        feedback.contains("calibrat", ignoreCase = true) -> MaterialTheme.colorScheme.secondaryContainer
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

            // Display total reps
            Text(
                "Total Reps: $accurateReps",
                style = MaterialTheme.typography.headlineSmall,
                color = blueTextColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Reset button
            Button(
                onClick = {
                    calibrationComplete = false
                    calibrationFrames = 0
                    accurateReps = 0
                    exerciseTimer = 0
                    feedback = "Please stand in frame for calibration"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
            ) {
                Text("Reset Exercise")
            }

            // Spacer before settings and permissions
            Spacer(modifier = Modifier.height(16.dp))
        }
        else if (!showInstructions && !cameraPermissionGranted) {
            Text(
                "Camera permission is required to detect your exercise form.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Red
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { launcher.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
            ) {
                Text("Allow Camera Access")
            }
            if (showSettingsMessage) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Permission denied. Please enable camera access in app settings.",
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Save exercise record when leaving the screen
    DisposableEffect(accurateReps, exerciseTimer) {
        onDispose {
            if (accurateReps > 0) {
                AppPreferences.addExerciseRecord(
                    context,
                    AppPreferences.ExerciseRecord(
                        name = "Lunges",
                        timestamp = System.currentTimeMillis(),
                        reps = accurateReps,
                        durationSeconds = exerciseTimer
                    )
                )
            }
            ttsInstance?.stop()
            ttsInstance?.shutdown()
        }
    }
}
