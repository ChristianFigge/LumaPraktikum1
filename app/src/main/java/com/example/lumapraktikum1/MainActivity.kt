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
import android.os.Looper
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

typealias SensorType = Int

class MainActivity : ComponentActivity() {
    private val MIN_SENSOR_DELAY_MS: Int = 20

    private lateinit var sensorManager: SensorManager
    private var sensorLoopHandler = Handler(Looper.getMainLooper())
    private val SENSOR_TYPES = listOf(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_LIGHT,
        Sensor.TYPE_MAGNETIC_FIELD
    )
    // alle sensor Objekte als Dictionary, Sensor.TYPE_X jeweils als key
    private var sensorListeners = mutableMapOf<SensorType, LumaticSensorListener>()
    private var sensorDataStrings = mutableMapOf<SensorType, MutableState<String>>()
    private var sensorRunnables = mutableMapOf<SensorType, Runnable>()

    private lateinit var locationManager: LocationManager
    private val LOCATION_PROVIDERS = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    )
    private var locationListenersAndData =
        mutableMapOf<String, Pair<LocationListener, MutableState<String>>>()

    private val LOCATION_PERMISSIONS = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
    private var locationPermissionsGranted: Boolean = true

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
        values.forEach { result += it.pow(2) }
        return sqrt(result)
    }

    private fun radToDeg(rad: Float): Double {
        return rad * 180 / Math.PI
    }

    private fun createListeners() {
        // mutableString map für Textausgabe der Sensordaten
        SENSOR_TYPES.forEach {
            sensorDataStrings[it] = mutableStateOf("\nNO DATA YET\n")
        }

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
                            sensorDataStrings[Sensor.TYPE_GYROSCOPE]?.value =
                                "X: %.2f deg/s\nY: %.2f deg/s\nZ: %.2f deg/s\nMag: %.2f deg/s".format(
                                    radToDeg(event.values[0]),
                                    radToDeg(event.values[1]),
                                    radToDeg(event.values[2]),
                                    radToDeg(getMagnitude(event.values))
                                )
                        }

                        Sensor.TYPE_ACCELEROMETER -> {
                            sensorDataStrings[Sensor.TYPE_ACCELEROMETER]?.value =
                                "X: %.2f m/s²\nY: %.2f m/s²\nZ: %.2f m/s²\nMag: %.2f m/s²".format(
                                    event.values[0],
                                    event.values[1],
                                    event.values[2],
                                    getMagnitude(event.values)
                                )
                        }

                        Sensor.TYPE_LIGHT -> {
                            sensorDataStrings[Sensor.TYPE_LIGHT]?.value = "\n${event.values[0].toInt()} lx"
                        }

                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            sensorDataStrings[Sensor.TYPE_MAGNETIC_FIELD]?.value =
                                "X: %.2f µT\nY: %.2f µT\nZ: %.2f µT".format(
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
        LOCATION_PROVIDERS.forEach {
            val str_locData = mutableStateOf("NO DATA YET\n")

            locationListenersAndData[it] = Pair<LocationListener, MutableState<String>>(
                object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        str_locData.value =
                            "Lat: ${location.latitude}\nLong: ${location.longitude}\nAltitude: ${location.altitude}"
                    }
                },
                str_locData
            )
        }
    }

    private fun registerSensorListener(sensorType : Int, sampleFrequencyMs : Int, runDelayedLoop : Boolean) {
        sensorListeners[sensorType]?.let {
            it.runDelayedLoop = runDelayedLoop

            // falls nötig invalid user input abfangen & millisec -> mikrosec
            val freqUs = if (sampleFrequencyMs < MIN_SENSOR_DELAY_MS) {
                MIN_SENSOR_DELAY_MS * 1000
            } else {
                sampleFrequencyMs * 1000
            }

            sensorManager.registerListener(
                it,
                sensorManager.getDefaultSensor(sensorType),
                freqUs,
                freqUs
            )
        }
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
                registerSensorListener(sensorType, MIN_SENSOR_DELAY_MS, true)
                sensorLoopHandler.postDelayed(this, sampleFrequencyMs)
            }
        }
        sensorLoopHandler.post(sensorRunnables[sensorType] as Runnable)
    }

    private fun stopDelayedSensorLoop(sensorType : Int) {
        sensorRunnables[sensorType]?.let { sensorLoopHandler.removeCallbacks(it) }
    }

    private fun hasAllLocationPermissions(): Boolean {
        LOCATION_PERMISSIONS.forEach {
            if (ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_DENIED) {
                return false
            }
        }
        return true
    }

    @SuppressLint("MissingPermission")  // permissions werden in onCreate() erteilt
    private fun registerLocationListener(provider : String, minTimeMs: Long, minDistanceM : Float = 0f) {
        if (locationPermissionsGranted) {
            locationListenersAndData[provider]?.let {
                it.second.value = "Waiting 4 signal ...\n"
                locationManager.requestLocationUpdates(provider, minTimeMs, minDistanceM, it.first)
            }
        }
    }

    private fun unregisterLocationListener(provider : String) {
        locationListenersAndData[provider]?.let {
            locationManager.removeUpdates(it.first)
            it.second.value = "STOPPED\n"
        }
    }

    private fun registerAllLocationListeners(minTimeMs: Long, minDistanceM : Float = 0f) {
        LOCATION_PROVIDERS.forEach {
            registerLocationListener(it, minTimeMs, minDistanceM)
        }
    }

    private fun unregisterAllLocationListeners() {
        LOCATION_PROVIDERS.forEach {
            unregisterLocationListener(it)
        }
    }

    private fun startSensor(sensorType : Int, sampleFrequencyMs : Int) {
        // nutzt .registerListener(..., samplingPeriodUs) für schnelle Frequenzen
        // (ansonsten total unzuverlässig) und einen delayed Loop für langsamere
        if (sampleFrequencyMs < 200) {
            registerSensorListener(sensorType, sampleFrequencyMs, false)
        } else {
            startDelayedSensorLoop(sensorType, sampleFrequencyMs.toLong())
        }
    }

    private fun stopSensor(sensorType : Int) {
        if(sensorListeners[sensorType]?.runDelayedLoop == true) {
            stopDelayedSensorLoop(sensorType)
        }
        unregisterSensorListener(sensorType)
        sensorDataStrings[sensorType]?.value = "\nSTOPPED\n"
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
        unregisterAllLocationListeners()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // PERMISSION Request Launcher definieren:
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
            locationPermissionRequest.launch(LOCATION_PERMISSIONS)
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
                            PrintSensorData("Accelerometer", sensorDataStrings[Sensor.TYPE_ACCELEROMETER])
                            PrintSensorData("Gyroskop", sensorDataStrings[Sensor.TYPE_GYROSCOPE])
                        }
                        Row() {
                            PrintSensorData("Beleuchtung", sensorDataStrings[Sensor.TYPE_LIGHT])
                            PrintSensorData("Magnetfeld", sensorDataStrings[Sensor.TYPE_MAGNETIC_FIELD])
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
                                    registerAllLocationListeners(0L)
                                }
                            )
                            Button(
                                content = { Text("Slow Scan") },
                                modifier = Modifier.padding(horizontal = 20.dp),
                                onClick = {
                                    stopAllReadouts()
                                    startAllSensors(1000)
                                    registerAllLocationListeners(1000L)
                                }
                            )
                        }
                        Row() {
                            Button(
                                content = { Text("STOP") },
                                modifier = Modifier.padding(vertical = 20.dp),
                                onClick = { stopAllReadouts() },
                            )
                        }
                    }
                }
            }
        }
    }
}