package com.example.lumapraktikum1.ui.composables.magnetometer


import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.example.lumapraktikum1.model.MagnetometerReading


@Composable
fun MagnetometerDataComposable(
    readings: List<MagnetometerReading>,
    showAmount: Int,
    safetyPadding: Int
) {
    LazyColumn {
        items(1) {
            if (readings.size > (showAmount + safetyPadding)) {
                readings.subList(
                    readings.size - showAmount,
                    readings.size
                ).asReversed().forEach {
                    Text(
                        "(${it.x},  ${it.y}, ${it.z})",
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}