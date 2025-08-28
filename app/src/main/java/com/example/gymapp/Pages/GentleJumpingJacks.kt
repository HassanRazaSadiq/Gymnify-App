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
import kotlin.math.*

@Composable
fun GentleJumpingJacks(modifier: Modifier = Modifier) {
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

    // Track latest reps for save on exit
    var latestRepsForSave by remember { mutableStateOf(0) }
    var latestDurationForSave by remember { mutableStateOf(0) }

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
                "Gentle Jumping Jacks",
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
                        "How to Perform Gentle Jumping Jacks:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "1. Stand straight with feet together and arms at your sides\n" +
                                "2. Gently jump or step your feet out wider than your shoulders\n" +
                                "3. Simultaneously raise your arms out to the sides and overhead\n" +
                                "4. Return to the starting position with feet together and arms down\n" +
                                "5. Maintain a gentle, controlled motion throughout",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tip: Keep your movements smooth and avoid locking your joints.",
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
            var accurateReps by remember { mutableStateOf(0) }
            var isExercising by remember { mutableStateOf(false) }
            var calibrationComplete by remember { mutableStateOf(false) }
            var exerciseTimer by remember { mutableStateOf(0) }
            var neutralArmDistance by remember { mutableStateOf(0f) }
            var neutralLegDistance by remember { mutableStateOf(0f) }
            var calibrationFrames by remember { mutableStateOf(0) }

            // Mirror reps for saving on exit
            LaunchedEffect(accurateReps) { latestRepsForSave = accurateReps }
            LaunchedEffect(exerciseTimer) { latestDurationForSave = exerciseTimer }

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
                val importantFeedback = listOf("Perfect form", "Rep counted", "Good expansion", "calibration complete", "Try straighter arms")
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
                                        val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
                                        val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
                                        val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
                                        val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
                                        val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                                        val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
                                        val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                                        val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
                                        val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
                                        val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)

                                        if (leftWrist != null && rightWrist != null &&
                                            leftAnkle != null && rightAnkle != null &&
                                            leftShoulder != null && rightShoulder != null &&
                                            leftHip != null && rightHip != null) {

                                            // Calculate distances
                                            val wristDistance = calculateDistance(
                                                leftWrist.position.x, leftWrist.position.y,
                                                rightWrist.position.x, rightWrist.position.y
                                            )

                                            val ankleDistance = calculateDistance(
                                                leftAnkle.position.x, leftAnkle.position.y,
                                                rightAnkle.position.x, rightAnkle.position.y
                                            )

                                            val shoulderDistance = calculateDistance(
                                                leftShoulder.position.x, leftShoulder.position.y,
                                                rightShoulder.position.x, rightShoulder.position.y
                                            )

                                            val hipDistance = calculateDistance(
                                                leftHip.position.x, leftHip.position.y,
                                                rightHip.position.x, rightHip.position.y
                                            )

                                            // Calculate arm and leg angles for form validation
                                            val leftArmAngle = if (leftElbow != null) {
                                                calculateAngle(
                                                    leftShoulder.position.x, leftShoulder.position.y,
                                                    leftElbow.position.x, leftElbow.position.y,
                                                    leftWrist.position.x, leftWrist.position.y
                                                )
                                            } else 0.0

                                            val rightArmAngle = if (rightElbow != null) {
                                                calculateAngle(
                                                    rightShoulder.position.x, rightShoulder.position.y,
                                                    rightElbow.position.x, rightElbow.position.y,
                                                    rightWrist.position.x, rightWrist.position.y
                                                )
                                            } else 0.0

                                            // Calibration phase
                                            if (!calibrationComplete) {
                                                calibrationFrames++
                                                if (calibrationFrames >= 30) {
                                                    neutralArmDistance = shoulderDistance
                                                    neutralLegDistance = hipDistance
                                                    calibrationComplete = true
                                                    feedback = "Calibration complete! Start gentle jumping jacks"
                                                } else {
                                                    val progress = (calibrationFrames * 100 / 30)
                                                    feedback = "Calibrating... $progress% complete. Stand still."
                                                }
                                            }
                                            // Exercise phase
                                            else {
                                                // Calculate expansion ratios
                                                val armExpansion = wristDistance / neutralArmDistance
                                                val legExpansion = ankleDistance / neutralLegDistance

                                                // Check if both arms and legs are expanded
                                                val isArmsExpanded = armExpansion > 1.7f
                                                val isLegsExpanded = legExpansion > 1.4f
                                                val isFullyExpanded = isArmsExpanded && isLegsExpanded

                                                // Check if back to neutral
                                                val isBackToNeutral = armExpansion < 1.3f && legExpansion < 1.2f

                                                // Check form quality
                                                val isGoodForm = leftArmAngle > 150 && rightArmAngle > 150 && // Arms mostly straight
                                                        abs(leftArmAngle - rightArmAngle) < 30 // Symmetrical movement

                                                // Debug information
                                                if (debugMode) {
                                                    debugInfo = """
                                                    Arm Expansion: ${String.format(Locale.US, "%.2f", armExpansion)}x
                                                    Leg Expansion: ${String.format(Locale.US, "%.2f", legExpansion)}x
                                                    Arm Angles: L=${String.format(Locale.US, "%.1f", leftArmAngle)}° R=${String.format(Locale.US, "%.1f", rightArmAngle)}°
                                                    Form: ${if (isGoodForm) "Good" else "Needs improvement"}
                                                    """.trimIndent()
                                                }

                                                // State machine for exercise
                                                if (isFullyExpanded && !isExercising) {
                                                    isExercising = true
                                                    feedback = "Good expansion! Now return to center"
                                                }
                                                else if (isBackToNeutral && isExercising) {
                                                    isExercising = false
                                                    accurateReps++
                                                    if (isGoodForm) {
                                                        feedback = "Perfect form! Rep: $accurateReps"
                                                    } else {
                                                        feedback = "Rep counted, but try straighter arms. Rep: $accurateReps"
                                                    }
                                                }
                                                else if (!isExercising && isBackToNeutral) {
                                                    feedback = "Ready for next rep"
                                                }
                                                else if (isExercising && !isFullyExpanded) {
                                                    feedback = "Expand more - arms up and out!"
                                                }
                                                else if (!isExercising) {
                                                    feedback = "Return to neutral position to start"
                                                }
                                            }

                                        } else {
                                            feedback = "Please make sure your full body is visible"
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
                        feedback.contains("Good expansion") -> primaryColor.copy(alpha = 0.2f)
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

            Spacer(modifier = Modifier.height(16.dp))

            // Display only accurate reps
            Text(
                "Accurate Reps: $accurateReps",
                style = MaterialTheme.typography.headlineSmall,
                color = blueTextColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // Timer
            Text(
                "Time: ${exerciseTimer / 60}:${"%02d".format(exerciseTimer % 60)}",
                style = MaterialTheme.typography.bodyMedium,
                color = blueTextColor
            )

            // Reset calibration button
            if (calibrationComplete) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        calibrationComplete = false
                        calibrationFrames = 0
                        accurateReps = 0
                        exerciseTimer = 0
                        feedback = "Please stand in frame for calibration"
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor, contentColor = Color.Black)
                ) {
                    Text("Reset Exercise")
                }
            }

        } else if (!cameraPermissionGranted) {
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
    // Remove shadowing accurateReps; save using latestRepsForSave
    // Disposable effect to record exercise data when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            if (latestRepsForSave > 0) {
                AppPreferences.addExerciseRecord(
                    context,
                    AppPreferences.ExerciseRecord(
                        name = "Gentle Jumping Jacks",
                        timestamp = System.currentTimeMillis(),
                        reps = latestRepsForSave,
                        durationSeconds = latestDurationForSave
                    )
                )
            }
        }
    }
}

private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y1 - y2
    return sqrt(dx * dx + dy * dy)
}
