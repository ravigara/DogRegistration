package com.example.dogregistration.camera

import android.graphics.Rect
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.ObjectDetector
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

/**
 * Passes back the list of detected objects and the size of the image they were
 * detected in (to help with coordinate scaling).
 */
typealias ObjectDetectedListener = (objects: List<DetectedObject>, imageSize: Size) -> Unit

class ObjectDetectionAnalyzer(
    private val listener: ObjectDetectedListener
) : ImageAnalysis.Analyzer {

    // Configure the ML Kit Object Detector for live video
    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.STREAM_MODE)
        .enableClassification() // We want to know *what* it is (person, animal)
        .build()

    private val detector: ObjectDetector = ObjectDetection.getClient(options)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        // Get rotation degrees to pass to ML Kit
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        // Create an InputImage from the ImageProxy
        val inputImage = InputImage.fromMediaImage(mediaImage, rotationDegrees)

        // Get the size of the image being analyzed
        val imageSize = Size(imageProxy.width, imageProxy.height)

        // Process the image using a listener instead of await()
        // This removes the need for the kotlinx-coroutines-play-services dependency
        detector.process(inputImage)
            .addOnSuccessListener { results ->
                // Pass the full results list and image size to our listener
                listener(results, imageSize)
            }
            .addOnFailureListener { e ->
                Log.e("ObjectDetectionAnalyzer", "ML Kit error: ${e.message}", e)
            }
            .addOnCompleteListener {
                // VERY IMPORTANT: Close the ImageProxy to get the next frame
                imageProxy.close()
            }
    }
}