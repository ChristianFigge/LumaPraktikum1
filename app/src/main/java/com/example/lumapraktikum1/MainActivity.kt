package com.example.lumapraktikum1

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.example.lumapraktikum1.ui.theme.LumaPraktikum1Theme
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : ComponentActivity() {
    private val MIN_SENSOR_DELAY_MS: Int = 20
    private var str_accelData = mutableStateOf("\nNO DATA YET\n")
    private var str_gyrosData = mutableStateOf("\nNO DATA YET\n")
    private var str_lightData = mutableStateOf("\nNO DATA YET\n")
    private var str_magnetData = mutableStateOf("\nNO DATA YET\n")

    private lateinit var sensorManager: SensorManager
    private val SENSOR_TYPES = listOf(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_LIGHT,
        Sensor.TYPE_MAGNETIC_FIELD
    )

    // alle sensor Objekte als Dictionary, Sensor.TYPE_X jeweils als key
    private var sensorListeners = mutableMapOf<Int, LumaticSensorListener>()
    //private var sensorDataStrings = mutableMapOf<Int, MutableState<String>>()
    private var sensorRunnables = mutableMapOf<Int, Runnable>()


    private lateinit var locationManager: LocationManager
    private val locationProviders =
        listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
    private var locationListenersAndData =
        mutableMapOf<String, Pair<LocationListener, MutableState<String>>>()

    private val locationPermissions = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
    private var locationPermissionsGranted: Boolean = true

    // delayed sensor loop:
    private var handler = Handler() // deprecated TODO update
    //private var runDelayedSensorLoop: Boolean = true
    //private var runnable: Runnable? = null

    @Composable
    private fun PrintSensorData(
        label: String,
        dataString: MutableState<String>?,
        modifier: Modifier = Modifier
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append("$label:\n")
                }
                append("${dataString?.value}")
            },
            textAlign = TextAlign.Center,
            modifier = modifier.padding(horizontal = 20.dp, vertical = 10.dp)
        )
    }

    private fun getMagnitude(values: FloatArray): Float {
        var result = 0.0f
        values.forEach {
            result += it.pow(2)
        }
        return sqrt(result)
    }

    private fun radToDeg(rad: Float): Double {
        return rad * 180 / Math.PI
    }

    private fun createListeners() {
        // Sensor Listeners anlegen (1 für jeden sensor_type)
        SENSOR_TYPES.forEach {
            sensorListeners[it] = object : LumaticSensorListener {
                override var runDelayedLoop : Boolean = false

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                    // kann leer sein, muss aber implementiert werden
                }

                override fun onSensorChanged(event: SensorEvent?) {
                    when (event?.sensor?.type) {
                        Sensor.TYPE_GYROSCOPE -> {
                            str_gyrosData.value =
                                "X: %.2f deg/s\nY: %.2f deg/s\nZ: %.2f deg/s\nMag: %.2f deg/s".format(
                                    radToDeg(event.values[0]),
                                    radToDeg(event.values[1]),
                                    radToDeg(event.values[2]),
                                    radToDeg(getMagnitude(event.values))
                                )
                        }

                        Sensor.TYPE_ACCELEROMETER -> {
                            str_accelData.value =
                                "X: %.2f m/s²\nY: %.2f m/s²\nZ: %.2f m/s²\nMag: %.2f m/s²".format(
                                    event.values[0],
                                    event.values[1],
                                    event.values[2],
                                    getMagnitude(event.values)
                                )
                        }

                        Sensor.TYPE_LIGHT -> {
                            str_lightData.value = "\n${event.values[0].toInt()} lx"
                        }

                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            str_magnetData.value =
                                "X: %.2f uT\nY: %.2f uT\nZ: %.2f uT".format(
                                    event.values[0],
                                    event.values[1],
                                    event.values[2]
                                )
                        }
                    }

                    if (this.runDelayedLoop) { // unregister nach 1 readout:
                        sensorManager.unregisterListener(this)
                    }
                }
            }

        }

        // Location Listeners & Data als dictionary mit ["provider"] = Pair<Listener, str_Data>
        locationProviders.forEach {
            val str_locData = mutableStateOf("NO DATA YET\n")

            locationListenersAndData[it] = Pair<LocationListener, MutableState<String>>(
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        //Log.d("Location", "Position:\nLatitude: ${location.latitude}\nLongitude: ${location.longitude}")
                        str_locData.value =
                            "Lat: ${location.latitude}\nLong: ${location.longitude}\nAltitude: ${location.altitude}"
                    }
                },
                str_locData
            )
        }
    }

    private fun registerSensorListener(sensorType : Int, sampleFrequencyUs : Int, runDelayedLoop : Boolean) {
        sensorListeners[sensorType]?.runDelayedLoop = runDelayedLoop

        sensorManager.registerListener(
            sensorListeners[sensorType],
            sensorManager.getDefaultSensor(sensorType),
            sampleFrequencyUs,
            sampleFrequencyUs
        )
    }

    private fun unregisterSensorListener(sensorType : Int) {
        sensorManager.unregisterListener(sensorListeners[sensorType])
    }

    // Für langsame Samplerates:
    // .registerListener(.., samplingPeriodUs) wird für Werte >200ms scheinbar ignoriert
    // und ein postDelayed skript im Listener würde nur die Verarbeitung delayen, nicht das sampling.
    // Also workaround mit delayed register & unregister (im Listener) nach dem ersten readout:
    private fun startDelayedSensorLoop(sensorType : Int, sampleFrequencyMs : Long) {
        sensorRunnables[sensorType] = object : Runnable {
            override fun run() {
                registerSensorListener(sensorType, MIN_SENSOR_DELAY_MS * 1000, true)
                handler.postDelayed(this, sampleFrequencyMs)
            }
        }
        handler.post(sensorRunnables[sensorType] as Runnable)
    }

    private fun stopDelayedSensorLoop(sensorType : Int) {
        sensorRunnables[sensorType]?.let { handler.removeCallbacks(it) }
    }

    private fun hasAllLocationPermissions(): Boolean {
        locationPermissions.forEach {
            if (ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    @SuppressLint("MissingPermission")  // permissions werden in onCreate() erteilt
    private fun registerLocationListeners(minTimeMs: Long = 1000L) {
        if (locationPermissionsGranted) {
            // registriere location listener für GPS & NETWORK:
            locationListenersAndData.forEach { (provider, listenerAndData) ->
                listenerAndData.second.value = "Waiting 4 signal ...\n"
                locationManager.requestLocationUpdates(
                    provider,
                    minTimeMs,
                    0f,
                    listenerAndData.first
                )
            }
        }
    }

    private fun unregisterLocationListeners() {
        locationListenersAndData.forEach { (_, listenerAndData) ->
            locationManager.removeUpdates(listenerAndData.first)
        }
    }

    private fun startSensor(sensorType : Int, sampleFrequencyMs : Int) {
        // nutzt .registerListener(..., samplingPeriodUs) für schnelle Frequenzen
        // (ansonsten total unzuverlässig) und einen delayed Loop für langsamere
        if (sampleFrequencyMs < 200) {
            registerSensorListener(sensorType, sampleFrequencyMs * 1000, false)
        } else {
            startDelayedSensorLoop(sensorType, sampleFrequencyMs.toLong())
        }
    }

    private fun stopSensor(sensorType : Int) {
        if(sensorListeners[sensorType]?.runDelayedLoop == true) {
            stopDelayedSensorLoop(sensorType)
        } else {
            unregisterSensorListener(sensorType)
        }
    }

    private fun startAllSensors(sampleFrequencyMs: Int) {
        SENSOR_TYPES.forEach {
            startSensor(it, sampleFrequencyMs)
        }
    }

    private fun stopAllSensors() {
        SENSOR_TYPES.forEach {
            stopSensor(it)
        }
    }

    private fun stopAllReadouts() {
        stopAllSensors()
        unregisterLocationListeners()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request Launcher definieren:
        locationPermissionRequest =
            registerForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { isGranted: Map<String, @JvmSuppressWildcards Boolean> ->
                if (isGranted.containsValue(true)) {
                    // Mindestens eine location permission erteilt (coarse oder fine)
                    locationPermissionsGranted = true
                    Log.i("LocPermissions", "Request: Location Permissions granted: $isGranted")
                } else {
                    // Keine permission erteilt
                    locationPermissionsGranted = false
                    locationListenersAndData.forEach { (_, listenerAndData) ->
                        listenerAndData.second.value =
                            "Standortzugriff verweigert\n(Änderbar in Einstellungen)"
                    }
                    Log.i("LocPermissions", "Request: Location Permissions denied.")
                }
            }

        // TODO shouldShowRequestPermissionRationale(permission) abfragen
        // s. https://developer.android.com/training/permissions/requesting#allow-system-manage-request-code
        if (!hasAllLocationPermissions()) {
            Log.i("LocPermissions", "Check: Location Permissions denied.")
            locationPermissionRequest.launch(locationPermissions)
        } else {
            Log.i("LocPermissions", "Check: Location Permissions granted.")
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        this.createListeners()

        enableEdgeToEdge()
        setContent {
            LumaPraktikum1Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // SENSOREN
                        Row() {
                            PrintSensorData("Accelerometer", str_accelData)
                            PrintSensorData("Gyroskop", str_gyrosData)
                        }
                        Row() {
                            PrintSensorData("Beleuchtung", str_lightData)
                            PrintSensorData("Magnetfeld", str_magnetData)
                        }

                        // LOCATIONS
                        locationListenersAndData.forEach { (provider, listenerAndData) ->
                            Row() {
                                PrintSensorData(
                                    "Position (${provider.uppercase(Locale.ROOT)})",
                                    listenerAndData.second
                                )
                            }
                        }

                        // BUTTONS
                        Row(Modifier.padding(top = 30.dp)) {
                            Button(
                                content = { Text("Fast Scan") },
                                modifier = Modifier.padding(horizontal = 20.dp),
                                onClick = {
                                    stopAllReadouts()
                                    startAllSensors(MIN_SENSOR_DELAY_MS)
                                    registerLocationListeners(0L)
                                }
                            )
                            Button(
                                content = { Text("Slow Scan") },
                                modifier = Modifier.padding(horizontal = 20.dp),
                                onClick = {
                                    stopAllReadouts()
                                    startAllSensors(1000)
                                    registerLocationListeners(1000L)
                                }
                            )
                        }
                        Row() {
                            Button(
                                content = { Text("STOP") },
                                modifier = Modifier.padding(vertical = 20.dp),
                                onClick = {
                                    stopAllReadouts()
                                    str_accelData.value = "\nSTOPPED\n"
                                    str_gyrosData.value = "\nSTOPPED\n"
                                    str_lightData.value = "\nSTOPPED\n"
                                    str_magnetData.value = "\nSTOPPED\n"

                                    if (locationPermissionsGranted) {
                                        locationListenersAndData.forEach { (_, listenerAndData) ->
                                            listenerAndData.second.value = "STOPPED\n"
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}