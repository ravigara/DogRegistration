package com.example.dogregistration.ui
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun HeaderText(text: String) {
    Text(text, style = androidx.compose.material3.MaterialTheme.typography.headlineSmall)
}