package de.codlin.MissionPossible.model


data class GyroscopeReading(
    val timestampMillis: Long,
    val xRotation: Float,
    val yRotation: Float,
    val zRotation: Float,
    val magnitude: Float,

)