@file:OptIn(androidx.camera.core.ExperimentalGetImage::class)

package com.example.gymapp.Pages

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.gymapp.Pages.calculateAngle
import com.example.gymapp.AppPreferences
import java.util.Locale

@Composable
fun StandingToeTouches(modifier: Modifier = Modifier, navController: NavController) {
    val context = LocalContext.current
    var cameraPermissionGranted by remember { mutableStateOf(false) }
    var showSettingsMessage by remember { mutableStateOf(false) }
    var showInstructions by remember { mutableStateOf(true) }
    var debugMode by remember { mutableStateOf(false) }

    // Track latest values for saving on exit
    var latestRepsForSave by remember { mutableStateOf(0) }
    var latestDurationForSave by remember { mutableStateOf(0) }

    // Primary color from your request
    val primaryColor = Color(0xFFB0C929)

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
                "Standing Toe Touches",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f),
                color = Color.Black
            )
            IconButton(onClick = { showInstructions = !showInstructions }) {
                Icon(Icons.Default.Info, contentDescription = "Instructions", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Instructions Section
        if (showInstructions) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.LightGray)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "How to Perform Standing Toe Touches:",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "1. Stand straight with feet shoulder-width apart\n" +
                                "2. Slowly bend forward at your hips, keeping your back straight\n" +
                                "3. Reach down toward your toes with both hands\n" +
                                "4. Go as far as comfortable without straining\n" +
                                "5. Hold for a moment, then slowly return to standing position\n" +
                                "6. Keep a slight bend in your knees to avoid locking them",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tip: Focus on feeling the stretch in your hamstrings, not how far you can reach.",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = Color.DarkGray
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Debug mode toggle
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
            var totalReps by remember { mutableStateOf(0) }
            var accurateReps by remember { mutableStateOf(0) }
            var inaccurateReps by remember { mutableStateOf(0) }
            var isExercising by remember { mutableStateOf(false) }
            var calibrationComplete by remember { mutableStateOf(false) }
            var exerciseTimer by remember { mutableStateOf(0) }

            // Mirror for save-on-exit
            LaunchedEffect(accurateReps) { latestRepsForSave = accurateReps }
            LaunchedEffect(exerciseTimer) { latestDurationForSave = exerciseTimer }

            // Calibration data
            var neutralHandHeight by remember { mutableStateOf(0f) }
            var neutralHipHeight by remember { mutableStateOf(0f) }
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

                            imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx), object : ImageAnalysis.Analyzer {
                                @androidx.camera.core.ExperimentalGetImage
                                override fun analyze(imageProxy: androidx.camera.core.ImageProxy) {
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
                                                    // Get key landmarks for toe touch detection
                                                    val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
                                                    val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)
                                                    val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)
                                                    val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)
                                                    val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)
                                                    val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)
                                                    val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
                                                    val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
                                                    val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
                                                    val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)

                                                    if (leftWrist != null && rightWrist != null &&
                                                        leftAnkle != null && rightAnkle != null &&
                                                        leftHip != null && rightHip != null &&
                                                        leftShoulder != null && rightShoulder != null) {

                                                        // Calculate average positions
                                                        val avgWristY = (leftWrist.position.y + rightWrist.position.y) / 2
                                                        val avgAnkleY = (leftAnkle.position.y + rightAnkle.position.y) / 2
                                                        val avgHipY = (leftHip.position.y + rightHip.position.y) / 2

                                                        // Calculate distances for hand-to-foot contact
                                                        val leftHandToLeftFoot = calculateDistance(
                                                            leftWrist.position.x, leftWrist.position.y,
                                                            leftAnkle.position.x, leftAnkle.position.y
                                                        )

                                                        val rightHandToRightFoot = calculateDistance(
                                                            rightWrist.position.x, rightWrist.position.y,
                                                            rightAnkle.position.x, rightAnkle.position.y
                                                        )

                                                        val leftHandToRightFoot = calculateDistance(
                                                            leftWrist.position.x, leftWrist.position.y,
                                                            rightAnkle.position.x, rightAnkle.position.y
                                                        )

                                                        val rightHandToLeftFoot = calculateDistance(
                                                            rightWrist.position.x, rightWrist.position.y,
                                                            leftAnkle.position.x, leftAnkle.position.y
                                                        )

                                                        // Calculate back angle to check for proper form
                                                        val backAngle = calculateAngle(
                                                            leftShoulder.position.x, leftShoulder.position.y,
                                                            leftHip.position.x, leftHip.position.y,
                                                            leftAnkle.position.x, leftAnkle.position.y
                                                        )

                                                        // Calculate knee bend to check if legs are straight
                                                        val leftKneeAngle = if (leftKnee != null) {
                                                            calculateAngle(
                                                                leftHip.position.x, leftHip.position.y,
                                                                leftKnee.position.x, leftKnee.position.y,
                                                                leftAnkle.position.x, leftAnkle.position.y
                                                            )
                                                        } else 180.0

                                                        val rightKneeAngle = if (rightKnee != null) {
                                                            calculateAngle(
                                                                rightHip.position.x, rightHip.position.y,
                                                                rightKnee.position.x, rightKnee.position.y,
                                                                rightAnkle.position.x, rightAnkle.position.y
                                                            )
                                                        } else 180.0

                                                        val avgKneeAngle = (leftKneeAngle + rightKneeAngle) / 2

                                                        // Calibration phase
                                                        if (!calibrationComplete) {
                                                            calibrationFrames++
                                                            // Collect multiple frames for stable calibration
                                                            if (calibrationFrames >= 30) {
                                                                neutralHandHeight = avgWristY
                                                                neutralHipHeight = avgHipY
                                                                calibrationComplete = true
                                                                feedback = "Calibration complete! Start toe touches"
                                                            } else {
                                                                val progress = (calibrationFrames * 100 / 30)
                                                                feedback = "Calibrating... $progress% complete. Stand straight."
                                                            }
                                                        }
                                                        // Exercise phase
                                                        else {
                                                            // Check if hand and foot are close enough (contact)
                                                            val minHandToFoot = minOf(
                                                                leftHandToLeftFoot, rightHandToRightFoot,
                                                                leftHandToRightFoot, rightHandToLeftFoot
                                                            )

                                                            val isContact = minHandToFoot < 100 // pixels threshold

                                                            // Check for proper form (back should be straight, knees not locked)
                                                            val isGoodForm = backAngle > 70 && // Back not too rounded
                                                                    avgKneeAngle > 160 // Knees not too bent

                                                            // Check if user is in bent position
                                                            val isBentPosition = avgWristY > avgHipY + 50

                                                            // Check if returned to standing
                                                            val isStanding = abs(avgWristY - neutralHandHeight) < 50 &&
                                                                    abs(avgHipY - neutralHipHeight) < 30

                                                            // Debug information
                                                            if (debugMode) {
                                                                debugInfo = """
                                                                Hand Height: ${String.format("%.1f", avgWristY)}
                                                                Hip Height: ${String.format("%.1f", avgHipY)}
                                                                Back Angle: ${String.format("%.1f", backAngle)}°
                                                                Knee Angle: ${String.format("%.1f", avgKneeAngle)}°
                                                                Contact Distance: ${String.format("%.1f", minHandToFoot)}
                                                                Form: ${if (isGoodForm) "Good" else "Needs improvement"}
                                                                """.trimIndent()
                                                            }

                                                            // State machine for exercise
                                                            if (isContact && !isExercising) {
                                                                isExercising = true
                                                                feedback = "Good reach! Now return to standing"
                                                            }
                                                            else if (isStanding && isExercising) {
                                                                isExercising = false
                                                                totalReps++

                                                                if (isGoodForm) {
                                                                    accurateReps++
                                                                    feedback = "Perfect form! Rep: $totalReps"
                                                                } else {
                                                                    inaccurateReps++
                                                                    if (backAngle <= 70) {
                                                                        feedback = "Rep counted, but keep back straighter. Rep: $totalReps"
                                                                    } else {
                                                                        feedback = "Rep counted, but bend knees less. Rep: $totalReps"
                                                                    }
                                                                }
                                                            }
                                                            else if (!isExercising && isStanding) {
                                                                feedback = "Ready for next rep"
                                                            }
                                                            else if (isExercising && !isContact && !isStanding) {
                                                                feedback = "Return to standing position"
                                                            }
                                                            else if (isBentPosition && !isExercising && !isStanding) {
                                                                feedback = "Reach further toward your toes"
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
                                        Log.e("ToeTouches", "Image analysis error", e)
                                        imageProxy.close()
                                    }
                                }
                            })

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    ctx as androidx.lifecycle.LifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (exc: Exception) {
                                Log.e("ToeTouches", "Camera binding failed", exc)
                            }
                        } catch (e: Exception) {
                            Log.e("ToeTouches", "Camera initialization failed", e)
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
                        feedback.contains("calibrat", ignoreCase = true) -> Color.LightGray
                        else -> Color.White
                    }
                )
            ) {
                Text(
                    feedback,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats in a row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Reps", style = MaterialTheme.typography.labelMedium, color = Color.Black)
                    Text("$totalReps", style = MaterialTheme.typography.headlineSmall, color = Color.Black)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Accurate", style = MaterialTheme.typography.labelMedium, color = Color.Black)
                    Text("$accurateReps",
                        style = MaterialTheme.typography.headlineSmall,
                        color = primaryColor)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Inaccurate", style = MaterialTheme.typography.labelMedium, color = Color.Black)
                    Text("$inaccurateReps",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Red)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timer
            Text(
                "Time: ${exerciseTimer / 60}:${"%02d".format(exerciseTimer % 60)}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )

            // Reset button with rounded edges
            if (calibrationComplete) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        calibrationComplete = false
                        calibrationFrames = 0
                        totalReps = 0
                        accurateReps = 0
                        inaccurateReps = 0
                        exerciseTimer = 0
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
                    color = Color.Red,
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
                        name = "Standing Toe Touches",
                        timestamp = System.currentTimeMillis(),
                        reps = latestRepsForSave,
                        durationSeconds = latestDurationForSave
                    )
                )
            }
        }
    }
}

// Helper function to calculate distance between two points
private fun calculateDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dx = x2 - x1
    val dy = y2 - y1
    return sqrt(dx * dx + dy * dy)
}
