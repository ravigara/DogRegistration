package com.example.dogregistration

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import com.example.dogregistration.camera.NoseRecognizer
import com.example.dogregistration.data.AppDatabase
import com.example.dogregistration.ui.CaptureFlowScreenWithPermission
import com.example.dogregistration.ui.IdentifyDogScreen
import com.example.dogregistration.ui.RegistrationFormScreen
import com.example.dogregistration.ui.WelcomeScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Define the main screens of our app
enum class AppScreen {
    WELCOME,
    REGISTER_FLOW,
    SCAN_FLOW
}

class MainActivity : ComponentActivity() {

    // Lazily initialize the database and recognizer
    // This ensures they are created only when first needed.
    private val database by lazy { AppDatabase.getInstance(this) }
    private val noseRecognizer by lazy { NoseRecognizer(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // This state now controls the entire app's navigation
            var currentScreen by remember { mutableStateOf(AppScreen.WELCOME) }

            // Use a 'when' statement to show the correct screen
            when (currentScreen) {
                AppScreen.WELCOME -> {
                    WelcomeScreen(
                        onRegisterClick = { currentScreen = AppScreen.REGISTER_FLOW },
                        onScanClick = { currentScreen = AppScreen.SCAN_FLOW }
                    )
                }

                AppScreen.REGISTER_FLOW -> {
                    // This is your original multi-step registration flow
                    RegistrationFlow(
                        database = database,
                        noseRecognizer = noseRecognizer,
                        onComplete = {
                            // When registration is done, go back to welcome
                            currentScreen = AppScreen.WELCOME
                        }
                    )
                }

                AppScreen.SCAN_FLOW -> {
                    // This is the new Identification/Scan screen
                    IdentifyDogScreen(
                        database = database,
                        recognizer = noseRecognizer,
                        onBack = {
                            // When user presses back, go back to welcome
                            currentScreen = AppScreen.WELCOME
                        }
                    )
                }
            }
        }
    }
}

/**
 * A new composable to bundle your entire registration flow.
 * This is the same logic you had in MainActivity before, but now
 * it correctly generates the embedding.
 */
@Composable
fun RegistrationFlow(
    database: AppDatabase,
    noseRecognizer: NoseRecognizer,
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // States for the capture and form process
    var capturedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var captureCompleted by remember { mutableStateOf(false) }
    var noseEmbedding by remember { mutableStateOf<FloatArray?>(null) }

    if (!captureCompleted) {
        // --- STAGE 1: CAPTURE FLOW ---
        // This is your existing screen that uses Stabilization, Quality check, etc.
        CaptureFlowScreenWithPermission(
            onCompleted = { uris ->
                // --- Generate the embedding HERE ---
                // Find the first photo (which we assume is the NOSE)
                val noseUri = uris.firstOrNull()

                if (noseUri != null) {
                    // Run the model in a coroutine
                    coroutineScope.launch(Dispatchers.IO) {
                        noseEmbedding = noseRecognizer.getEmbedding(noseUri)
                    }
                }
                capturedUris = uris
                captureCompleted = true
            }
        )
    } else {
        // --- STAGE 2: REGISTRATION FORM ---
        RegistrationFormScreen(
            capturedUris = capturedUris,
            onSubmit = { dogProfile ->
                // --- Attach the embedding ---
                val finalProfile = dogProfile.copy(
                    embedding = noseEmbedding
                )

                // --- Save to Local Database! ---
                coroutineScope.launch(Dispatchers.IO) { // Run in background
                    database.dogDao().insertDog(finalProfile)
                }

                Toast.makeText(context, "${finalProfile.dogName} submitted!", Toast.LENGTH_LONG).show()

                // Call the onComplete callback to navigate back to Welcome
                onComplete()
            }
        )
    }
}