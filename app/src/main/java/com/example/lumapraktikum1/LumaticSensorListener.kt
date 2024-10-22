package com.example.lumapraktikum1

import android.hardware.SensorEventListener

interface LumaticSensorListener : SensorEventListener {
    var runDelayedLoop : Boolean
}