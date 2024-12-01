package com.example.dynodroid

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.util.Arrays

/**
 * AppScanner class for analyzing Android applications.
 *
 * @property context The application context.
 */
class AppScanner(private val context: Context) {
    private var classifier: TFLiteClassifier? = null

    /**
     * Scans an app and classifies it as either "Malware" or "Benign".
     *
     * @param packageName The package name of the app to scan.
     * @return A string indicating the scan result ("Malware", "Benign", or "Scan failed").
     */
    fun scanApp(packageName: String): String {
        try {
            // Initialize the classifier if it hasn't been already
            if (classifier == null) {
                classifier = TFLiteClassifier(context)
            }

            val packageManager = context.packageManager
            val inputFeatures = prepareInputFeatures(packageManager, packageName)

            val prediction = classifier!!.predict(inputFeatures)
            Log.d("Prediction", prediction?.joinToString("\n") ?: "None")

            // Extract malware probability from prediction
            val malwareProbability = prediction?.get(1) ?: 0f

            // Classify based on a threshold
            return if (malwareProbability > 0.5) "Malware" else "Benign"
        } catch (e: Exception) {
            Log.e("AppScanner", "Error during scan: $e")
            return "Scan failed"
        }
    }

    /**
     * Prepares input features for the classifier based on the app's package information.
     *
     * @param pm The PackageManager instance.
     * @param packageName The package name of the app.
     * @return A FloatArray containing the prepared input features.
     */
    private fun prepareInputFeatures(pm: PackageManager, packageName: String): FloatArray {
        val input = FloatArray(10) // 10 features, excluding the label
        try {
            val packageInfo = pm.getPackageInfo(
                packageName,
                PackageManager.GET_PERMISSIONS or
                        PackageManager.GET_PROVIDERS or
                        PackageManager.GET_ACTIVITIES or
                        PackageManager.GET_RECEIVERS or
                        PackageManager.GET_SERVICES or
                        PackageManager.GET_CONFIGURATIONS
            )
            // Populate input features
            input[0] = 0f // api_call (placeholder)
            input[1] = (packageInfo.permissions?.size ?: 0).toFloat() // permission count
            Log.d("Permissions", packageInfo.permissions?.joinToString("\n")?:"NONE")
            input[2] = 0f // url (placeholder)
            input[3] = (packageInfo.providers?.size ?: 0).toFloat() // provider count
            Log.d("Providers", packageInfo.providers?.joinToString("\n")?:"NONE")
            input[4] = (packageInfo.reqFeatures?.size ?: 0).toFloat() // feature count
            Log.d("Features", packageInfo.reqFeatures?.joinToString("\n")?:"NONE")
            input[5] = 0f // intent (placeholder)
            input[6] = (packageInfo.activities?.size ?: 0).toFloat() // activity count
            Log.d("Activities", packageInfo.activities?.joinToString("\n")?:"NONE")
            input[7] = 0f // call (placeholder)

            // service_receiver count (combined services and receivers)
            val serviceCount = packageInfo.services?.size ?: 0
            val receiverCount = packageInfo.receivers?.size ?: 0
            Log.d("Services", packageInfo.services?.joinToString("\n")?:"NONE")
            Log.d("Recievers", packageInfo.receivers?.joinToString("\n")?:"NONE")
            input[8] = (serviceCount + receiverCount).toFloat()

            // real_permission (using requested permissions as an approximation)
            input[9] = (packageInfo.requestedPermissions?.size ?: 0).toFloat()
            Log.d("Real Permissions", packageInfo.requestedPermissions?.joinToString("\n")?:"NONE")

            Log.d("Input", input.contentToString())
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
            // Handle the exception by setting all inputs to 0
            Arrays.fill(input, 0f)
        }
        return input
    }
    fun getRawFeatures(packageName: String, context: Context): FloatArray {
        val pm = context.packageManager
        return prepareInputFeatures(pm, packageName)
    }
}