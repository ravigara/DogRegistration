package com.example.dogregistration.ui

import android.Manifest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.dogregistration.camera.CameraController
import com.example.dogregistration.camera.NoseRecognizer
import com.example.dogregistration.data.AppDatabase
import com.example.dogregistration.data.DogProfile
import com.example.dogregistration.data.euclideanDistance
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * This is the "brain" of the recognition logic. It runs in the background.
 */
private suspend fun findMatch(
    database: AppDatabase,
    recognizer: NoseRecognizer,
    imageUri: Uri,
    onResult: (String) -> Unit
) {
    // 1. Get the new embedding for the unknown dog (on a background thread)
    val queryEmbedding = withContext(Dispatchers.IO) {
        recognizer.getEmbedding(imageUri)
    }

    if (queryEmbedding == null) {
        onResult("Could not process image. Please try a clearer photo.")
        return
    }

    // 2. Get all dogs from your database (on a background thread)
    val allDogsInDatabase = withContext(Dispatchers.IO) {
        database.dogDao().getAllDogProfiles()
    }

    if (allDogsInDatabase.isEmpty()) {
        onResult("No dogs are registered in the database yet.")
        return
    }

    val MATCH_THRESHOLD = 0.8f

    var bestMatch: DogProfile? = null
    var smallestDistance = Float.MAX_VALUE

    // 4. The "Recognition" Loop
    for (dog in allDogsInDatabase) {
        if (dog.embedding != null) {
            val distance = euclideanDistance(queryEmbedding, dog.embedding)
            if (distance < smallestDistance) {
                smallestDistance = distance
                bestMatch = dog
            }
        }
    }

    // 5. The Final Decision
    if (bestMatch != null && smallestDistance <= MATCH_THRESHOLD) {
        // SUCCESS!
        onResult("Match Found!\n\nName: ${bestMatch!!.dogName}\nBreed: ${bestMatch!!.breed}\n(Distance: ${String.format("%.4f", smallestDistance)})")
    } else {
        // NO MATCH
        onResult("No match found in database.\n(Closest match distance: ${String.format("%.4f", smallestDistance)})")
    }
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun IdentifyDogScreen(
    database: AppDatabase,
    recognizer: NoseRecognizer,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cameraController = remember { CameraController(context) }
    val outputDir = context.cacheDir

    // --- State Management ---
    var showCamera by remember { mutableStateOf(false) }
    var showResultDialog by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf("") }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) } // For loading indicator

    // --- Permission Handling ---
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA) { isGranted ->
        if (isGranted) {
            showCamera = true
        } else {
            showPermissionRationale = true
        }
    }

    // --- Activity Launchers ---
    // 1. For picking from the gallery
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            uri?.let {
                isLoading = true
                coroutineScope.launch {
                    findMatch(database, recognizer, it) { result ->
                        resultMessage = result
                        isLoading = false
                        showResultDialog = true
                    }
                }
            }
        }
    )

    // 2. For after the camera takes a photo
    // --- THIS IS THE FIX ---
    // We explicitly define the type as (Uri?) -> Unit to match what takePhoto expects.
    val onPhotoTaken: (Uri?) -> Unit = { uri: Uri? ->
        showCamera = false // Hide camera view
        uri?.let {
            isLoading = true
            coroutineScope.launch {
                findMatch(database, recognizer, it) { result ->
                    resultMessage = result
                    isLoading = false
                    showResultDialog = true
                    // Clean up the temp photo
                    File(it.path ?: "").delete()
                }
            }
        }
    }

    // --- UI ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Identify Dog") },
                navigationIcon = {
                    IconButton(onClick = { if (!showCamera) onBack() else showCamera = false }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showCamera) {
                // --- STATE 1: CAMERA IS ACTIVE ---
                Column {
                    CameraPreview(
                        controller = cameraController,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    Button(
                        // This call will now work perfectly
                        onClick = { cameraController.takePhoto(outputDir, onPhotoTaken) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .height(50.dp)
                    ) {
                        Text("Scan Nose", fontSize = 18.sp)
                    }
                }
            } else {
                // --- STATE 2: CHOOSER UI ---
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "Scan a dog's nose to find a match in the database.",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(40.dp))

                    // "Take Photo" Button
                    Button(
                        onClick = {
                            if (cameraPermissionState.status.isGranted) {
                                showCamera = true
                            } else {
                                cameraPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, "Take Photo", modifier = Modifier.padding(end = 8.dp))
                        Text("Take Photo", fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // "Pick from Gallery" Button
                    Button(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, "From Gallery", modifier = Modifier.padding(end = 8.dp))
                        Text("Pick from Gallery", fontSize = 18.sp)
                    }
                }
            }

            // --- Loading Indicator ---
            if (isLoading) {
                Dialog(onDismissRequest = {}) {
                    Card(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.padding(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.width(24.dp))
                            Text("Identifying...", style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }

            // --- Dialog for showing the recognition result ---
            if (showResultDialog) {
                Dialog(onDismissRequest = { showResultDialog = false }) {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Recognition Result",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = resultMessage,
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showResultDialog = false },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("OK")
                            }
                        }
                    }
                }
            }

            // --- Dialog for camera permission rationale ---
            if (showPermissionRationale) {
                Dialog(onDismissRequest = { showPermissionRationale = false }) {
                    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(modifier = Modifier.padding(24.dp)) {
                            Text("Permission Required", style = MaterialTheme.typography.titleLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("We need camera access to scan a dog's nose. Please grant the permission.")
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = { showPermissionRationale = false },
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Text("OK")
                            }
                        }
                    }
                }
            }
        }
    }
}