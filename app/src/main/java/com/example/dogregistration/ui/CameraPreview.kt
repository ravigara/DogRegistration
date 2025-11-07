package com.example.dogregistration.ui

import android.util.Size
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import com.example.dogregistration.camera.CameraController
import com.google.mlkit.vision.objects.DetectedObject
import androidx.compose.ui.graphics.toComposeRect

@Composable
fun CameraPreview(
    controller: CameraController,
    modifier: Modifier = Modifier,
    // New parameters to receive detection data
    detectedObjects: List<DetectedObject> = emptyList(),
    analysisImageSize: Size = Size(640, 480) // Default to our analyzer's size
) {
    val lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current

    Box(modifier = modifier) {
        // AndroidView is the bridge between Compose and the traditional Android View system.
        AndroidView(
            factory = { context ->
                // 1. Factory: This block creates the PreviewView ONCE.
                PreviewView(context).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { previewView ->
                // 2. Update: This block connects your controller to the view.
                controller.startCamera(previewView, lifecycleOwner)
            }
        )

        // --- New: Canvas to draw bounding boxes ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Get the size of this Composable (which matches the PreviewView)
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate scaling factors
            // Note: This assumes the PreviewView and ImageAnalysis have the same orientation
            // and the PreviewView is in FILL_CENTER mode.
            val scaleX = canvasWidth / analysisImageSize.width.toFloat()
            val scaleY = canvasHeight / analysisImageSize.height.toFloat()

            // Use the same scale factor for both to maintain aspect ratio
            val scale = maxOf(scaleX, scaleY)

            // Calculate offsets to center the scaled image (for FILL_CENTER)
            val offsetX = (canvasWidth - analysisImageSize.width * scale) / 2
            val offsetY = (canvasHeight - analysisImageSize.height * scale) / 2

            // Draw each detected object
            detectedObjects.forEach { obj ->
                val box = obj.boundingBox.toComposeRect()

                // Apply scaling and offset to the box coordinates
                val scaledRect = box.translate(offsetX, offsetY).let {
                    it.copy(
                        left = it.left * scale,
                        top = it.top * scale,
                        right = it.right * scale,
                        bottom = it.bottom * scale
                    )
                }

                // Draw the box
                drawRect(
                    color = Color.Green,
                    topLeft = scaledRect.topLeft,
                    size = scaledRect.size,
                    style = Stroke(width = 2.dp.toPx())
                )

                // Optional: Draw labels
                // val label = obj.labels.firstOrNull()?.text ?: "Object"
                // ... (code to draw text using drawContext.canvas.nativeCanvas) ...
            }
        }
    }
}