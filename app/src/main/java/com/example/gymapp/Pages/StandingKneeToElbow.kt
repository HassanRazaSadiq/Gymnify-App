package com.example.gymapp.Pages

import android.Manifest
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.camera.view.PreviewView
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.core.Preview
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.google.mlkit.vision.pose.PoseLandmark
import kotlin.math.*
import kotlinx.coroutines.delay
import java.util.Locale
import com.example.gymapp.AppPreferences

@androidx.camera.core.ExperimentalGetImage
@Suppress("UNUSED_PARAMETER")
@Composable
fun StandingKneeToElbow(modifier: Modifier = Modifier, navController: NavController) {
    val context = LocalContext.current
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var showSettingsMessage by remember { mutableStateOf(false) }
    var debugMode by remember { mutableStateOf(false) }
    var lastSpokenFeedback by remember { mutableStateOf("") }
    var lastSpeechTime by remember { mutableStateOf(0L) }
    val minSpeechIntervalMs = 1200L // Minimum time between spoken feedback (1.2 seconds)

    // Track latest values for saving on exit
    var latestRepsForSave by remember { mutableStateOf(0) }
    var latestDurationForSave by remember { mutableStateOf(0) }

    // Primary color from your request
    val primaryColor = Color(0xFFB0C929) // Restored to lime/green
    val blueTextColor = Color(0xFF2196F3) // Blue for text

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
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

    // Scroll state for the column
    val scrollState = rememberScrollState()

    // TTS state
    var ttsInstance: TextToSpeech? = null
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
                "Standing Knee to Elbow",
                style = MaterialTheme.typography.headlineMedium,
                color = blueTextColor, // Blue text
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Debug mode toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Debug Mode: ", style = MaterialTheme.typography.bodySmall, color = blueTextColor) // Blue text
            Switch(
                checked = debugMode,
                onCheckedChange = { debugMode = it },
                colors = SwitchDefaults.colors(checkedThumbColor = primaryColor, checkedTrackColor = primaryColor.copy(alpha = 0.5f))
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (cameraPermissionGranted) {
            var feedback by remember { mutableStateOf("Please stand in frame for calibration") }
            var debugInfo by remember { mutableStateOf("") }
            var accurateReps by remember { mutableStateOf(0) }
            var isExercising by remember { mutableStateOf(false) }
            var calibrationComplete by remember { mutableStateOf(false) }
            var exerciseTimer by remember { mutableStateOf(0) }
            var lastSide by remember { mutableStateOf("") } // Track which side was last
            var lastCompletedSide by remember { mutableStateOf("") } // Track last completed side
            val contactDistanceThreshold = 100 // You can tune this value
            val kneeLiftThreshold = 50 // You can tune this value
            val goodFormKneeLiftThreshold = 80 // You can tune this value
            val goodFormTwistThreshold = 20 // You can tune this value

            // Mirror to latest values for saving on exit
            LaunchedEffect(accurateReps) { latestRepsForSave = accurateReps }
            LaunchedEffect(exerciseTimer) { latestDurationForSave = exerciseTimer }

            // Latch whether good form occurred during the current rep
            var wasGoodFormDuringContact by remember { mutableStateOf(false) }

            // Calibration data
            var neutralKneeHeight by remember { mutableStateOf(0f) }
            var calibrationFrames by remember { mutableStateOf(0) }

            // Timer for exercise duration
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1000)
                    if (calibrationComplete) exerciseTimer++
                }
            }

            Text(
                "Camera active. Follow the instructions for accurate counting.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = blueTextColor // Blue text
            )

            Spacer(modifier = Modifier.height(16.dp))

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        try {
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
                                try {
                                    val mediaImage = imageProxy.image
                                    if (mediaImage != null) {
                                        val inputImage = com.google.mlkit.vision.common.InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                        val options = PoseDetectorOptions.Builder()
                                            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
                                            .build()
                                        val poseDetector = PoseDetection.getClient(options)

                                        poseDetector.process(inputImage)
                                            .addOnSuccessListener { pose ->
                                                // Get key landmarks for knee-to-elbow detection
                                                val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
                                                val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
                                                val leftElbow = pose.getPoseLandmark(PoseLandmark.LEFT_ELBOW)
                                                val rightElbow = pose.getPoseLandmark(PoseLandmark.RIGHT_ELBOW)
                                                val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                                                val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
                                                val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                                                val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)

                                                if (leftKnee != null && rightKnee != null &&
                                                    leftElbow != null && rightElbow != null &&
                                                    leftHip != null && rightHip != null &&
                                                    leftShoulder != null && rightShoulder != null) {

                                                    // Calculate average positions
                                                    val avgKneeY = (leftKnee.position.y + rightKnee.position.y) / 2

                                                    // Calculate distances for knee-to-elbow contact
                                                    val leftKneeToRightElbow = calculateDistance(
                                                        leftKnee.position.x, leftKnee.position.y,
                                                        rightElbow.position.x, rightElbow.position.y
                                                    )

                                                    val rightKneeToLeftElbow = calculateDistance(
                                                        rightKnee.position.x, rightKnee.position.y,
                                                        leftElbow.position.x, leftElbow.position.y
                                                    )

                                                    // Calculate torso twist angles
                                                    val leftTorsoTwist = calculateTorsoTwist(
                                                        leftShoulder.position.x, leftShoulder.position.y,
                                                        leftHip.position.x, leftHip.position.y,
                                                        rightHip.position.x, rightHip.position.y
                                                    )

                                                    val rightTorsoTwist = calculateTorsoTwist(
                                                        rightShoulder.position.x, rightShoulder.position.y,
                                                        rightHip.position.x, rightHip.position.y,
                                                        leftHip.position.x, leftHip.position.y
                                                    )

                                                    // Calculate knee lift height
                                                    val leftKneeLift = neutralKneeHeight - leftKnee.position.y
                                                    val rightKneeLift = neutralKneeHeight - rightKnee.position.y

                                                    // Calibration phase
                                                    if (!calibrationComplete) {
                                                        calibrationFrames++
                                                        // Collect multiple frames for stable calibration
                                                        if (calibrationFrames >= 30) {
                                                            neutralKneeHeight = avgKneeY
                                                            calibrationComplete = true
                                                            feedback = "Calibration complete! Start exercise"
                                                        } else {
                                                            val progress = (calibrationFrames * 100 / 30)
                                                            feedback = "Calibrating... $progress% complete. Stand normally."
                                                        }
                                                    }
                                                    // Exercise phase
                                                    else {
                                                        // Check if knee and elbow are close enough (contact)
                                                        val isLeftContact = leftKneeToRightElbow < contactDistanceThreshold && leftKneeLift > kneeLiftThreshold
                                                        val isRightContact = rightKneeToLeftElbow < contactDistanceThreshold && rightKneeLift > kneeLiftThreshold
                                                        val isContact = (isLeftContact xor isRightContact) // Only one contact allowed per frame

                                                        // Check for proper form (torso should twist, knee should lift high)
                                                        val isGoodForm = ((isLeftContact && leftTorsoTwist > goodFormTwistThreshold) ||
                                                                (isRightContact && rightTorsoTwist > goodFormTwistThreshold)) &&
                                                                ((isLeftContact && leftKneeLift > goodFormKneeLiftThreshold) ||
                                                                        (isRightContact && rightKneeLift > goodFormKneeLiftThreshold))

                                                        // Check if we're returning to neutral
                                                        val isNeutralPosition = leftKneeToRightElbow > 150 &&
                                                                rightKneeToLeftElbow > 150 &&
                                                                abs(leftKnee.position.y - neutralKneeHeight) < 30 &&
                                                                abs(rightKnee.position.y - neutralKneeHeight) < 30

                                                        // Debug information
                                                        if (debugMode) {
                                                            debugInfo = """
                                                            L-Knee to R-Elbow: ${String.format(Locale.US, "%.1f", leftKneeToRightElbow)}
                                                            R-Knee to L-Elbow: ${String.format(Locale.US, "%.1f", rightKneeToLeftElbow)}
                                                            L-Knee Lift: ${String.format(Locale.US, "%.1f", leftKneeLift)}
                                                            R-Knee Lift: ${String.format(Locale.US, "%.1f", rightKneeLift)}
                                                            Torso Twist: L=${String.format(Locale.US, "%.1f", leftTorsoTwist)}° R=${String.format(Locale.US, "%.1f", rightTorsoTwist)}°
                                                            Form: ${if (isGoodForm) "Good" else "Needs improvement"}
                                                            """.trimIndent()
                                                        }

                                                        // Simplified and clearer state machine for exercise
                                                        if (isContact) {
                                                            if (!isExercising) {
                                                                // Only allow rep if alternating sides
                                                                val currentSide = if (isLeftContact) "left knee to right elbow" else if (isRightContact) "right knee to left elbow" else ""
                                                                if (currentSide.isNotEmpty() && currentSide != lastCompletedSide) {
                                                                    isExercising = true
                                                                    lastSide = currentSide
                                                                    wasGoodFormDuringContact = isGoodForm
                                                                    feedback = "Good contact! $currentSide"
                                                                } else {
                                                                    feedback = "Alternate sides for next rep!"
                                                                }
                                                            } else {
                                                                // If still in contact, keep latching good form improvements
                                                                wasGoodFormDuringContact = wasGoodFormDuringContact || isGoodForm
                                                            }
                                                        } else { // No contact
                                                            if (isExercising) {
                                                                if (isNeutralPosition) {
                                                                    isExercising = false
                                                                    accurateReps++
                                                                    if (wasGoodFormDuringContact) {
                                                                        feedback = "Perfect rep! Count: $accurateReps"
                                                                    } else {
                                                                        feedback = "Rep counted, try better form. Count: $accurateReps"
                                                                    }
                                                                    lastCompletedSide = lastSide // Mark this side as completed
                                                                    wasGoodFormDuringContact = false
                                                                } else {
                                                                    feedback = "Return to neutral position"
                                                                }
                                                            } else { // Not exercising
                                                                if (isNeutralPosition) {
                                                                    feedback = "Ready for next rep"
                                                                } else {
                                                                    feedback = "Bring knee and opposite elbow together"
                                                                }
                                                            }
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
                                } catch (e: Exception) {
                                    Log.e("KneeToElbow", "Image analysis error", e)
                                    imageProxy.close()
                                }
                            }

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    ctx as androidx.lifecycle.LifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (exc: Exception) {
                                Log.e("KneeToElbow", "Camera binding failed", exc)
                            }
                        } catch (e: Exception) {
                            Log.e("KneeToElbow", "Camera initialization failed", e)
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
                    colors = CardDefaults.cardColors(containerColor = Color.DarkGray)
                ) {
                    Text(
                        debugInfo,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
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
                        feedback.contains("Good contact") -> primaryColor.copy(alpha = 0.2f)
                        feedback.contains("Try to keep good form") -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        feedback.contains("calibrat", ignoreCase = true) -> Color.LightGray
                        else -> Color.White
                    }
                )
            ) {
                val isRepFeedback = feedback.contains("Perfect") || feedback.contains("Good contact")
                val isGuidance = feedback.contains("Bring knee") || feedback.contains("Return to neutral position") || feedback.contains("Alternate sides") || feedback.contains("Ready for next rep") || feedback.contains("Try to keep good form")
                Text(
                    feedback,
                    style = when {
                        isRepFeedback -> MaterialTheme.typography.headlineLarge
                        isGuidance -> MaterialTheme.typography.bodyMedium
                        else -> MaterialTheme.typography.bodySmall
                    },
                    color = Color.Black,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Speak feedback if changed
            LaunchedEffect(feedback) {
                val now = System.currentTimeMillis()
                val importantFeedback = listOf("Perfect rep", "Perfect form", "Rep counted", "Good contact", "calibration complete", "Try to keep good form")
                val shouldSpeak = importantFeedback.any { feedback.contains(it, ignoreCase = true) }
                if (shouldSpeak && feedback != lastSpokenFeedback && feedback.isNotBlank()) {
                    if (!tts.isSpeaking && now - lastSpeechTime > minSpeechIntervalMs) {
                        tts.speak(feedback, TextToSpeech.QUEUE_FLUSH, null, null)
                        lastSpokenFeedback = feedback
                        lastSpeechTime = now
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Display only accurate reps
            Text(
                "Accurate Reps: $accurateReps",
                style = MaterialTheme.typography.headlineSmall,
                color = blueTextColor,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            // Feedback text (theme-aware color)
            Text(
                feedback,
                style = MaterialTheme.typography.bodyLarge,
                color = blueTextColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            )
            Text(
                "Time: ${exerciseTimer / 60}:${"%02d".format(exerciseTimer % 60)}",
                style = MaterialTheme.typography.bodyMedium,
                color = blueTextColor // Blue text
            )

            // Reset button with rounded edges
            if (calibrationComplete) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        calibrationComplete = false
                        calibrationFrames = 0
                        accurateReps = 0
                        exerciseTimer = 0
                        lastSide = ""
                        lastCompletedSide = ""
                        isExercising = false
                        feedback = "Please stand in frame for calibration"
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        contentColor = Color.Black
                    )
                ) {
                    Text("Reset Exercise", style = MaterialTheme.typography.bodyLarge)
                }
            }

        } else {
            // Camera permission not granted
            Text(
                "Camera permission is required to detect your exercise form.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black
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
                Text("Allow Camera Access")
            }
            if (showSettingsMessage) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Permission denied. Please enable camera access in app settings.",
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    // Save exercise record when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            if (latestRepsForSave > 0) {
                AppPreferences.addExerciseRecord(
                    context,
                    AppPreferences.ExerciseRecord(
                        name = "Standing Knee to Elbow",
                        timestamp = System.currentTimeMillis(),
                        reps = latestRepsForSave,
                        durationSeconds = latestDurationForSave
                    )
                )
            }
        }
    }
}

// Helper to compute Euclidean distance between two points
private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return sqrt(dx * dx + dy * dy)
}


// Helper function to calculate torso twist
private fun calculateTorsoTwist(
    shoulderX: Float, shoulderY: Float,
    sameHipX: Float, sameHipY: Float,
    oppositeHipX: Float, oppositeHipY: Float
): Double {
    // Calculate angle between shoulder, same hip, and opposite hip
    // This measures how much the torso is twisting
    return calculateAngle(shoulderX, shoulderY, sameHipX, sameHipY, oppositeHipX, oppositeHipY)
}