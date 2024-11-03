package de.codlin.MissionPossible.model

data class LocationReading(
    val timestampMillis: Long,
    val lat: Double,
    val long: Double,
    val altitude: Double,
    val magnitude: Float,
)