package com.example.lumapraktikum1.core

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.example.lumapraktikum1.model.LocationReading
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import kotlin.math.abs
import kotlin.math.roundToInt

class LumaticRoute(
    private val mapView : MapView
) {
    companion object {  // kotlin static?!
        /*** ROUTE DATA ***/
        // TODO in prakt2 git: Read model, RoutePointModel -> GeoList conv
        private val routePoints_Julian = listOf(
            GeoPoint(51.4926391, 7.4150329),
            GeoPoint(51.4927257, 7.4149413),
            GeoPoint(51.4926435, 7.4143429),
            //GeoPoint(51.4926391, 7.4150329), // doppelt?
            GeoPoint(51.4923991, 7.4143392),
            GeoPoint(51.492482, 7.415013),
            GeoPoint(51.4926391, 7.4150329),
            //GeoPoint(51.4926391, 7.4150329), // doppelt?
        )
        // Placeholder! TODO implement maltes route
        private val routePoints_Malte = listOf(
            GeoPoint(51.44669, 7.27072),
            GeoPoint(51.44804, 7.27376),
            GeoPoint(51.44582, 7.27243),
            GeoPoint(51.44804, 7.27149),
            GeoPoint(51.44667, 7.27428),
        )
    }

    private var mLine : Polyline? = null
    private var mMarkers : MutableList<Marker>? = null
    enum class RouteID { JULIAN, MALTE }

    /**
     * Polyline factory for displaying routes
     */
    private fun initPolyline(points : List<GeoPoint>, color : Int) {
        mLine = Polyline() // we need no infowindow

        // create polyline
        points.forEach { mLine!!.addPoint(it) }

        // styling
        mLine!!.outlinePaint.color = color
    }

    /**
     * Marker list factory
     */
    private fun initMarkerList(points: List<GeoPoint>) {
        mMarkers = mutableListOf<Marker>()
        points.forEachIndexed { idx, p ->
            // make sure that there's only one marker on start/finish point
            // for closed loop routes
            if(idx+1 == points.size && p == points.first()) {
                mMarkers!!.firstOrNull()?.let {
                    it.title += " / #${idx+1}"
                }
            }
            else {
                val newMarker = Marker(mapView)
                newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                newMarker.setPosition(p)
                newMarker.title = "Route point #${idx+1}"
                newMarker.subDescription = ""
                newMarker.snippet = ""
                mMarkers!!.add(newMarker)
            }
        }
    }

    /**
     * Help function for testing: Draws Markers from GeoPoint List on the map.
     */
    fun drawMarkers(points: List<GeoPoint>) {
        points.forEach {
            val newMarker = Marker(mapView)
            newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            newMarker.setPosition(it)
            mapView.overlays.add(newMarker)
        }
        mapView.invalidate()
    }

    /**
     * Draws polyline with(out) waypoint map markers for route A.
     */
    fun drawRoute(routeId : RouteID, drawMarkers : Boolean = true, color : Int = Color.Blue.toArgb()) {
        clearRoute()

        val points = when(routeId) {
            RouteID.JULIAN -> routePoints_Julian
            RouteID.MALTE -> routePoints_Malte
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

    /**
     * Remove the currently drawn route from the map, if any.
     */
    fun clearRoute() {
        mapView.overlays.remove(mLine)
        mMarkers?.let { mapView.overlays.removeAll(it) }
        mapView.invalidate()
    }

    fun getRoutePoints(routeId : RouteID) : MutableList<GeoPoint> {
        return when(routeId) {
            RouteID.JULIAN -> routePoints_Julian.toMutableList()
            RouteID.MALTE -> routePoints_Malte.toMutableList()
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

        val pointList = mutableListOf<GeoPoint>(a)
        for (stepCount in 1..<nSteps) {
            pointList.add(
                GeoPoint(a.latitude + latStep * stepCount, a.longitude + longStep * stepCount))
        }
        pointList.add(b)
        return pointList
    }

    /**
     * Interpolates on a straight line between 2 given LocationReadings a, b.
     * The number of in-between points is floor(abs(bTime - aTime) / step) - 1.
     * Returns a list<LocationReading> containing a, [interpolated points], b.
     */
    fun interpolate(
        a : LocationReading,
        b: LocationReading,
        stepMillis: Long = 1000L
    )
        : MutableList<LocationReading>
    {
        val timeDiff = abs(b.timestampMillis - a.timestampMillis)
        val nSteps = timeDiff / stepMillis // implicit floor

        val latStep = abs(b.lat - a.lat) / nSteps
        val longStep = abs(b.long - a. long) / nSteps
        val timeStep = timeDiff / nSteps

        val pointList = mutableListOf<LocationReading>(a)
        for (stepCount in 1..<nSteps) {
            pointList.add(
                LocationReading(
                    a.timestampMillis + timeStep * stepCount,
                    a.lat + latStep * stepCount,
                    a.long + longStep * stepCount,
                    0.0
                )
            )
        }
        pointList.add(b)
        return pointList
    }

    /**
     * Returns distance between 2 LocationReadings in meters.
     */
    fun getDistance(a : LocationReading, b : LocationReading) : Double {
        val gpA = GeoPoint(a.lat, a.long)
        val gpB = GeoPoint(b.lat, b.long)
        return gpA.distanceToAsDouble(gpB)
    }

    /**
     * Returns CDF plot points for a given list of distances.
     * Single Points are stored in Pair<X,Y> with X = Distance rounded to 0.1
     * and Y = Probability from 0.0 to 1.0 for any distance d <= X
     */
    fun getCDFPlotPoints(distances : List<Double>) : List< Pair<Double,Double> > {
        // Sort distances ascending & round to 0.1 meters
        val sortedDistances = distances.sorted().map { (it * 10.0).roundToInt() / 10.0 }

        val cdfData = mutableListOf< Pair<Double,Double> >()
        sortedDistances.forEachIndexed { idx, d ->
            // Create "frequency bin" on the fly using index count
            sortedDistances.getOrNull(idx + 1)?.let { next ->
                if(d == next) return@forEachIndexed // continue
            }

            // Get the probability for any distance being <= d based on the sample size
            val cumProbability = (idx + 1).toDouble() / distances.size
            cdfData.add( Pair(d, cumProbability) )
        }
        return cdfData.toList()
    }
}