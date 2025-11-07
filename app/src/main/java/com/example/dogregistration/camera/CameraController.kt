package com.example.dogregistration.camera

import android.content.Context
import android.net.Uri
import android.util.Log
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.objects.DetectedObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraController(private val context: Context) {
    private var imageCapture: ImageCapture? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // --- New properties for manual zoom and object detection ---
    private var cameraControl: CameraControl? = null
    private var cameraInfo: CameraInfo? = null

    /**
     * A public callback to send detected objects (and their source image size)
     * up to the Composable UI layer.
     */
    var onObjectsDetected: (objects: List<DetectedObject>, imageSize: Size) -> Unit = { _, _ -> }

    fun startCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            // --- Set up the ImageAnalysis use case ---
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480)) // A reasonable size for analysis
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            // --- Set the analyzer ---
            imageAnalysis.setAnalyzer(cameraExecutor, ObjectDetectionAnalyzer { objects, imageSize ->
                // Pass the results from the analyzer to our public callback
                onObjectsDetected(objects, imageSize)
            })

            try {
                cameraProvider.unbindAll()

                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis, // Bind the analyzer
                    imageCapture
                )

                // --- Get the CameraControl and CameraInfo for manual zoom ---
                this.cameraControl = camera.cameraControl
                this.cameraInfo = camera.cameraInfo

                Log.d("CameraController", "Camera started successfully")
            } catch (exc: Exception) {
                Log.e("CameraController", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Sets the camera's zoom ratio. Clamped between min and max.
     */
    fun setZoomRatio(zoomRatio: Float) {
        val info = cameraInfo ?: return
        val control = cameraControl ?: return

        val zoomState = info.zoomState.value ?: return
        val clampedZoom = zoomRatio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)

        control.setZoomRatio(clampedZoom)
    }

    /**
     * Gets the current zoom ratio.
     */
    fun getZoomRatio(): Float {
        return cameraInfo?.zoomState?.value?.zoomRatio ?: 1.0f
    }

    fun takePhoto(outputDir: File, onPhotoTaken: (Uri?) -> Unit) {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
            outputDir,
            SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
            outputOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e("CameraController", "Photo capture failed: ${exc.message}", exc)
                    onPhotoTaken(null)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    Log.d("CameraController", "Photo saved: $savedUri")
                    onPhotoTaken(savedUri)
                }
            }
        )
    }

    fun shutdown() {
        cameraExecutor.shutdown()
    }
}