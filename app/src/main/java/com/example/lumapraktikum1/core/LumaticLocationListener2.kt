package com.example.lumapraktikum1.core

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationListener
import android.location.LocationManager
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import com.example.lumapraktikum1.model.LocationReading
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView

/*
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
    private var providerType : PROVIDER, /** 0: FusedLoc HIGH, 1: FusedLoc BALANCED, 2: LocationManager GPS */
    private var intervalMillis : Long,
    ctx: Context,
) {
    enum class PROVIDER { FUSED_HIGH, FUSED_BALANCED, GPS }

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
    private var fusedLocRequest_HIGH = buildLocationRequest(Priority.PRIORITY_HIGH_ACCURACY)
    private var fusedLocRequest_BALANCED = buildLocationRequest(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
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
    private var isRecording = mutableStateOf(false)
    var reachedWaypoint = false


    private fun handleLocationReading(loc : LocationReading) {
        latestLocation.value = loc //.copy()

        allReadings += latestLocation.value!!
        Log.i("Location", "Location Reading #${allReadings.size} added")

        if(reachedWaypoint) {
            waypointReadings += latestLocation.value!!
            reachedWaypoint = false
            Log.i("Location", "Waypoint Reading #${waypointReadings.size} added")
        }
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        when(providerType) {
            PROVIDER.GPS ->  {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    intervalMillis,
                    1f,
                    locationListener
                )
            }
            PROVIDER.FUSED_HIGH -> {
                fusedLocationProviderClient.requestLocationUpdates(
                    fusedLocRequest_HIGH,
                    fusedLocCallback,
                    null
                )
            }
            PROVIDER.FUSED_BALANCED -> {
                fusedLocationProviderClient.requestLocationUpdates(
                    fusedLocRequest_BALANCED,
                    fusedLocCallback,
                    null
                )
            }
        }
        isRecording.value = true
        Log.i("LocReading", "START location reading & recording (${getProviderInfoString()})")
    }

    fun stopRecording() {
        when(providerType) { // switch vllt überflüssig
            PROVIDER.GPS ->  {
                locationManager.removeUpdates(locationListener)
            }
            PROVIDER.FUSED_HIGH -> {
                fusedLocationProviderClient.removeLocationUpdates(fusedLocCallback)
            }
            PROVIDER.FUSED_BALANCED -> {
                fusedLocationProviderClient.removeLocationUpdates(fusedLocCallback)
            }
        }
        isRecording.value = false
        latestLocation.value = null
        Log.i("LocReading", "STOP location reading & recording (${getProviderInfoString()})")
    }

    /*** GETTERS ***/
    fun getProviderInfoString() : String {
        return when(providerType) {
            PROVIDER.GPS -> "Android LocationService GPS"
            PROVIDER.FUSED_HIGH -> "Google FusedLocationProvider HIGH"
            PROVIDER.FUSED_BALANCED -> "Google FusedLocationProvider BALANCED"
        }
    }

    fun getLatestLocation(): LocationReading? {
        return this.latestLocation.value
    }

    fun isRecording() : Boolean {
        return this.isRecording.value
    }

    /*** SETTERS ***/
    fun setIntervalMillis(intervalMillis : Long) {
        this.intervalMillis = intervalMillis
        // Rebuild LocationRequests
        fusedLocRequest_HIGH = buildLocationRequest(Priority.PRIORITY_HIGH_ACCURACY)
        fusedLocRequest_BALANCED = buildLocationRequest(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
    }

    fun setProviderType(provider : PROVIDER) {
        if(!this.isRecording())
            this.providerType = provider
        // else kaputt -> ProviderType toggle während recording disablen
    }

    /*** UTILS ***/
    private fun buildLocationRequest(priority : Int): LocationRequest {
        return LocationRequest
            .Builder(priority, intervalMillis)
            .setMinUpdateDistanceMeters(1f) // weniger nervt beim testen
            .build()
    }
}