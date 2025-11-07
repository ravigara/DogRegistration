package com.example.dogregistration.util
import android.Manifest
import androidx.compose.runtime.Composable
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CameraPermissionRequest(onGranted: () -> Unit) {
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    // Change this line
    if (cameraPermissionState.status.isGranted) {
        onGranted()
    } else {
        cameraPermissionState.launchPermissionRequest()
    }
}
