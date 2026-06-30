package com.orbis.orbis.helpers

import android.util.DisplayMetrics
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.SphericalUtil
import com.orbis.orbis.models.Coordinates
import com.orbis.orbis.models.entities.Point
import com.orbis.orbis.models.place.PolygonPlaceDetails
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt


object CoordinatesUtil {
    val earth_radius = 6378.137
    val chaikinPartitions = 4
    val polygonLogoPadding = 0.75
    val polygonRadius = HashMap<PolygonPlaceDetails, Double>()

    fun getZoomLevel(radius: Double, center: LatLng, metrics: DisplayMetrics): Float {
        val padding = 1.28 // 28%

        // Screen width in dp (density-independent pixels)
        val screenWidthInDp = metrics.widthPixels / metrics.density

        // Meters per pixel at the equator for zoom level 0
        val equatorMetersPerPixelAtZoom0 = 156543.03392
        // Meters per pixel at the given latitude for zoom level 0
        val metersPerPixelAtZoom0 = equatorMetersPerPixelAtZoom0 * cos(Math.toRadians(center.latitude))

        // Expansion is the diameter (2 * radius) with padding applied
        val expansion = 2 * radius * padding

        // Required meters per pixel to fit the expanded circle into the screen width
        val requiredMetersPerPixel = expansion / screenWidthInDp

        // Zoom level calculation using log2 to scale the zoom based on pixel density
        val zoomLevel = log2(metersPerPixelAtZoom0 / requiredMetersPerPixel).toFloat()

        // Cap zoom level to standard max and min values for maps
        val maxZoom = 21.0f
        val minZoom = 1.0f
        val maxZoomLevel = zoomLevel.coerceIn(minZoom, maxZoom)

        return maxZoomLevel
    }

    fun pixelsToDegrees(pixels: Int, zoom: Float, center: LatLng): Double {
        val metersPerPixel =
            156543.03392 * cos(Math.toRadians(0.0)) / 2.0.pow(zoom.toDouble())
        //deviation by equator
        val equatorMetersPerPixel = metersPerPixel * cos(Math.toRadians(center.latitude))

        // Convert meters to degrees (latitude)
        val degreesPerPixel = equatorMetersPerPixel / 111320.0
        return pixels * degreesPerPixel
    }

    fun bspline(poly: MutableList<LatLng>): List<LatLng> {
        if (poly[0].latitude != poly[poly.size - 1].latitude || poly[0].longitude != poly[poly.size - 1].longitude) {
            poly.add(LatLng(poly[0].latitude, poly[0].longitude))
        } else {
            poly.removeAt(poly.size - 1)
        }
        poly.add(0, LatLng(poly[poly.size - 1].latitude, poly[poly.size - 1].longitude))
        poly.add(LatLng(poly[1].latitude, poly[1].longitude))
        val lats = arrayOfNulls<Double>(poly.size)
        val lons = arrayOfNulls<Double>(poly.size)
        for (i in poly.indices) {
            lats[i] = poly[i].latitude
            lons[i] = poly[i].longitude
        }
        var ax: Double
        var ay: Double
        var bx: Double
        var by: Double
        var cx: Double
        var cy: Double
        var dx: Double
        var dy: Double
        var lat: Double
        var lon: Double
        var t: Float
        val points: MutableList<LatLng> = ArrayList()
        // For every point
        var i: Int = 2
        while (i < lats.size - 2) {
            // We do B-Spline interpolation https://en.wikipedia.org/wiki/B-spline
            t = 0f
            while (t < 1) {
                ax = (-lats[i - 2]!! + 3 * lats[i - 1]!! - 3 * lats[i]!! + lats[i + 1]!!) / 6
                ay = (-lons[i - 2]!! + 3 * lons[i - 1]!! - 3 * lons[i]!! + lons[i + 1]!!) / 6
                bx = (lats[i - 2]!! - 2 * lats[i - 1]!! + lats[i]!!) / 2
                by = (lons[i - 2]!! - 2 * lons[i - 1]!! + lons[i]!!) / 2
                cx = (-lats[i - 2]!! + lats[i]!!) / 2
                cy = (-lons[i - 2]!! + lons[i]!!) / 2
                dx = (lats[i - 2]!! + 4 * lats[i - 1]!! + lats[i]!!) / 6
                dy = (lons[i - 2]!! + 4 * lons[i - 1]!! + lons[i]!!) / 6
                lat =
                    ax * (t + 0.1).pow(3.0) + bx * (t + 0.1).pow(2.0) + cx * (t + 0.1) + dx
                lon =
                    ay * (t + 0.1).pow(3.0) + by * (t + 0.1).pow(2.0) + cy * (t + 0.1) + dy
                points.add(LatLng(lat, lon))
                t += 0.2.toFloat()
            }
            i++
        }
        return points
    }

