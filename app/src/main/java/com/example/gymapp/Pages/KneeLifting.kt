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
import com.example.gymapp.Pages.calculateAngle
import com.example.gymapp.AppPreferences

@androidx.camera.core.ExperimentalGetImage
@Composable
fun KneeLifting(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var showSettingsMessage by remember { mutableStateOf(false) }
    var debugMode by remember { mutableStateOf(false) }
    var lastSpokenFeedback by remember { mutableStateOf("") }
    var lastSpeechTime by remember { mutableStateOf(0L) }
    val minSpeechIntervalMs = 1200L // Minimum time between spoken feedback (1.2 seconds)
    val primaryColor = Color(0xFFB0C929)

    // Track latest for save-on-exit
    var latestRepsForSave by remember { mutableStateOf(0) }
    var latestDurationForSave by remember { mutableStateOf(0) }

    // Permission launcher for camera
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            cameraPermissionGranted = granted
            if (!granted) {
                showSettingsMessage = true
            }
        }
    )

    LaunchedEffect(Unit) {
        val permission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permission == PackageManager.PERMISSION_GRANTED) {
            cameraPermissionGranted = true
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val scrollState = rememberScrollState()

    val ttsInstance: TextToSpeech? by remember {
        mutableStateOf(null)
    }

    val tts = remember {
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInstance?.language = Locale.US
            }
        }.also { ttsInstance }
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
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Knee Lifting",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Debug Mode: ", style = MaterialTheme.typography.bodySmall, color = Color.Black)
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
            var inaccurateReps by remember { mutableStateOf(0) } // Counted but not displayed
            var isExercising by remember { mutableStateOf(false) }
            var calibrationComplete by remember { mutableStateOf(false) }
            var exerciseTimer by remember { mutableStateOf(0) }
            var lastLiftedKnee by remember { mutableStateOf("") }

            // Mirror for save-on-exit
            LaunchedEffect(accurateReps) { latestRepsForSave = accurateReps }
            LaunchedEffect(exerciseTimer) { latestDurationForSave = exerciseTimer }

            // Calibration data
            var neutralKneeHeight by remember { mutableStateOf(0f) }
            var neutralHipHeight by remember { mutableStateOf(0f) }
            var calibrationFrames by remember { mutableStateOf(0) }

            // Threshold values - Made more lenient
            val kneeLiftThreshold = 70f // Reduced from 80f for easier detection
            val hipMovementThreshold = 60f // Increased from 50f to allow more natural movement
            val kneeAngleThreshold = 145.0 // Keep 145 degrees but with more lenient conditions
            val neutralPositionThreshold = 40f // Increased from 30f for easier return to neutral

            // Timer for exercise duration
            LaunchedEffect(Unit) {
                while (true) {
                    delay(1000)
                    if (calibrationComplete) exerciseTimer++
                }
            }

            Text(
                "Camera active. Lift your knees toward your chest one at a time.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black
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
                                                val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
                                                val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
                                                val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                                                val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
                                                val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
                                                val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)

                                                if (leftKnee != null && rightKnee != null &&
                                                    leftHip != null && rightHip != null &&
                                                    leftAnkle != null && rightAnkle != null) {

                                                    val avgKneeY = (leftKnee.position.y + rightKnee.position.y) / 2
                                                    val avgHipY = (leftHip.position.y + rightHip.position.y) / 2

                                                    val leftKneeAngle = calculateAngle(
                                                        leftHip.position.x, leftHip.position.y,
                                                        leftKnee.position.x, leftKnee.position.y,
                                                        leftAnkle.position.x, leftAnkle.position.y
                                                    )
                                                    val rightKneeAngle = calculateAngle(
                                                        rightHip.position.x, rightHip.position.y,
                                                        rightKnee.position.x, rightKnee.position.y,
                                                        rightAnkle.position.x, rightAnkle.position.y
                                                    )

                                                    val leftKneeLift = neutralKneeHeight - leftKnee.position.y
                                                    val rightKneeLift = neutralKneeHeight - rightKnee.position.y

                                                    val leftHipMovement = abs(leftHip.position.y - neutralHipHeight)
                                                    val rightHipMovement = abs(rightHip.position.y - neutralHipHeight)

                                                    if (!calibrationComplete) {
                                                        calibrationFrames++
                                                        if (calibrationFrames >= 30) {
                                                            neutralKneeHeight = avgKneeY
                                                            neutralHipHeight = avgHipY
                                                            calibrationComplete = true
                                                            feedback = "Calibration complete! Start lifting your knees"
                                                        } else {
                                                            val progress = (calibrationFrames * 100 / 30)
                                                            feedback = "Calibrating... $progress% complete. Stand normally."
                                                        }
                                                    } else {
                                                        val isLeftKneeLifted = leftKneeLift > kneeLiftThreshold
                                                        val isRightKneeLifted = rightKneeLift > kneeLiftThreshold

                                                        // Check for angle greater than 145 degrees
                                                        val isLeftKneeBent = leftKneeAngle > kneeAngleThreshold
                                                        val isRightKneeBent = rightKneeAngle > kneeAngleThreshold

                                                        // More lenient hip stability check
                                                        val isHipsStable = leftHipMovement < hipMovementThreshold ||
                                                                rightHipMovement < hipMovementThreshold

                                                        // More lenient neutral position detection
                                                        val isNeutralPosition = abs(leftKnee.position.y - neutralKneeHeight) < neutralPositionThreshold &&
                                                                abs(rightKnee.position.y - neutralKneeHeight) < neutralPositionThreshold

                                                        if (debugMode) {
                                                            debugInfo = """
                                                            L-Knee Lift: ${String.format(Locale.getDefault(), "%.1f", leftKneeLift)}
                                                            R-Knee Lift: ${String.format(Locale.getDefault(), "%.1f", rightKneeLift)}
                                                            L-Knee Angle: ${String.format(Locale.getDefault(), "%.1f", leftKneeAngle)}°
                                                            R-Knee Angle: ${String.format(Locale.getDefault(), "%.1f", rightKneeAngle)}°
                                                            L-Hip Movement: ${String.format(Locale.getDefault(), "%.1f", leftHipMovement)}
                                                            R-Hip Movement: ${String.format(Locale.getDefault(), "%.1f", rightHipMovement)}
                                                            Hips Stable: $isHipsStable
                                                            Accurate Angle: ${String.format(Locale.getDefault(), "%.1f", kneeAngleThreshold)}°
                                                            """.trimIndent()
                                                        }

                                                        if ((isLeftKneeLifted || isRightKneeLifted) && !isExercising) {
                                                            val liftedKnee = if (isLeftKneeLifted && isRightKneeLifted) {
                                                                if (leftKneeLift > rightKneeLift) "left" else "right"
                                                            } else if (isLeftKneeLifted) {
                                                                "left"
                                                            } else {
                                                                "right"
                                                            }

                                                            // More lenient form check - only require knee angle > 145°
                                                            val isGoodForm = (liftedKnee == "left" && isLeftKneeBent) ||
                                                                    (liftedKnee == "right" && isRightKneeBent)

                                                            if (liftedKnee != lastLiftedKnee) {
                                                                isExercising = true
                                                                lastLiftedKnee = liftedKnee

                                                                if (isGoodForm) {
                                                                    accurateReps++
                                                                    feedback = "Perfect! Rep counted."
                                                                } else {
                                                                    inaccurateReps++
                                                                    feedback = "Lift your knee higher (angle > ${kneeAngleThreshold.toInt()}°)"
                                                                }
                                                            } else {
                                                                feedback = "Alternate knees for next rep!"
                                                            }
                                                        } else if (isNeutralPosition && isExercising) {
                                                            isExercising = false
                                                        } else if (!isExercising && isNeutralPosition) {
                                                            feedback = "Ready for next rep"
                                                        } else if (isExercising && !isNeutralPosition) {
                                                            feedback = "Lower your knee to complete the rep"
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
                                    Log.e("KneeLifting", "Image analysis error", e)
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
                                Log.e("KneeLifting", "Camera binding failed", exc)
                            }
                        } catch (e: Exception) {
                            Log.e("KneeLifting", "Camera initialization failed", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        feedback.contains("Perfect") -> primaryColor.copy(alpha = 0.2f)
                        feedback.contains("Good form") -> primaryColor.copy(alpha = 0.2f)
                        feedback.contains("Inaccurate") -> MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        feedback.contains("calibrat", ignoreCase = true) -> Color.LightGray
                        else -> Color.White
                    }
                )
            ) {
                val isRepFeedback = feedback.contains("Perfect") || feedback.contains("Good form")
                val isGuidance = feedback.contains("Keep your hips") || feedback.contains("Lift your knee higher") || feedback.contains("Alternate knees") || feedback.contains("Ready for next rep") || feedback.contains("Lower your knee")
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

            LaunchedEffect(feedback) {
                val now = System.currentTimeMillis()
                val importantFeedback = listOf("Perfect", "Good form", "Inaccurate", "calibration complete")
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

            // Only show accurate reps count (keep counting total and inaccurate in backend)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Accurate Reps", style = MaterialTheme.typography.labelMedium, color = primaryColor)
                Text("$accurateReps",
                    style = MaterialTheme.typography.headlineSmall,
                    color = primaryColor)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Time: ${exerciseTimer / 60}:${"%02d".format(exerciseTimer % 60)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )

            if (calibrationComplete) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        calibrationComplete = false
                        calibrationFrames = 0
                        accurateReps = 0
                        inaccurateReps = 0
                        exerciseTimer = 0
                        lastLiftedKnee = ""
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
            Text(
                "Camera permission is required to detect your exercise form.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) },
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
                        name = "Knee Lifting",
                        timestamp = System.currentTimeMillis(),
                        reps = latestRepsForSave,
                        durationSeconds = latestDurationForSave
                    )
                )
            }
        }
    }
}
