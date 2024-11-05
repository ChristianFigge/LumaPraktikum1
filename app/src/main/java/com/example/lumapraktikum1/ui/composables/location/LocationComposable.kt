package com.example.lumapraktikum1.ui.composables.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.preference.PreferenceManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import com.example.lumapraktikum1.model.LocationReading
import com.example.lumapraktikum1.ui.composables.system.LifeCycleHookWrapper
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@SuppressLint("MissingPermission")
@Composable
fun LocationComposable(navController: NavHostController, ctx: Context) {

    var locationManager by remember { mutableStateOf<LocationManager?>(null) }
    var locationListener by remember { mutableStateOf<LocationListener?>(null) }
    var firstFix by remember { mutableStateOf(true) }

    var singleCurrentLocation by remember {
        mutableStateOf<LocationReading>(
            LocationReading(
                timestampMillis = System.currentTimeMillis(),
                long = 0.0,
                lat = 0.0,
                altitude = 0.0
            )
        )
    }

    var allCurrentReadings by remember {
        mutableStateOf<List<LocationReading>>(listOf())
    }

    var sampleRateMs by remember { mutableIntStateOf(0) }
    var meterSelection by remember { mutableIntStateOf(1) }
    var isRecording by remember { mutableStateOf(false) }
    var provider by remember { mutableStateOf(LocationManager.GPS_PROVIDER) }


    val mapCenter = GeoPoint(51.4818, 7.2162)
    Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
    Configuration.getInstance().userAgentValue = "MapApp"

    LifeCycleHookWrapper(
        attachToDipose = {},
        onEvent = { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                locationListener?.let { locationManager?.removeUpdates(it) }
            } else if (event == Lifecycle.Event.ON_CREATE) {
                locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                locationListener = LocationListener { p0 ->
                    singleCurrentLocation = LocationReading(
                        timestampMillis = System.currentTimeMillis(),
                        long = p0.longitude,
                        lat = p0.latitude,
                        altitude = 0.0 //values[2]
                    )

                    if (isRecording) {
                        allCurrentReadings += singleCurrentLocation
                    }
                    if (firstFix) {
                        GeoPoint(singleCurrentLocation.lat, singleCurrentLocation.long)
                        firstFix = false
                    }
                }

                locationListener?.let {
                    locationManager!!.requestLocationUpdates(
                        provider,
                        sampleRateMs.toLong(),
                        meterSelection.toFloat(),
                        it
                    )
                }

            }
        }
    )

    DisposableEffect(key1 = sampleRateMs, key2 = meterSelection, key3 = provider) {
        locationListener?.let { locationManager?.removeUpdates(it) }
        locationListener?.let {
            locationManager!!.requestLocationUpdates(
                provider,
                sampleRateMs.toLong(),
                meterSelection.toFloat(),
                it
            )
        }

        onDispose { }
    }

    Surface(Modifier.height(500.dp)) {
        Column {
            Button(onClick = { isRecording = !isRecording }) { Text("Start Recording") }
            AndroidView(
                modifier = Modifier,
                factory = { context ->
                    val mapView = MapView(context)
                    mapView.setTileSource(TileSourceFactory.MAPNIK)
                    //mapView.setBuiltInZoomControls(true)
                    mapView.setMultiTouchControls(true)
                    mapView.setBackgroundColor(Color.Gray.toArgb())
                    mapView
                },
                update = { view ->
                    // Code to update or recompose the view goes here
                    // Since geoPoint is read here, the view will recompose whenever it is updated
                    view.controller.setCenter(mapCenter)
                    view.controller.setZoom(12.0)
                    allCurrentReadings.map {
                        val testMarker = Marker(view)
                        testMarker.setPosition(mapCenter)
                        //testMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        view.overlays.add(Marker(view))
                        //view.getOverlays().add()
                    }
                    view.invalidate()
                }
            )
        }
    }
}