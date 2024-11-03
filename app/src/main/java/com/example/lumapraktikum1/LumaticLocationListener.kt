package com.example.lumapraktikum1

import android.location.Location
import android.location.LocationListener
import androidx.compose.runtime.MutableState
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class LumaticLocationListener(
    override val name : String,
    val dataString : MutableState<String>,
    val mapView : MapView
) :
    DataCollector(name),
    LocationListener
{
    private val mapMarker : Marker = Marker(mapView)
    init {
        mapMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
    }

    override fun onLocationChanged(location: Location) {
        dataString.value =
            "Lat: ${location.latitude}\nLong: ${location.longitude}"

        super.collectDatum(location)

        mapMarker.setPosition(GeoPoint(location.latitude, location.longitude))
        mapView.overlays.add(mapMarker)
    }

    fun removeMarker() {
        mapView.overlays.remove(mapMarker)
    }
}