package com.example.dogregistration.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dogregistration.R // Make sure to import your R class

@Composable
fun WelcomeScreen(
    onRegisterClick: () -> Unit,
    onScanClick: () -> Unit // New callback for the "Scan" button
) {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. The Background Image
        Image(
            painter = painterResource(id = R.drawable.welcome_background), // Your image
            contentDescription = "A cute dog background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 2. The Content on top
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Dog Nose ID",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(64.dp))

            // 1. Register Button
            Button(
                onClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Register a New Dog", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Scan/Identify Button
            Button(
                onClick = onScanClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Identify a Dog", fontSize = 18.sp)
            }
        }
    }
}