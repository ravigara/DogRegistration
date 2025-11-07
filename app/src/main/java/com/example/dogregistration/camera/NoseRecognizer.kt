package com.example.dogregistration.camera

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp
import java.nio.ByteBuffer

/**
 * Manages loading the TFLite model and running inference
 * to generate a unique "embedding" (fingerprint) for a dog's nose.
 */
class NoseRecognizer(private val context: Context) {

    private var interpreter: Interpreter? = null

    // --- These MUST match your Python training script ---
    private val MODEL_NAME = "dog_nose_model.tflite"
    private val INPUT_IMAGE_WIDTH = 224
    private val INPUT_IMAGE_HEIGHT = 224
    private val EMBEDDING_SIZE = 128 // The output size (e.g., FloatArray[128])

    // This processor will resize/pad and normalize the image
    // to exactly match the model's training input
    private val imageProcessor: ImageProcessor

    init {
        imageProcessor = ImageProcessor.Builder()
            // 1. Resize/Pad to 224x224, maintaining aspect ratio
            // This matches the "tf.image.resize_with_pad" you used in Python
            .add(ResizeWithCropOrPadOp(INPUT_IMAGE_HEIGHT, INPUT_IMAGE_WIDTH))

            // 2. Normalize from [0, 255] (Bitmap) to [0, 1] (Float)
            // This matches the "image / 255.0" in your Python pipeline
            .add(NormalizeOp(0.0f, 255.0f))
            .build()

        loadModel()
    }

    private fun loadModel() {
        try {
            // Load the model file from the 'assets' folder
            val modelByteBuffer: ByteBuffer = FileUtil.loadMappedFile(context, MODEL_NAME)

            // --- CRITICAL ---
            // We must use the .Options() to enable the Flex Ops
            // that your model requires.
            val options = Interpreter.Options()
            // This line is no longer needed with the 'select-tf-ops' library
            // but it's good practice.

            interpreter = Interpreter(modelByteBuffer, options)
            Log.i("NoseRecognizer", "TFLite model loaded successfully.")
        } catch (e: Exception) {
            Log.e("NoseRecognizer", "Error loading TFLite model", e)
        }
    }

    /**
     * Takes a photo URI, processes it, and generates a unique embedding.
     *
     * @param imageUri The Uri of the photo to analyze (the nose).
     * @return A FloatArray of size [EMBEDDING_SIZE], or null if it fails.
     */
    fun getEmbedding(imageUri: Uri): FloatArray? {
        if (interpreter == null) {
            Log.e("NoseRecognizer", "Interpreter is not initialized.")
            return null
        }

        try {
            // 1. Load the image from Uri into a Bitmap
            val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)

            // 2. Pre-process the image
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val processedImage = imageProcessor.process(tensorImage)

            // 3. Prepare the output buffer
            // This buffer will hold the result (the FloatArray[128])
            val outputBuffer = Array(1) { FloatArray(EMBEDDING_SIZE) }

            // 4. Run inference
            // The model itself will handle Grayscale, Contrast, etc.
            interpreter?.run(processedImage.buffer, outputBuffer)

            // 5. Return the result
            return outputBuffer[0]

        } catch (e: Exception) {
            Log.e("NoseRecognizer", "Error generating embedding", e)
            return null
        }
    }
}