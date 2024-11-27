package com.example.lumapraktikum1.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.abs

class LumaticRoute(
    private val mapView : MapView
) {
    companion object {  // kotlin static?!
        /*** ROUTE DATA ***/
        private val routeA_points = listOf(
            GeoPoint(51.44669, 7.27072),
            GeoPoint(51.44804, 7.27376),
            GeoPoint(51.44582, 7.27243),
            GeoPoint(51.44804, 7.27149),
            GeoPoint(51.44667, 7.27428),
        )
        private val routeB_points = listOf(
            GeoPoint(51.44669, 7.27072),
            GeoPoint(51.44804, 7.27376),
            GeoPoint(51.44582, 7.27243),
        )
    }

    private var mLine : Polyline? = null
    private var mMarkers : MutableList<Marker>? = null
    enum class RouteID { A, B }

    /**
     * Polyline factory for closed routes
     */
    private fun initPolyline(points : List<GeoPoint>, color : Int) {
        mLine = Polyline() // we need no infowindow

        // create closed route line
        points.forEach { mLine!!.addPoint(it) }
        if(points.first() != points.last())
            mLine!!.addPoint(points.first())

        // styling
        mLine!!.outlinePaint.color = color
    }

    /**
     * Marker list factory
     */
    private fun initMarkerList(points: List<GeoPoint>) {
        mMarkers = mutableListOf<Marker>()
        points.forEach {
            val newMarker = Marker(mapView)
            newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            newMarker.setPosition(it)
            mMarkers!!.add(newMarker)
        }
    }

    fun drawMarkers(points: List<GeoPoint>) {
        points.forEach {
            val newMarker = Marker(mapView)
            newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            newMarker.setPosition(it)
            mapView.overlays.add(newMarker)
        }
    }

    /**
     * Draws polyline with(out) waypoint map markers for route A.
     */
    fun drawRoute(routeId : RouteID, drawMarkers : Boolean = true, color : Int = Color.Blue.toArgb()) {
        clearRoute()

        val points = when(routeId) {
            RouteID.A -> routeA_points
            RouteID.B -> routeB_points
        }

        // polyline
        initPolyline(points, color)
        mapView.overlays.add(mLine)

        // markers
        if(drawMarkers) {
            initMarkerList(points)
            mapView.overlays.addAll(mMarkers!!)
        }

        mapView.controller.setCenter(points.first())
        mapView.controller.setZoom(20.0)
        mapView.invalidate() // force map refresh
    }

    fun clearRoute() {
        mapView.overlays.remove(mLine)
        mMarkers?.let { mapView.overlays.removeAll(it) }
        mapView.invalidate()
    }

    fun getRoutePoints(routeId : RouteID) : MutableList<GeoPoint> {
        return when(routeId) {
            RouteID.A -> routeA_points.toMutableList()
            RouteID.B -> routeB_points.toMutableList()
        }
    }

    /**
     * Interpolates on a straight line between 2 given GeoPoints a, b.
     * The number of in-between points is floor(abs(bTime - aTime) / step) - 1.
     * Returns an ordered list containing a, [interpolated points], b.
     */
    fun interpolate(
        a: GeoPoint,
        aTimeMillis: Long,
        b : GeoPoint,
        bTimeMillis: Long,
        stepMillis: Long = 1000L
    )
        : MutableList<GeoPoint>
    {
        val nSteps = abs(bTimeMillis - aTimeMillis) / stepMillis // implicit floor
        val latStep = abs(b.latitude - a.latitude) / nSteps
        val longStep = abs(b.longitude - a. longitude) / nSteps

        val pointList = mutableListOf<GeoPoint>()
        for (stepCount in 0..nSteps) {
            pointList.add(
                GeoPoint(a.latitude + latStep * stepCount, a.longitude + longStep * stepCount))
        }
        return pointList
    }

    /**
     * Distance in meters between 2 GeoPoints. Same as a.distanceToAsDouble(b)
     */
    fun getDistance(a : GeoPoint, b : GeoPoint) : Double {
        return a.distanceToAsDouble(b)
    }
}