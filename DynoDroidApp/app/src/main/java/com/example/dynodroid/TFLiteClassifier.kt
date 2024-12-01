package com.example.dynodroid

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil

/**
 * TFLiteClassifier for running inference on a TensorFlow Lite model.
 *
 * @property context The application context used to load the model file.
 */
class TFLiteClassifier(context: Context) {
    private var tflite: Interpreter? = null

    init {
        try {
            // Load the TensorFlow Lite model from assets
            val tfliteModel = FileUtil.loadMappedFile(context, "staticModel.tflite")
            // Initialize the TensorFlow Lite interpreter
            tflite = Interpreter(tfliteModel)
        } catch (ex: Exception) {
            Log.e("TFLiteClassifier", "Error initializing interpreter", ex)
        }
    }

    /**
     * Makes predictions using the loaded TensorFlow Lite model.
     *
     * @param input A FloatArray containing the input features.
     * @return A FloatArray containing the model's output, or null if prediction fails.
     * @throws IllegalStateException if the interpreter is not initialized.
     */
    fun predict(input: FloatArray): FloatArray {
        if (tflite == null) {
            throw IllegalStateException("Interpreter not initialized")
        }

        // Create output array (assuming model outputs [1, 2])
        val output = Array(1) { FloatArray(2) }

        // Run inference
        tflite!!.run(input, output)
        // Return the first row of the output (assuming batch size 1)
        return output[0]
    }

    /**
     * Closes the TensorFlow Lite interpreter and releases resources.
     */
    fun close() {
        tflite?.close()
        tflite = null
    }
}
