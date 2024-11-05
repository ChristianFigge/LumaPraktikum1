package com.example.lumapraktikum1.ui.composables.sensor

import DataCollector
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import com.example.lumapraktikum1.core.LumaticLocationListener
import com.example.lumapraktikum1.core.LumaticSensorListener
import com.example.lumapraktikum1.core.SaveSensorDataService
import com.example.lumapraktikum1.ui.composables.system.LifeCycleHookWrapper
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.FileNotFoundException
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

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
                createListeners(ctx)


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
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // SENSOREN MIT DELAY SLIDER
            SENSOR_TYPES.forEach { (iType, strName) ->
                SensorPanel(iType, strName,
                    Modifier.drawBehind {
                        drawLine(
                            Color.Gray,
                            Offset(0f, size.height),
                            Offset(size.width, size.height),
                            1.dp.toPx())
                    }
                )
            }

            // LOCATIONS
            locationListenersAndData.keys.forEach { provider ->
                LocationPanel(provider,
                    Modifier.drawBehind {
                        drawLine(
                            Color.Gray,
                            Offset(0f, size.height),
                            Offset(size.width, size.height),
                            1.dp.toPx())
                    }
                )
            }
            Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
            Configuration.getInstance().userAgentValue = "MapApp"

            /*** START/STOP ALL BUTTONS ***/
            Row(Modifier.padding(vertical = 20.dp)) {
                Button(
                    content = { Text("Stop All") },
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onClick = { stopAllReadouts() }
                )
                Button(
                    content = { Text("Start All") },
                    modifier = Modifier.padding(horizontal = 20.dp),
                    onClick = {
                        stopAllReadouts()
                        startAllSensors()
                        registerAllLocationListeners()
                    }
                )
            }

            /*** I/O Controls ***/
            Row () {
                Button(
                    content = { Text("Clear All") },
                    modifier = Modifier.padding(horizontal = 10.dp),
                    onClick = {
                        clearAllData(ctx)
                        strFileContents.value = ""
                        locationListenersAndData.values.forEach { listenerAndData ->
                            listenerAndData.first.removeMarker()
                        }
                    }
                )
                Button(
                    content = { Text("Load All") },
                    modifier = Modifier.padding(horizontal = 10.dp),
                    onClick = { strFileContents.value = readAllDataFromStorage(ctx) }
                )
                Button(
                    content = { Text("Save All") },
                    modifier = Modifier.padding(horizontal = 10.dp),
                    onClick = { writeAllDataToStorage(ctx) }
                )
            }
            Text (
                text = strFileContents.value,
                modifier = Modifier.padding(vertical = 20.dp),
            )
        }
    }

}

/*** GLOABLE MEMBER FÜR SENSOREN ***/
/* 20ms delay entspricht SENSOR_DELAY_GAME, sollte also garantiert laufen,
 * ausreichend schnell sein und keine zusätzliche permission brauchen. */
private val MIN_SENSOR_DELAY_MS: Int = 20
private lateinit var sensorManager: SensorManager
private val sensorLoopHandler = Handler(Looper.getMainLooper())
private val SENSOR_TYPES = mapOf(
    Sensor.TYPE_ACCELEROMETER to "Accelerometer",
    Sensor.TYPE_GYROSCOPE to "Gyroskop",
    Sensor.TYPE_LIGHT to "Beleuchtung",
    Sensor.TYPE_MAGNETIC_FIELD to "Magnetfeld",
)
/* alle sensor Objekte als Dictionary, mit Sensor.TYPE_XY jeweils als key */
private var sensorListeners = mutableMapOf<SensorType, LumaticSensorListener>()
private var sensorDataStrings = mutableMapOf<SensorType, MutableState<String>>()
private var sensorRunnables = mutableMapOf<SensorType, Runnable>()

/*** GLOABLE MEMBER FÜR LOCATIONS ***/
private lateinit var locationManager: LocationManager
private val LOCATION_PROVIDERS = listOf(
    LocationManager.GPS_PROVIDER,       // = "gps"
    LocationManager.NETWORK_PROVIDER    // = "network"
)
private var locationListenersAndData =
    mutableMapOf<String, Pair<LumaticLocationListener, MutableState<String>>>()

/* LOCATION--Permissions */
private val LOCATION_PERMISSIONS = arrayOf(
    android.Manifest.permission.ACCESS_FINE_LOCATION,
    android.Manifest.permission.ACCESS_COARSE_LOCATION
)
private lateinit var locationPermissionRequest: ActivityResultLauncher<Array<String>>
private var locationPermissionsGranted: Boolean = true

