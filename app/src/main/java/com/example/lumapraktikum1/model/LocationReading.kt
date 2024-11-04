package com.example.lumapraktikum1.model

data class LocationReading(
    val timestampMillis: Long,
    val lat: Double,
    val long: Double,
    val altitude: Double,
)