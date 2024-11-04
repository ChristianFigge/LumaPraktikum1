package com.example.lumapraktikum1.ui.composables.sensor

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.example.lumapraktikum1.LumaticSensorListener
import com.example.lumapraktikum1.core.SaveSensorDataService
import com.example.lumapraktikum1.ui.composables.system.LifeCycleHookWrapper
import java.util.Locale
import kotlin.math.pow
import kotlin.math.sqrt

typealias SensorType = Int



@Composable
fun AllSensorComposable(
    saveSensorDataService: SaveSensorDataService,
    navController: NavController
){

    val ctx = LocalContext.current

    LifeCycleHookWrapper(
        onEvent = { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                //sensorManager!!.unregisterListener(sensorGyroscopeEventListener)
                unregisterAllLocationListeners()
            } else if (event == Lifecycle.Event.ON_CREATE) {


                /*** ------------- INIT Manager & Listener ------------ ***/


                /*** ------------- INIT Manager & Listener ------------ ***/
                sensorManager = ctx.getSystemService(Context.SENSOR_SERVICE) as SensorManager
                locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                createListeners(saveSensorDataService)


            }
        },
        attachToDipose = {
            unregisterAllLocationListeners()
            //sensorManager?.unregisterListener(sensorGyroscopeEventListener)
            //sensorManager?.unregisterListener(sensorAccelerationEventListener)
        }
    )

    /*** ------------------- GUI DEF --------------------- ***/
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

    /*** GLOABLE MEMBER FÜR SENSOREN ***/
    /* 20ms delay entspricht SENSOR_DELAY_GAME, sollte also garantiert laufen,
     * ausreichend schnell sein und keine zusätzliche permission brauchen. */
    private val MIN_SENSOR_DELAY_MS: Int = 20
    private lateinit var sensorManager: SensorManager
    private val sensorLoopHandler = Handler(Looper.getMainLooper())
    private val SENSOR_TYPES = listOf(
        Sensor.TYPE_ACCELEROMETER,
        Sensor.TYPE_GYROSCOPE,
        Sensor.TYPE_LIGHT,
        Sensor.TYPE_MAGNETIC_FIELD
    )
    /* alle sensor Objekte als Dictionary, mit Sensor.TYPE_XY jeweils als key */
    private var sensorListeners = mutableMapOf<SensorType, LumaticSensorListener>()
    private var sensorDataStrings = mutableMapOf<SensorType, MutableState<String>>()
    private var sensorRunnables = mutableMapOf<SensorType, Runnable>()

    /*** GLOABLE MEMBER FÜR LOCATIONS ***/
    private lateinit var locationManager: LocationManager
    private val LOCATION_PROVIDERS = listOf(
        LocationManager.GPS_PROVIDER,
        LocationManager.NETWORK_PROVIDER
    )
    private var locationListenersAndData =
        mutableMapOf<String, Pair<LocationListener, MutableState<String>>>()



    /***  ------------------------ START Sensor Steuerung ------------------------------ ***/
    /**
     * Registriert den Default-Sensor eines Sensor-Typs beim globalen SensorManager.
     * @param sensorType integer Identifier für den Sensor Typ (z.B. Sensor.TYPE_LIGHT)
     * @param sampleFrequencyMs gewünschte Sample Geschwindigkeit des Sensors in Millisekunden
     * @param runDelayedLoop Switch für Loop bei langsameren Sample Geschwindigkeiten (wird intern gehandelt)
     */
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

    /**
     * Meldet Default-Sensor beim globalen SensorManager ab.
     * @param sensorType integer Identifier für den Sensor Typ (z.B. Sensor.TYPE_LIGHT)
     */
    private fun unregisterSensorListener(sensorType : Int) {
        sensorManager.unregisterListener(sensorListeners[sensorType])
    }

    /** Startet einen timed loop für den Default-Sensor des gegebenen Typs bei langsameren Samplerates.
     * @param sensorType integer Identifier für den Sensor Typ (z.B. Sensor.TYPE_LIGHT)
     * @param sampleFrequencyMs gewünschte Sample Geschwindigkeit des Sensors in Millisekunden
     */
    /* Grund: .registerListener(.., samplingPeriodUs) wird für Werte >200ms scheinbar ignoriert
     * und ein postDelayed skript im Listener würde nur die Verarbeitung delayen, nicht das sampling.
     * Also workaround mit delayed register & unregister (im Listener) nach dem ersten readout. */
    private fun startDelayedSensorLoop(sensorType : Int, sampleFrequencyMs : Long) {
        sensorRunnables[sensorType] = object : Runnable {
            override fun run() {
                registerSensorListener(sensorType, MIN_SENSOR_DELAY_MS, true)
                sensorLoopHandler.postDelayed(this, sampleFrequencyMs)
            }
        }
        sensorLoopHandler.post(sensorRunnables[sensorType] as Runnable)
    }

    /**
     * Stoppt den timed loop für den Default-Sensor des gegebenen Typs.
     * @param sensorType integer Identifier für den Sensor Typ (z.B. Sensor.TYPE_LIGHT)
     */
    private fun stopDelayedSensorLoop(sensorType : Int) {
        sensorRunnables[sensorType]?.let { sensorLoopHandler.removeCallbacks(it) }
    }

    /**
     * Startet den Default-Sensor eines gegebenen Sensor Typs.
     * @param sensorType integer Identifier für den Sensor Typ (z.B. Sensor.TYPE_LIGHT)
     * @param sampleFrequencyMs gewünschte Sample Geschwindigkeit des Sensors in Millisekunden
     */
    private fun startSensor(sensorType : Int, sampleFrequencyMs : Int) {
        // nutzt .registerListener(..., samplingPeriodUs) für schnelle Frequenzen
        // (ansonsten total unzuverlässig) und einen delayed Loop für langsamere
        if (sampleFrequencyMs < 200) {
            registerSensorListener(sensorType, sampleFrequencyMs, false)
        } else {
            startDelayedSensorLoop(sensorType, sampleFrequencyMs.toLong())
        }
    }

    /**
     * Stopt den Default-Sensor eines gegebenen Sensor Typs.
     * @param sensorType integer Identifier für den Sensor Typ (z.B. Sensor.TYPE_LIGHT)
     */
    private fun stopSensor(sensorType : Int) {
        if(sensorListeners[sensorType]?.runDelayedLoop == true) {
            stopDelayedSensorLoop(sensorType)
        }
        unregisterSensorListener(sensorType)
        sensorDataStrings[sensorType]?.value = "\nSTOPPED\n"
    }

    /**
     * Startet die Default-Sensoren von allen im globalen
     * SENSOR_TYPES array definierten Sensor Typen.
     * @param sampleFrequencyMs gewünschte Sample Geschwindigkeit
     * des Sensors in Millisekunden
     */
    private fun startAllSensors(sampleFrequencyMs: Int) {
        SENSOR_TYPES.forEach {
            startSensor(it, sampleFrequencyMs)
        }
    }

    /**
     * Stoppt die Default-Sensoren von allen im globalen array
     * SENSOR_TYPES definierten Sensor Typen.
     */
    private fun stopAllSensors() {
        SENSOR_TYPES.forEach {
            stopSensor(it)
        }
    }

    /**
     * Berechnet die Magnitude für gegebene (Sensor-)Werte.
     * @param values array mit den Werten
     * @return Wurzel der Quadratsumme der Werte
     */
    fun getMagnitude(values: FloatArray): Float {
        var result = 0.0f
        values.forEach { result += it.pow(2) }
        return sqrt(result)
    }

    /**
     * Wandelt Radians in Grad um.
     * @param rad der Radians Wert
     * @return der entsprechende Grad Wert
     */
    private fun radToDeg(rad: Float): Double {
        return rad * 180 / Math.PI
    }
    /***  ---------------------------- ENDE Sensor Steuerung ----------------------- ***/


    /***  --------------------------- START Location Steuerung  ---------------------- ***/

    /**
     * Registriert den Location Listener eines gegebenen Providers
     * beim globalen LocationManager.
     * @param provider String identifier des Location Providers
     * ("gps" oder "network" bzw. die entsprechenden Konstanten im LocationManager)
     * @param minTimeMs gewünschte Mindestgeschwindigkeit der Location-Updates in Millisekunden
     * @param minDistanceM Minimaldistanz zur letzten bekannten Position in Metern,
     * die überschritten werden muss für ein Location Update
     */
    @SuppressLint("MissingPermission")  // permissions werden in onCreate() erteilt
    private fun registerLocationListener(provider : String, minTimeMs: Long, minDistanceM : Float = 0f) {
            locationListenersAndData[provider]?.let {
                it.second.value = "Waiting 4 signal ...\n"
                locationManager.requestLocationUpdates(provider, minTimeMs, minDistanceM, it.first)
            }
    }

    /**
     * Meldet den Location Listener eines gegebenen Providers beim
     * globalen LocationManager ab.
     * @param provider String identifier des Location Providers
     * ("gps" oder "network" bzw. die entsprechenden Konstanten im LocationManager)
     * */
    private fun unregisterLocationListener(provider : String) {
        locationListenersAndData[provider]?.let {
            locationManager.removeUpdates(it.first)
            it.second.value = "STOPPED\n"
        }
    }

    /**
     * Registriert die Location Listener für GPS & NETWORK beim globalen LocationManager.
     * @param minTimeMs gewünschte Mindestgeschwindigkeit der Location-Updates in Millisekunden
     * @param minDistanceM Minimaldistanz zur letzten bekannten Position in Metern,
     * die überschritten werden muss für ein Location Update
     */
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
    /***  ----------------------- ENDE Location Steuerung  ----------------------- ***/


    /***  ---------------------------- APP MAIN -------------------------------- ***/
    /**
     * Stoppt alle Datenupdates (Sensoren UND Location).
     */
    private fun stopAllReadouts() {
        stopAllSensors()
        unregisterAllLocationListeners()
    }

    /**
     * Stellt beliebige Textdaten dar.
     * @param label Überschrift der Daten, wird unterstrichen
     * @param dataString mutableState des Datenstrings
     */
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

    /**
     * Richtet die Listener für Sensoren- und Location-Services ein,
     * inklusive der von ihnen für Steuerung und Darstellung benötigten Objekte.
     */
    private fun createListeners(
        saveSensorDataService: SaveSensorDataService
        ) {
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
                            saveSensorDataService.saveGyroscopeData(event)
                            sensorDataStrings[Sensor.TYPE_GYROSCOPE]?.value =
                                "X: %.2f deg/s\nY: %.2f deg/s\nZ: %.2f deg/s\nMag: %.2f deg/s".format(
                                    radToDeg(event.values[0]),
                                    radToDeg(event.values[1]),
                                    radToDeg(event.values[2]),
                                    radToDeg(getMagnitude(event.values))
                                )
                        }

                        Sensor.TYPE_ACCELEROMETER -> {
                            saveSensorDataService.saveAccelerometerData(event)
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
                            saveSensorDataService.saveMagnetometerData(event)
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
