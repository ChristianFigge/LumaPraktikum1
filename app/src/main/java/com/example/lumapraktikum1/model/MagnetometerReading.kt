package com.example.lumapraktikum1.model

data class MagnetometerReading (
        val timestampMillis: Long,
        val x: Float,
        val y: Float,
        val z: Float,

)