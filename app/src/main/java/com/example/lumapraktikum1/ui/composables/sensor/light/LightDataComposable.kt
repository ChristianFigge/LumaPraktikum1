package com.example.lumapraktikum1.ui.composables.sensor.light


import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.sp
import com.example.lumapraktikum1.model.LightSensorReading


@Composable
fun LightDataComposable(readings: List<LightSensorReading>, showAmount: Int, safetyPadding: Int) {
    LazyColumn {
        items(1) {
            if (readings.size > (showAmount + safetyPadding)) {
                readings.subList(
                    readings.size - showAmount,
                    readings.size
                ).forEach {
                    Text(
                        "(${it.lux})",
                        fontSize = 20.sp
                    )
                }
            }
        }
    }
}