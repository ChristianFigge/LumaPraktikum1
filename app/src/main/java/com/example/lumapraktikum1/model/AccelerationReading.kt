package de.codlin.MissionPossible.model


data class AccelerationReading(
    val timestampMillis: Long,
    val xAxis: Float,
    val yAxis: Float,
    val zAxis: Float,
    val magnitude: Float,
)