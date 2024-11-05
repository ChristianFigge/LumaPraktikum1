package com.example.lumapraktikum1.ui.composables.sensor.gyroscope

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.example.lumapraktikum1.model.GyroscopeReading
import com.example.lumapraktikum1.ui.composables.system.LifeCycleHookWrapper
import com.example.lumapraktikum1.ui.composables.system.SensorSettingsModal
import kotlinx.coroutines.launch

@Composable
fun GyroscopeComposable(
    navController: NavController,
) {
    val ctx = LocalContext.current

    var sensorManager by remember { mutableStateOf<SensorManager?>(null) }
    var sensorEventListener by remember { mutableStateOf<SensorEventListener?>(null) }

    var allCurrentReadings by remember {
        mutableStateOf<List<GyroscopeReading>>(listOf())
    }

    var singleCurrentGyroscope by remember {
        mutableStateOf<GyroscopeReading>(
            GyroscopeReading(
                timestampMillis = System.currentTimeMillis(),
                x = 0f,
                y = 0f,
                z = 0f,
            )
        )
    }

    var sampleRate by remember { mutableIntStateOf(3) }
    var isRecording by remember { mutableStateOf(false) }

    val insertionScope = rememberCoroutineScope()

    LifeCycleHookWrapper(
        onEvent = { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                sensorManager!!.unregisterListener(sensorEventListener)
            } else if (event == Lifecycle.Event.ON_CREATE) {
                sensorManager =
                    ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager

                sensorEventListener = object : SensorEventListener {
                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                    }

                    override fun onSensorChanged(event: SensorEvent) {
                        var singleCurrentGyroscope = GyroscopeReading(
                            timestampMillis = System.currentTimeMillis(),
                            x = event.values[0],
                            y = event.values[1],
                            z = event.values[2],
                        )

                        var executed = false

                        insertionScope.launch {
                            if (isRecording && !executed) {
                                println("isRecording")
                                allCurrentReadings = allCurrentReadings + singleCurrentGyroscope

                                executed = true
                            } else {
                                return@launch
                            }
                        }
                    }
                }

                sensorManager!!.registerListener(
                    sensorEventListener,
                    sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                    sampleRate
                )
            }
        },
        attachToDipose = { sensorManager?.unregisterListener(sensorEventListener) }
    )

    DisposableEffect(key1 = sampleRate) {
        sensorManager?.unregisterListener(sensorEventListener)
        sensorManager!!.registerListener(
            sensorEventListener,
            sensorManager!!.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
            sampleRate
        )

        onDispose { }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(10.dp))
        Row {
            Spacer(modifier = Modifier.width(10.dp))
            Button(onClick = { isRecording = !isRecording }) { Text("Start Recording") }
            Spacer(modifier = Modifier.width(10.dp))
            SensorSettingsModal(setSampleRate = { sampleRate = it })
            Spacer(modifier = Modifier.width(10.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        GyroscopeDataComposable(allCurrentReadings, 25, 2)

    }


}