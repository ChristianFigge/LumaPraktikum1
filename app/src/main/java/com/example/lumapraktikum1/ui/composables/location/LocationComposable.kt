package com.example.lumapraktikum1.ui.composables.location

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavHostController
import com.example.lumapraktikum1.core.LumaticLocationListener2
import com.example.lumapraktikum1.core.LumaticRoute
import com.example.lumapraktikum1.ui.composables.system.LifeCycleHookWrapper
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline

@SuppressLint("MissingPermission")
@Composable
fun LocationComposable(navController: NavHostController, ctx: Context) {

    var sampleRateMs by remember { mutableIntStateOf(0) }
    var meterSelection by remember { mutableIntStateOf(1) }

    /*** MapView Init ***/
    val mapCenter = GeoPoint(51.4818, 7.2162)
    Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx))
    Configuration.getInstance().userAgentValue = "MapApp"
    val mapView by remember { mutableStateOf(MapView(ctx))}
    val lumaRoutes by remember { mutableStateOf(LumaticRoute(mapView)) }
    val walkPath by remember { mutableStateOf(Polyline())}
    walkPath.outlinePaint.color = Color.Red.toArgb()

    // TEST LumaticLocationListener2:
    var lumaLocListener by remember { mutableStateOf<LumaticLocationListener2?>(null) }

    fun startRecording() {
        lumaLocListener?.startRecording()

        // (re-)init walkPath polyline & make sure it's on the current top
        walkPath.actualPoints.clear()
        mapView.overlays.remove(walkPath)
        mapView.overlays.add(walkPath)
        mapView.invalidate()
    }

    fun stopRecording() {
        lumaLocListener?.stopRecording()
    }

    LifeCycleHookWrapper(
        attachToDipose = {},
        onEvent = { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                stopRecording()
            } else if (event == Lifecycle.Event.ON_CREATE) {
                lumaLocListener = LumaticLocationListener2(
                    LumaticLocationListener2.PROVIDER.GPS,
                    sampleRateMs.toLong(),
                    mapView,
                    ctx
                )
            }
        }
    )

    DisposableEffect(key1 = sampleRateMs, key2 = meterSelection) {
        /* Warum sollte der listener laufen wenn man den Knopf noch nicht gedrückt hat?
        locationListener?.let { locationManager?.removeUpdates(it) }
        locationListener?.let {
            locationManager!!.requestLocationUpdates(
                provider,
                sampleRateMs.toLong(),
                meterSelection.toFloat(),
                it
            )
        }
        */
        onDispose { stopRecording() }
    }

    Surface(Modifier.fillMaxHeight()) {
        Column (
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(20.dp))
            Surface(Modifier.height(500.dp)) {
                AndroidView(
                    modifier = Modifier,
                    factory = { _ ->
                        mapView.setTileSource(TileSourceFactory.MAPNIK)
                        //mapView.setBuiltInZoomControls(true)
                        mapView.setMultiTouchControls(true)
                        mapView.setBackgroundColor(Color.Gray.toArgb())
                        mapView.controller.setCenter(mapCenter)
                        mapView.controller.setZoom(12.0)
                        mapView
                    },
                    update = { view ->

                        lumaLocListener?.getLatestLocation()?.let {
                            val newPoint = GeoPoint(it.lat, it.long)
                            walkPath.addPoint(newPoint)

                            /* // add walk path Marker (too much clutter imo)
                            val newMarker = Marker(view)
                            newMarker.setPosition(newPoint)
                            view.overlays.add(newMarker)
                            */
                        }
                        view.invalidate()
                        Log.i("MapViewInfo", "MapView updated")
                    }
                )
            }

            Spacer(Modifier.height(20.dp))

            Button(onClick = {
                if(lumaLocListener?.isRecording() == true) stopRecording()
                else startRecording()
            }) {
                if(lumaLocListener?.isRecording() == true) Text("Stop Recording")
                else Text("Start Recording")
            }

            Spacer(Modifier.height(20.dp))

            // Route Buttons
            Row() {
                Button(
                    onClick = { lumaRoutes.drawRoute(LumaticRoute.RouteID.JULIAN) },
                    content = { Text("Show Route JULIAN") })
            }

            Spacer(Modifier.height(20.dp))

            // Button speichert den nächsten fix in der waypointReading list
            Button(
                onClick = { lumaLocListener?.reachedWaypoint = true; },
                content = { Text("Wegpunkt erreicht") }
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val routeA = lumaRoutes.getRoutePoints((LumaticRoute.RouteID.JULIAN))
                    val points = lumaRoutes.interpolate(routeA[0], 0, routeA[1], 29300)
                    lumaRoutes.drawMarkers(points)

                    /* CDF TEST
                    val samples = listOf(2.4152, 2.3925, 0.512, 1.277, 2.999, 0.8912, 0.0)
                    val cdfPoints = lumaRoutes.getCDFPlotPoints(samples);
                    Log.i("CDF TEST", "$cdfPoints");
                     */
                },
                content = { Text("Interpolation Test A") }
            )
        }
    }
}