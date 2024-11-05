package com.example.lumapraktikum1.ui.composables.system

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun HomeComposable(navController: NavHostController) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Lokalisierung und mobile Applikationen")
            Text("Wintersemester 2024/2025")
            Spacer(modifier = Modifier.height(10.dp))
            Text(buildAnnotatedString {
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline))
                { append("\"The Lumatics\":") }
            })
            Text("Christian Figge")
            Text("Julian Niedbal")
            Text("Malte Schmidt")
            Text("Fabian Zöllner")
            Spacer(modifier = Modifier.height(10.dp))
            Text("App: Kotlin und JetBrains Compose")
            Text("Backend: .NET und InfluxDB")
        }
    }
}