/*** GLOBALE MEMBER FÜR I/O ***/
private val strFileContents = mutableStateOf("")
private val dataCollectors = mutableListOf<DataCollector>()

/*** MEMBER FÜR SLIDER ***/
private val sliderValues = mutableMapOf<String, MutableState<Float>>()
private val listenerIsRunning = mutableMapOf<String, MutableState<Boolean>>()
private val mapViews = mutableMapOf<String, MapView>()


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
    stopSensor(sensorType)
    if (sampleFrequencyMs < 200) {
        registerSensorListener(sensorType, sampleFrequencyMs, false)
    } else {
        startDelayedSensorLoop(sensorType, sampleFrequencyMs.toLong())
    }
    listenerIsRunning[SENSOR_TYPES[sensorType] /* get name str */]?.value = true
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
    listenerIsRunning[SENSOR_TYPES[sensorType] /* get name str */]?.value = false
}

/**
 * Startet die Default-Sensoren von allen im globalen
 * SENSOR_TYPES array definierten Sensor Typen.
 * Nutzt die aktuelle Slider Stellung für den Delay Wert.
 */
private fun startAllSensors() {
    SENSOR_TYPES.forEach { (type, name) ->
        sliderValues[name]?.value?.roundToInt()?.let { startSensor(type, it) }
    }
}

/**
 * Stoppt die Default-Sensoren von allen im globalen array
 * SENSOR_TYPES definierten Sensor Typen.
 */
private fun stopAllSensors() {
    SENSOR_TYPES.keys.forEach {
        stopSensor(it)
    }
}
/***  ---------------------------- ENDE Sensor Steuerung ----------------------- ***/

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
    if (locationPermissionsGranted) {
        locationListenersAndData[provider]?.let {
            it.second.value = "Waiting 4 signal ...\n"
            locationManager.requestLocationUpdates(provider, minTimeMs, minDistanceM, it.first)
            listenerIsRunning[provider]?.value = true
        }
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
        listenerIsRunning[provider]?.value = false
    }
}

/**
 * Registriert die Location Listener für GPS & NETWORK beim globalen LocationManager.
 * Nutzt die aktuelle Slider Stellung für die Abfragegeschwindigkeit.
 * @param minDistanceM Minimaldistanz zur letzten bekannten Position in Metern,
 * die überschritten werden muss für ein Location Update
 */
private fun registerAllLocationListeners(minDistanceM : Float = 0f) {
    LOCATION_PROVIDERS.forEach {
        sliderValues[it]?.value?.roundToLong()?.let{ minTimeMs ->
            registerLocationListener(it, minTimeMs, minDistanceM) }
    }
}

private fun unregisterAllLocationListeners() {
    LOCATION_PROVIDERS.forEach {
        unregisterLocationListener(it)
    }
}
/***  ----------------------- ENDE Location Steuerung  ----------------------- ***/


/*** ----------------------------- START I/O ---------------------------- ***/
private fun writeAllDataToStorage(ctx : Context, successMsg : String = "Data saved") {
    try {
        dataCollectors.forEach { it.writeJsonToStorage(ctx) }
    } catch(e: Exception) {
        Toast.makeText(ctx, "Data saving failed", Toast.LENGTH_LONG).show()
        e.printStackTrace()
    }
    Toast.makeText(ctx, successMsg, Toast.LENGTH_SHORT).show()
}

private fun readAllDataFromStorage(ctx : Context) : String {
    val strOut = StringBuilder()
    dataCollectors.forEach {
        strOut.append("${it.name} data:\n\n")
        try {
            strOut.append("${it.readJsonFromStorage(ctx)}\n\n")
        } catch(e: FileNotFoundException) {
            strOut.append("File not found.\n\n")
        }
    }
    return strOut.toString()
}

private fun clearAllData(ctx : Context) {
    dataCollectors.forEach { it.clearData() }
    writeAllDataToStorage(ctx,"Data cleared") // clear json file
}
/*** ------------------------------ ENDE I/O ---------------------------- ***/


/***  ------------------------------- APP MAIN ------------------------------- ***/
/**
 * Richtet die Listener für Sensoren- und Location-Services ein,
 * inklusive der von ihnen für Steuerung und Darstellung benötigten Objekte.
 */
