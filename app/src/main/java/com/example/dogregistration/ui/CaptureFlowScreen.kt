package com.example.dogregistration.ui

import android.net.Uri
import android.util.Size
import java.io.File
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dogregistration.camera.CameraController
import com.example.dogregistration.camera.ImageQualityAnalyzer
import com.example.dogregistration.camera.QualityMetrics
import com.example.dogregistration.camera.StabilizationMonitor
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog
import com.google.mlkit.vision.objects.DetectedObject
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign

enum class CaptureStep { NOSE, PAW, BODY1, BODY2, DONE }

@Composable
fun CaptureFlowScreen(onCompleted: (List<Uri>) -> Unit = {}) {
    val context = LocalContext.current
    val outputDir = context.cacheDir
    val analyzer = remember { ImageQualityAnalyzer() }

    // --- State for the new "Image Preview" feature ---
    var previewImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- States for the camera view ---
    var detectedObjects by remember { mutableStateOf<List<DetectedObject>>(emptyList()) }
    var analysisImageSize by remember { mutableStateOf(Size(640, 480)) } // Default
    var zoomRatio by remember { mutableStateOf(1f) }

    val cameraController = remember {
        CameraController(context).apply {
            // Set the callback to update our state
            onObjectsDetected = { objects, size ->
                detectedObjects = objects
                analysisImageSize = size
            }
        }
    }

    var step by remember { mutableStateOf(CaptureStep.NOSE) }
    val capturedUris = remember { mutableStateListOf<Uri>() }
    var lastCaptureMessage by remember { mutableStateOf("") }
    var isStable by remember { mutableStateOf(false) }

    val borderColor by remember(isStable) {
        mutableStateOf(if (isStable) Color.Green else Color.Red)
    }

    var showRejectedImageDialog by remember { mutableStateOf(false) }
    var lastRejectedUri by remember { mutableStateOf<Uri?>(null) }
    var lastRejectedMetrics by remember { mutableStateOf<QualityMetrics?>(null) }

    val stabilizationMonitor = remember {
        StabilizationMonitor(context) { stable -> isStable = stable }
    }

    DisposableEffect(previewImageUri) {
        // When we are IN PREVIEW, stop the sensors
        if (previewImageUri != null) {
            stabilizationMonitor.stop()
        } else {
            // When we are NOT in preview (camera is active), start sensors
            stabilizationMonitor.start()
            // Reset zoom when going back to camera
            zoomRatio = cameraController.getZoomRatio()
        }

        onDispose {
            stabilizationMonitor.stop()
        }
    }

    // Main UI switch: Show Camera or Show Preview
    if (previewImageUri == null) {
        // --- STATE 1: CAMERA IS ACTIVE ---
        Column(Modifier.fillMaxSize()) {
            Text(
                text = "Step: ${step.name}",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )

            // Camera Preview with dynamic border and zoom
            CameraPreview(
                controller = cameraController,
                detectedObjects = detectedObjects,
                analysisImageSize = analysisImageSize,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(4.dp)
                    .border(4.dp, borderColor, RoundedCornerShape(8.dp))
                    .clip(RoundedCornerShape(8.dp))
                    .pointerInput(Unit) {
                        // --- Add pinch-to-zoom gesture detector ---
                        detectTransformGestures { _, _, zoomChange, _ ->
                            zoomRatio = (zoomRatio * zoomChange).coerceIn(1f, 10f) // Example range
                            cameraController.setZoomRatio(zoomRatio)
                        }
                    }
            )

            // Controls
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                Button(
                    onClick = {
                        // Just take the photo and update the state
                        cameraController.takePhoto(outputDir) { uri ->
                            if (uri != null) {
                                previewImageUri = uri // This will switch the UI
                            } else {
                                lastCaptureMessage = "Capture failed ❌"
                            }
                        }
                    },
                    enabled = isStable,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(if (isStable) "Capture Photo" else "Hold Still…")
                }

                if (lastCaptureMessage.isNotEmpty()) {
                    Text(
                        text = lastCaptureMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                // Thumbnail row (unchanged)
                if (capturedUris.isNotEmpty()) {
                    // ... (LazyRow code from your original file) ...
                }
            }
        }
    } else {
        // --- STATE 2: IMAGE IS IN PREVIEW ---
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Accept Photo for ${step.name}?",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Show the captured image
            AsyncImage(
                model = previewImageUri,
                contentDescription = "Captured image preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.height(16.dp))

            // "Retake" and "Accept" buttons
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // RETAKE Button
                Button(
                    onClick = {
                        // Delete the temp file and go back to camera
                        previewImageUri?.path?.let { File(it).delete() }
                        previewImageUri = null // This switches UI back
                        lastCaptureMessage = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Retake")
                }

                // ACCEPT Button
                Button(
                    onClick = {
                        // --- Run Quality Analysis HERE ---
                        val uri = previewImageUri!!
                        val file = File(uri.path!!)
                        val metrics = analyzer.analyzeLuma(file.readBytes())

                        if (metrics.isGood) {
                            capturedUris.add(uri)
                            lastCaptureMessage = "Captured ${step.name} ✅"

                            // Advance to next step
                            step = when (step) {
                                CaptureStep.NOSE -> CaptureStep.PAW
                                CaptureStep.PAW -> CaptureStep.BODY1
                                CaptureStep.BODY1 -> CaptureStep.BODY2
                                CaptureStep.BODY2 -> CaptureStep.DONE
                                CaptureStep.DONE -> CaptureStep.DONE
                            }

                            if (step == CaptureStep.DONE) {
                                onCompleted(capturedUris.toList())
                            }
                        } else {
                            // Show rejection dialog and delete bad file
                            lastCaptureMessage = "Image rejected ❌ (Too dark/blurry)"
                            lastRejectedUri = uri
                            lastRejectedMetrics = metrics
                            showRejectedImageDialog = true
                            file.delete() // Delete the bad file
                        }

                        // Go back to the camera view
                        previewImageUri = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Accept")
                }
            }
        }
    }

    // Dialog for rejected images (unchanged from your original file)
    if (showRejectedImageDialog && lastRejectedUri != null) {
        RejectedImageDialog(
            imageUri = lastRejectedUri!!,
            metrics = lastRejectedMetrics,
            onDismiss = {
                showRejectedImageDialog = false
                lastRejectedUri = null // Clear the URI after dialog is dismissed
            }
        )
    }
}


// RejectedImageDialog composable (unchanged from your original file)
@Composable
fun RejectedImageDialog(imageUri: Uri, metrics: QualityMetrics?, onDismiss: () -> Unit) {
    // ... (Your existing dialog code is perfect, no changes needed) ...
}