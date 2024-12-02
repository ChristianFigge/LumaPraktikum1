package com.example.lumapraktikum1.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.example.lumapraktikum1.model.LocationReading
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/**
 * Notizen
 * LocationManager GPS braucht:
 * - LocationManager über getSystemService(ctx)
 * - LocationListener impl onLocationChanged(..)
 *
 * Die 2 FusedLocs brauchen:
 * - 2 LocationRequests (jeweils eine für jede PRIO)
 * - LocationCallback impl onLocationResult(..)
 * - FusedLocationProviderClient über google LocationServices.getBla(ctx)
 *
 * Alle brauchen:
 * - fn startRecording
 * - fn stopRecording
 * - member Liste allReadings
 * - member Liste waypointReadings
 * - member currentLocation
 * - member mapView
 * - parameter Interval in ms
 */

class LumaticLocationListener2 (
    var providerType : PROVIDER, // 0: LocationProvider GPS, 1: FusedLoc HIGH PRIO, 2: FusedLoc BALANCED
    var intervalMillis : Long,
    val mapView : MapView,
    ctx: Context,
) {
    enum class PROVIDER { GPS, FUSED_HIGH, FUSED_BALANCED }

    /*** Members for Android LocationService (GPS) ***/
    private val locationManager = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val locationListener = LocationListener { p0 ->
        handleLocationReading(LocationReading(
            System.currentTimeMillis(),
            p0.latitude,
            p0.longitude,
            0.0)
        )
    }

    /*** Members for Google FusedLocationProvider (HIGH/BALANCED) ***/
    private val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(ctx)
    private val fusedLocRequest_HIGH = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis).build()
    private val fusedLocRequest_BALANCED = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMillis).build()
    private val fusedLocCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            super.onLocationResult(p0)
            if(p0.lastLocation != null) {
                handleLocationReading(LocationReading(
                    System.currentTimeMillis(),
                    p0.lastLocation!!.latitude,
                    p0.lastLocation!!.longitude,
                    0.0
                ))
            }
        }
    }

    /*** Members for Data Collection ***/
    private var latestLocation = mutableStateOf<LocationReading?>(null)
    private val allReadings = mutableListOf<LocationReading>()
    private val waypointReadings = mutableListOf<LocationReading>()
    var reachedWaypoint = false

    // die 2 bools könnten auch komplett ins view, ohne firstFix entfällt dann der this.mapView member
    var firstFix = true
    private var isRecording = mutableStateOf(false)


    private fun handleLocationReading(loc : LocationReading) {
        latestLocation.value = loc //.copy()

        allReadings += latestLocation.value!!
        Log.i("Location", "Location Reading added")

        if(reachedWaypoint) {
            waypointReadings += latestLocation.value!!
            reachedWaypoint = false
            Log.i("Location", "Waypoint Reading added")
        }

        if (firstFix) {
            mapView.controller.setCenter(GeoPoint(loc.lat, loc.long))
            mapView.controller.setZoom(19.0)
            firstFix = false
        }
    }

    fun getProviderInfoString() : String {
        return when(providerType) {
            PROVIDER.GPS -> "Android LocationService GPS"
            PROVIDER.FUSED_HIGH -> "Google FusedLocationProvider HIGH_PRIORITY"
            PROVIDER.FUSED_BALANCED -> "Google FusedLocationProvider BALANCED"
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        var providerInfo = getProviderInfoString()
        when(providerType) {

            PROVIDER.GPS ->  {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    intervalMillis,
                    1f,
                    locationListener
                )
                isRecording.value = true
                firstFix = true
            }

            PROVIDER.FUSED_HIGH -> {
                // TODO
            }

            PROVIDER.FUSED_BALANCED -> {
                // TODO
            }
        }
        Log.i("LocReading", "START location reading & recording ($providerInfo)")
    }

    fun stopRecording() {
        var providerInfo = getProviderInfoString()
        when(providerType) {

            PROVIDER.GPS ->  {
                locationManager.removeUpdates(locationListener)
                isRecording.value = false
            }

            PROVIDER.FUSED_HIGH -> {
                // TODO
            }

            PROVIDER.FUSED_BALANCED -> {
                // TODO
            }
        }
        Log.i("LocReading", "STOP Location reading & recording ($providerInfo)")
    }

    fun getLatestLocation(): LocationReading? {
        return this.latestLocation.value
    }

    fun isRecording() : Boolean {
        return this.isRecording.value
    }
}