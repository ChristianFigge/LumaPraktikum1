package com.example.lumapraktikum1.ui.composables.system

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorSettingsModal(
    setSampleRate: (Int) -> Unit,
) {
    var sliderPosition by remember { mutableStateOf(3.0f) }


            Column(modifier = Modifier.padding(horizontal = 30.dp)) {
                Text(text = getSampleRateDescr(sliderPosition.toInt()))
                Slider(
                    value = sliderPosition,
                    onValueChange = {
                        Log.d("sliderPosition", getSampleRateDescr(it.toInt()).toString())
                        sliderPosition = it
                        setSampleRate(getSampleRate(it.toInt()))
                    },
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.secondary,
                        activeTrackColor = MaterialTheme.colorScheme.secondary,
                        inactiveTrackColor = MaterialTheme.colorScheme.secondaryContainer,
                    ),
                    steps = 2,
                    valueRange = 0.1f..3f
                )

            }
            Spacer(Modifier.height(20.dp))
}

fun getSampleRate(i: Int) : Int{
    return when (i) {
        3 -> 200000
        2 ->  60000
        1 ->  20000
        0 ->      0
        else -> 200000
    }
}

fun getSampleRateDescr(i: Int): String {
    return when (i) {
        3 -> "Normal" //langsamstes
        2 -> "UI"
        1 -> "Game"
        0 -> "Fastest" //schnellstes
        else -> "Normal"
    }
}