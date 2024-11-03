package com.example.lumapraktikum1.ui.composables

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

@Composable
fun LocationComposable(navController: NavHostController) {
    val gpBochum = GeoPoint(51.4818,7.2162)
    Surface(Modifier.height(500.dp)) {
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
                view.controller.setCenter(gpBochum)
                view.controller.setZoom(12.0)
            }
        )
    }
}