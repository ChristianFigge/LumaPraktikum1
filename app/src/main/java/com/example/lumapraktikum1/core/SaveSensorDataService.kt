package com.example.lumapraktikum1.core

import DataCollector
import android.hardware.SensorEvent
import com.example.lumapraktikum1.model.MagnetometerReading
import de.codlin.MissionPossible.model.AccelerationReading
import de.codlin.MissionPossible.model.GyroscopeReading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SaveSensorDataService(
    private val sensorDataRemoteApi: SensorDataRemoteApi,
    private val dataCollector: DataCollector

){
    private val scope= CoroutineScope(Dispatchers.IO)

    private val listAccelerometerData=ArrayList<AccelerationReading>()
    private val listMagnetometer=ArrayList<MagnetometerReading>()
    private val listGyroscopeData=ArrayList<GyroscopeReading>()

    private val batchSize = 100



    // Save gyroscope data
    @Synchronized
    fun saveGyroscopeData(event: SensorEvent) {
        dataCollector.collectDatum(event)
        listGyroscopeData.add(GyroscopeReading(event.timestamp,
            event.values[0],
            event.values[1],
            event.values[2]))

        if (listGyroscopeData.size >= batchSize) {
            sendBatchGyroscopeData()
        }
    }

    // Save accelerometer data
    @Synchronized
    fun saveAccelerometerData(event: SensorEvent) {
        dataCollector.collectDatum(event)
        listAccelerometerData.add(
            AccelerationReading(event.timestamp,
            event.values[0],
            event.values[1],
            event.values[2])
        )

        if (listAccelerometerData.size >= batchSize) {
            sendBatchAccelerometerData()
        }
    }

    // Save magnetometer data
    @Synchronized
    fun saveMagnetometerData(event: SensorEvent) {
        dataCollector.collectDatum(event)
        listMagnetometer.add(
            MagnetometerReading(event.timestamp,
            event.values[0],
            event.values[1],
            event.values[2])
        )

        if (listMagnetometer.size >= batchSize) {
            sendBatchMagnetometerData()
        }
    }
    private fun sendBatchAccelerometerData(){
        scope.launch{
            try{
                sensorDataRemoteApi.uploadAccelerometerData(listAccelerometerData)
                listAccelerometerData.clear()
            }catch (e:Exception){
                println("Fehler: ${e.message}")
            }
        }
    }

    private fun sendBatchGyroscopeData(){
        scope.launch{
            try{
                sensorDataRemoteApi.uploadGyroscopeData(listGyroscopeData)
                listGyroscopeData.clear()
            }catch (e:Exception){
                println("Fehler: ${e.message}")
            }
        }
    }

    private fun sendBatchMagnetometerData(){
        scope.launch{
            try{
                sensorDataRemoteApi.uploadMagnetometerData(listMagnetometer)
                listMagnetometer.clear()
            }catch (e:Exception){
                println("Fehler: ${e.message}")
            }
        }
    }

}