    fun chaikin(arr: List<LatLng>, num: Int): List<LatLng> {
        if (num == 0) return arr
        val reduced : ArrayList<LatLng> = ArrayList()

        var i = 0
        while (i < arr.size){
            reduced.add(arr[i])
            i += chaikinPartitions
        }
        val l = reduced.size
        val smooth = reduced.flatMapIndexed { i, c ->
            val next = reduced[(i + 1) % l]

            listOf(
                LatLng(0.75 * c.latitude + 0.25 * next.latitude, 0.75 * c.longitude + 0.25 * next.longitude),
                LatLng(0.25 * c.latitude + 0.75 * next.latitude, 0.25 * c.longitude + 0.75 * next.longitude)
            )
        }

        return if (num == 1) smooth else chaikin(smooth, num - 1)
    }
    /**
     * Calculates the minimum enclosing circle of the given points
     *
     * @param polygonPoints List of LatLng representing the polygon points.
     * @return The pair of center and radius of the circle.
     */
    fun minimumEnclosingCircle(polygonPoints: List<LatLng>): Pair<LatLng, Double> {
        val centroid = calculateCentroid(polygonPoints)
        val maxDistance = calculateDistanceToFarthestPoint(centroid, polygonPoints)
        return Pair(centroid, maxDistance)
    }
    fun calculateCentroid(points: List<LatLng>): LatLng {
        var centerLatLng: LatLng? = null
        val builder = LatLngBounds.Builder()
        for (i in points.indices) {
            builder.include(points[i])
        }
        val bounds: LatLngBounds = builder.build()
        centerLatLng = bounds.center

        return centerLatLng
    }
    fun calculateDistanceToFarthestPoint(centroid: LatLng, points: List<LatLng>): Double {
        var maxDistance = 0.0
        for (point in points) {
            val distance = SphericalUtil.computeDistanceBetween(centroid, point)
            if (distance > maxDistance) {
                maxDistance = distance
            }
        }
        return maxDistance
    }
    fun computeDistance(p1: LatLng, p2: LatLng): Double{
        return SphericalUtil.computeDistanceBetween(p1, p2)
    }
    /**
     * Calculates the distance between a center point and the closest point of a polygon.
     *
     * @param polygonPoints List of LatLng representing the polygon points.
     * @param center LatLng representing the center point.
     * @return The shortest distance between the center point and the closest point of the polygon.
     */
    fun calculateShortestDistance(polygonPoints: List<LatLng>, center: LatLng): Double {
        var minDistance = Double.MAX_VALUE

        var i = 0
        while (i < polygonPoints.size) {
            val start = polygonPoints[i]
            val end = polygonPoints[(i + 1) % polygonPoints.size]

            // Calculate distance from the center to the current edge of the polygon
            val distance = SphericalUtil.computeDistanceBetween(center, getClosestPointOnSegment(start, end, center))

            if (distance < minDistance) {
                minDistance = distance
            }
            i += 10
        }

        return minDistance * polygonLogoPadding
    }

