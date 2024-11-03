package com.example.lumapraktikum1.core

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface SensorDataRemoteApi {
    @POST("api/gyroscope")
    suspend fun uploadGyroscopeData(@Body gyroscopeData: List<GyroscopeData>)

    @POST("api/accelerometer")
    suspend fun uploadAccelerometerData(@Body accelerometerData: List<AccelerometerData>)

    @POST("api/magnetometer")
    suspend fun uploadMagnetometerData(@Body magnetometerData: List<MagnetometerData>)
}

fun createSensorDataApi(): SensorDataRemoteApi {
    val retrofit = Retrofit.Builder()
        .baseUrl("https://luma.ospuze.de/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    return retrofit.create(SensorDataRemoteApi::class.java)
}