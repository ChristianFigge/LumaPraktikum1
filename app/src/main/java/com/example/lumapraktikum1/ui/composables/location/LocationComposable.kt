package com.example.lumapraktikum1.ui.composables.location

import DataCollector
import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import android.preference.PreferenceManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val listenerIsRunning = mutableMapOf<String, MutableState<Boolean>>()

private var locationListenersAndData =
    mutableMapOf<String, Pair<LumaticLocationListener, MutableState<String>>>()
// Location Listeners & Data als dictionary mit ["provider"] = Pair<Listener, str_Data>

private val mapViews = mutableMapOf<String, MapView>()

private val sliderValues = mutableMapOf<String, MutableState<Float>>()

private val dataCollectors = mutableListOf<DataCollector>()

private lateinit var locationManager: LocationManager

private val LOCATION_PROVIDERS = listOf(
    LocationManager.GPS_PROVIDER,       // = "gps"
    LocationManager.NETWORK_PROVIDER    // = "network"
)

@SuppressLint("MissingPermission")  // permissions werden in onCreate() erteilt
private fun registerLocationListener(provider: String, minTimeMs: Long, minDistanceM: Float = 0f) {
    locationListenersAndData[provider]?.let {
        it.second.value = "Waiting 4 signal ...\n"
        locationManager.requestLocationUpdates(provider, minTimeMs, minDistanceM, it.first)
        listenerIsRunning[provider]?.value = true
    }
}

fun createListeners(ctx: Context) {
    LOCATION_PROVIDERS.forEach {
        val newDataStr = mutableStateOf("NO DATA YET\n")
        val newMapView = MapView(ctx)
        mapViews[it] = newMapView
        val newListener = LumaticLocationListener(it, newDataStr, newMapView)
        sliderValues[it] = mutableFloatStateOf(1000f)

        locationListenersAndData[it] =
            Pair<LumaticLocationListener, MutableState<String>>(newListener, newDataStr)
        dataCollectors.add(newListener)
        listenerIsRunning[it] = mutableStateOf(false)
    }
}

@Composable
fun LocationComposable(navController: NavController, ctx: Context) {
    Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
    Configuration.getInstance().userAgentValue = "MapApp"
    locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    createListeners(ctx)
    locationListenersAndData.keys.forEach { provider ->
        LocationPanel(provider,
            Modifier.drawBehind {
                drawLine(
                    Color.Gray,
                    Offset(0f, size.height),
                    Offset(size.width, size.height),
                    1.dp.toPx()
                )
            }
        )
    }


}

@Composable
private fun LocationPanel(locProvider: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        PrintSensorData(
            "Position (${locProvider.uppercase(Locale.ROOT)})",
            locationListenersAndData[locProvider]?.second,
            Modifier.padding(bottom = 10.dp)
        )

        mapViews[locProvider]?.let { OsmdroidMapView(it) }

        SampleSpeedControl(
            locProvider,
            onClick = {
                if (!listenerIsRunning[locProvider]?.value!!) {
                    registerLocationListener(
                        locProvider,
                        sliderValues[locProvider]?.value!!.roundToLong()
                    )
                } else {
                    unregisterLocationListener(locProvider)
                }
            },
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun SampleSpeedControl(
    listenerName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..5000f,
    steps: Int = 19,
) {
    val sensorIsRunning = remember { listenerIsRunning[listenerName] }
    sliderValues[listenerName]?.value?.let { sliderValue ->
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = modifier
        ) {
            Box(contentAlignment = Alignment.Center) {
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

private fun unregisterLocationListener(provider: String) {
    locationListenersAndData[provider]?.let {
        locationManager.removeUpdates(it.first)
        it.second.value = "STOPPED\n"
        listenerIsRunning[provider]?.value = false
    }
}

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
        //textAlign = TextAlign.Center,
        //modifier = modifier,
        modifier = modifier.wrapContentSize(Alignment.Center)
    )
}

@Composable
fun OsmdroidMapView(mapView: MapView) {
    val gpBochum = GeoPoint(51.4818, 7.2162)
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