private fun createListeners(ctx : Context) {
    SENSOR_TYPES.forEach { (iType, strName) ->
        // mutableString map für Textausgabe der Sensordaten
        sensorDataStrings[iType] = mutableStateOf("\nNO DATA YET\n")
        sliderValues[strName] = mutableFloatStateOf(500f)

        // Sensor Listeners anlegen (1 für jeden sensor_type)
        val newListener = LumaticSensorListener(strName, sensorDataStrings, sensorManager)
        sensorListeners[iType] = newListener
        dataCollectors.add(newListener)
        listenerIsRunning[strName] = mutableStateOf(false)
    }

    // Location Listeners & Data als dictionary mit ["provider"] = Pair<Listener, str_Data>
    LOCATION_PROVIDERS.forEach {
        val newDataStr = mutableStateOf("NO DATA YET\n")
        val newMapView = MapView(ctx)
        mapViews[it] = newMapView
        val newListener = LumaticLocationListener(it, newDataStr, newMapView)
        sliderValues[it] = mutableFloatStateOf(1000f)

        locationListenersAndData[it] = Pair<LumaticLocationListener, MutableState<String>>(newListener, newDataStr)
        dataCollectors.add(newListener)
        listenerIsRunning[it] = mutableStateOf(false)
    }
}

/**
 * Stoppt alle Datenupdates (Sensoren UND Location).
 */
private fun stopAllReadouts() {
    stopAllSensors()
    unregisterAllLocationListeners()
}

/*** ------------------- COMPOSABLE --------------------------- ***/

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
        modifier = modifier,
    )
}

@Composable
private fun SampleSpeedControl(
    listenerName : String,
    onClick : () -> Unit,
    modifier : Modifier = Modifier,
    valueRange : ClosedFloatingPointRange<Float> = 0f..5000f,
    steps : Int = 19,
) {
    val sensorIsRunning = remember { listenerIsRunning[listenerName] }
    sliderValues[listenerName]?.value?.let { sliderValue ->
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = modifier
        ) {
            Box(contentAlignment = Alignment.Center,) {
                Text(
                    text = "Sample speed: ${sliderValue.roundToInt()}ms",
                    modifier = Modifier.padding(bottom = 40.dp)
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValues[listenerName]!!.value = it },
                    valueRange = valueRange,
                    steps = steps,
                    enabled = !sensorIsRunning!!.value,
                    modifier = Modifier.width(200.dp)
                )
            }
            Button(
                onClick = onClick,
                content = {
                    if (!sensorIsRunning!!.value) Text("Start")
                    else Text("Stop")
                },
                modifier = Modifier.padding(bottom = 20.dp, start = 20.dp),
            )
        }
    }
}

@Composable
private fun SensorPanel(sensorType : Int, sensorName : String, modifier : Modifier = Modifier ) {
    Column (
        modifier = modifier.fillMaxWidth().padding(top=20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PrintSensorData(
            sensorName,
            sensorDataStrings[sensorType],
            Modifier.padding(bottom=10.dp)
        )

        SampleSpeedControl(sensorName, onClick = {
            if (!listenerIsRunning[sensorName]?.value!!) {
                startSensor(sensorType, sliderValues[sensorName]?.value!!.roundToInt())
            } else {
                stopSensor(sensorType)
            }
        })
    }
}

@Composable
private fun LocationPanel(locProvider : String, modifier : Modifier = Modifier ) {
    Column (
        modifier = modifier.fillMaxWidth().padding(top=20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PrintSensorData(
            "Position (${locProvider.uppercase(Locale.ROOT)})",
            locationListenersAndData[locProvider]?.second,
            Modifier.padding(bottom=10.dp)
        )

        mapViews[locProvider]?.let { OsmdroidMapView(it) }

        SampleSpeedControl(locProvider,
            onClick = {
                if (!listenerIsRunning[locProvider]?.value!!) {
                    registerLocationListener(locProvider, sliderValues[locProvider]?.value!!.roundToLong())
                } else {
                    unregisterLocationListener(locProvider)
                }
            },
            modifier = Modifier.padding(top=10.dp)
        )
    }
}

@Composable
fun OsmdroidMapView(mapView : MapView) {
    val gpBochum = GeoPoint(51.4818,7.2162)
    Surface(Modifier.height(400.dp)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                mapView.setTileSource(TileSourceFactory.MAPNIK)
                mapView.setBuiltInZoomControls(true)
                mapView.setMultiTouchControls(true)
                mapView.setBackgroundColor(Color.Gray.toArgb())
                mapView
            },
            update = { view ->
                view.controller.setCenter(gpBochum)
                view.controller.setZoom(12.0)
            }
        )
    }
}
