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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.gymapp.AppPreferences
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import kotlinx.coroutines.delay
import java.util.*

@Composable
fun OverheadPress(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var showSettingsMessage by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(true) }
    var debugMode by remember { mutableStateOf(false) }

    val primaryColor = Color(0xFFB0C929)
    val blueTextColor = Color(0xFF2196F3)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
        if (!granted && ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            showSettingsMessage = true
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraPermissionGranted = true
        }
    }

    var ttsInstance: TextToSpeech? by remember { mutableStateOf(null) }
    var lastSpokenFeedback by remember { mutableStateOf("") }
    var lastSpeechTime by remember { mutableStateOf(0L) }
    val minSpeechIntervalMs = 1200L

    LaunchedEffect(Unit) {
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            ttsInstance?.stop()
            ttsInstance?.shutdown()
        }
    }

    val scrollState = rememberScrollState()

    // Track latest reps for saving on exit
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
                "Overhead Press",
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
            // Removed GIF preview on top
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "How to Perform Overhead Press:",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "1. Stand straight with feet shoulder-width apart\n" +
                                "2. Hold light weights or keep hands at shoulder level\n" +
                                "3. Press your arms straight overhead until fully extended\n" +
                                "4. Lower arms back to shoulder level\n" +
                                "5. Repeat for desired reps",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tip: Keep your core tight and avoid arching your back.",
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
            var calibrationComplete by remember { mutableStateOf(false) }
            var exerciseTimer by remember { mutableStateOf(0) }
            var neutralShoulderY by remember { mutableStateOf(0f) }
            var calibrationFrames by remember { mutableStateOf(0) }
            var exerciseState by remember { mutableStateOf("down") } // "down" or "up"

            // Mirror for saving
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
                val importantFeedback = listOf("Perfect form", "Rep counted", "Good press", "calibration complete", "Straighten your arms")
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

                        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx), object : ImageAnalysis.Analyzer {
                            @ExperimentalGetImage
                            override fun analyze(imageProxy: ImageProxy) {
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val inputImage = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )

                                    val options = PoseDetectorOptions.Builder()
                                        .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                                        .build()
                                    val poseDetector = PoseDetection.getClient(options)

                                    poseDetector.process(inputImage)
                                        .addOnSuccessListener { pose ->
                                            // Get key landmarks
                                            val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
                                            val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
                                            val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                                            val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
                                            val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
                                            val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)

                                            if (leftWrist != null && rightWrist != null &&
                                                leftShoulder != null && rightShoulder != null &&
                                                leftElbow != null && rightElbow != null) {

                                                // Calculate average shoulder Y position
                                                val avgShoulderY = (leftShoulder.position.y + rightShoulder.position.y) / 2

                                                // Calibration phase
                                                if (!calibrationComplete) {
                                                    calibrationFrames++
                                                    if (calibrationFrames >= 30) {
                                                        neutralShoulderY = avgShoulderY
                                                        calibrationComplete = true
                                                        feedback = "Calibration complete! Start overhead presses"
                                                    } else {
                                                        val progress = (calibrationFrames * 100 / 30)
                                                        feedback = "Calibrating... $progress% complete. Stand straight with arms at shoulder level."
                                                    }
                                                }
                                                // Exercise phase
                                                else {
                                                    // Check if arms are raised (wrists above shoulders)
                                                    val leftArmRaised = leftWrist.position.y < neutralShoulderY - 80
                                                    val rightArmRaised = rightWrist.position.y < neutralShoulderY - 80
                                                    val bothArmsRaised = leftArmRaised && rightArmRaised

                                                    // Check if arms are straight (elbow angle > 160 degrees)
                                                    val leftArmAngle = calculateAngle(
                                                        leftShoulder.position.x, leftShoulder.position.y,
                                                        leftElbow.position.x, leftElbow.position.y,
                                                        leftWrist.position.x, leftWrist.position.y
                                                    )

                                                    val rightArmAngle = calculateAngle(
                                                        rightShoulder.position.x, rightShoulder.position.y,
                                                        rightElbow.position.x, rightElbow.position.y,
                                                        rightWrist.position.x, rightWrist.position.y
                                                    )

                                                    val leftArmStraight = leftArmAngle > 160
                                                    val rightArmStraight = rightArmAngle > 160
                                                    val bothArmsStraight = leftArmStraight && rightArmStraight

                                                    // Check if arms are back to starting position
                                                    val leftArmDown = leftWrist.position.y > neutralShoulderY - 40 && leftWrist.position.y < neutralShoulderY + 40
                                                    val rightArmDown = rightWrist.position.y > neutralShoulderY - 40 && rightWrist.position.y < neutralShoulderY + 40
                                                    val bothArmsDown = leftArmDown && rightArmDown

                                                    // Debug information
                                                    if (debugMode) {
                                                        debugInfo = """
Left Arm Angle: ${String.format(Locale.US, "%.1f", leftArmAngle)}°\nRight Arm Angle: ${String.format(Locale.US, "%.1f", rightArmAngle)}°\nLeft Wrist Y: ${String.format(Locale.US, "%.1f", leftWrist.position.y)}\nRight Wrist Y: ${String.format(Locale.US, "%.1f", rightWrist.position.y)}\nShoulder Y: ${String.format(Locale.US, "%.1f", neutralShoulderY)}\nState: $exerciseState
""".trimIndent()
                                                    }

                                                    // State machine for exercise
                                                    when (exerciseState) {
                                                        "down" -> {
                                                            if (bothArmsRaised && bothArmsStraight) {
                                                                exerciseState = "up"
                                                                feedback = "Good press! Now lower the weights"
                                                            } else if (bothArmsRaised && !bothArmsStraight) {
                                                                feedback = "Straighten your arms more at the top"
                                                            } else {
                                                                feedback = "Press the weights overhead"
                                                            }
                                                        }
                                                        "up" -> {
                                                            if (bothArmsDown) {
                                                                exerciseState = "down"
                                                                accurateReps++
                                                                feedback = "Perfect rep! Count: $accurateReps"
                                                            } else {
                                                                feedback = "Lower the weights to shoulder level"
                                                            }
                                                        }
                                                    }
                                                }

                                            } else {
                                                feedback = "Please make sure your upper body is visible"
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
                        })
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
                        feedback.contains("Good press") -> primaryColor.copy(alpha = 0.2f)
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
                        exerciseState = "down"
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
    // Remove shadowing accurateReps; use latestRepsForSave to persist
    DisposableEffect(Unit) {
        onDispose {
            if (latestRepsForSave > 0) {
                AppPreferences.addExerciseRecord(
                    context,
                    AppPreferences.ExerciseRecord(
                        name = "Overhead Press",
                        timestamp = System.currentTimeMillis(),
                        reps = latestRepsForSave,
                        durationSeconds = latestDurationForSave
                    )
                )
            }
        }
    }
}