    /**
     * Finds the closest point on a segment to a given point.
     *
     * @param start LatLng representing the start of the segment.
     * @param end LatLng representing the end of the segment.
     * @param point LatLng representing the point to find the closest point to.
     * @return LatLng representing the closest point on the segment to the given point.
     */
    fun getClosestPointOnSegment(start: LatLng, end: LatLng, point: LatLng): LatLng {
        val latDiff = end.latitude - start.latitude
        val lngDiff = end.longitude - start.longitude

        if (latDiff == 0.0 && lngDiff == 0.0) {
            return start
        }

        val t = ((point.latitude - start.latitude) * latDiff + (point.longitude - start.longitude) * lngDiff) / (latDiff * latDiff + lngDiff * lngDiff)

        return when {
            t < 0 -> start
            t > 1 -> end
            else -> LatLng(start.latitude + t * latDiff, start.longitude + t * lngDiff)
        }
    }
    fun calculateCircleRadius(polygonPoints: ArrayList<Coordinates>): Double {
        val start = LatLng(polygonPoints[0].latitude, polygonPoints[0].longitude)
        val half = polygonPoints.size / 2
        val end = LatLng(polygonPoints[half].latitude, polygonPoints[half].longitude)
        return SphericalUtil.computeDistanceBetween(start, end) / 2
    }
    fun getCircleOuterPoints(center: Coordinates, radius: Double, numPoints: Int = 100): ArrayList<Coordinates> {
        val points = ArrayList<Coordinates>()
        val angleStep = 360.0 / numPoints
        val center = LatLng(center.latitude, center.longitude)

        for (i in 0 until numPoints) {
            val angle = angleStep * i
            val point = SphericalUtil.computeOffset(center, radius, angle)
            points.add(Coordinates(point.longitude, point.latitude))
        }

        return points
    }
    fun calculatePolygonRadius(place: PolygonPlaceDetails): Double {
        polygonRadius[place]?.let {
            return it
        }

        val coord = place.polygonPoints
        val points = coordinatesToLatLng(coord)
        var maxDistance = 0.0
        var i = 0
        while (i < points.size) {
            for (j in i + 1 until points.size) {
                val distance = SphericalUtil.computeDistanceBetween(points[i], points[j])
                if (distance > maxDistance) {
                    maxDistance = distance
                }
            }
            i += 25
        }
        polygonRadius[place] = maxDistance / 2
        return polygonRadius[place]!!
    }

    fun coordinatesToLatLng(line: ArrayList<Coordinates>): ArrayList<LatLng> {
        return line.map { LatLng(it.latitude, it.longitude) } as ArrayList<LatLng>
    }

    fun getNewLocation(
        coordinates: Coordinates,
        positionType: PositionType,
        distance: Double
    ): Coordinates {
        when (positionType) {
            //north
            PositionType.TOPCENTER -> {
                return Coordinates(
                    coordinates.longitude,
                    getNewLatitude(
                        coordinates.latitude,
                        distance
                    )
                )
            }
            //south
            PositionType.BOTTOMCENTER -> {
                return Coordinates(
                    coordinates.longitude,
                    getNewLatitude(
                        coordinates.latitude,
                        distance * -1
                    )
                )
            }

            //west
            PositionType.LEFTCENTER -> {
                return Coordinates(
                    getNewLongitude(
                        coordinates.longitude, coordinates.latitude,
                        distance * -1
                    ), coordinates.latitude
                )
            }

            //east
            PositionType.RIGHTCENTER -> {
                return Coordinates(
                    getNewLongitude(
                        coordinates.longitude, coordinates.latitude,
                        distance
                    ), coordinates.latitude
                )
            }

            //top west north corner
            PositionType.TOPLEFT -> {
                // distance/2 => 20.0/2 = 10.0 km
                val top_center = Coordinates(
                    coordinates.longitude,
                    getNewLatitude(
                        coordinates.latitude,
                        distance
                    )
                )
                return Coordinates(
                    getNewLongitude(
                        top_center.longitude, top_center.latitude,
                        distance * -1
                    ), top_center.latitude
                )
            }

            //top east north corner
            PositionType.BOTTOMRIGHT -> {
                val bottom_center = Coordinates(
                    coordinates.longitude,
                    getNewLatitude(
                        coordinates.latitude,
                        distance * -1
                    )
                )
                return Coordinates(
                    getNewLongitude(
                        bottom_center.longitude, bottom_center.latitude,
                        distance
                    ), bottom_center.latitude
                )
            }

        }
    }
    fun bearing(from: LatLng, to: LatLng): Float {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val dLng = Math.toRadians(to.longitude - from.longitude)
        val x = Math.sin(dLng) * Math.cos(lat2)
        val y = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLng)
        return Math.toDegrees(Math.atan2(x, y)).toFloat()
    }
    private fun getNewLatitude(latitude: Double, distance: Double): Double {
        return latitude + (distance / earth_radius) * (180 / Math.PI)
    }

    private fun getNewLongitude(longitude: Double, latitude: Double, distance: Double): Double {
        return longitude + (distance / earth_radius) * (180 / Math.PI) / cos(latitude * Math.PI / 180)
    }
}