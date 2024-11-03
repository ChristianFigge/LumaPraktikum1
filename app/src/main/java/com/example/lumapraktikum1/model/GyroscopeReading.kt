package de.codlin.MissionPossible.model


data class GyroscopeReading(
    val timestampMillis: Long,
    val x: Float,
    val y: Float,
    val z: Float,

)