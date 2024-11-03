package de.codlin.MissionPossible.model


data class AccelerationReading(
    val timestampMillis: Long,
    val x: Float,
    val y: Float,
    val z: Float,
)