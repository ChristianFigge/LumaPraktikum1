
import android.content.Context
import android.hardware.SensorEvent
import android.location.Location
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.FileNotFoundException
import java.io.IOException

open class DataCollector(open val name : String) {
    // Speichert Sensor- & Location-Daten mit Timestamp als Key:
    private var data = LinkedHashMap<Long, DoubleArray>()

    fun collectDatum(se: SensorEvent) {
        val dblValues = DoubleArray(se.values.size) { i ->
            se.values[i].toDouble()
        }
        this.data[se.timestamp] = dblValues
    }

    fun collectDatum(loc: Location) {
        this.data[loc.time] = doubleArrayOf(loc.latitude, loc.longitude /* altitude, accuracy?... */)
    }

    fun getData(): LinkedHashMap<Long, DoubleArray> {
        return this.data
    }

    fun getDataAsJson(): String {
        return Json.encodeToString(this.data)
    }

    fun clearData() {
        this.data.clear()
    }

    @Throws(FileNotFoundException::class, IOException::class)
    fun writeJsonToStorage(context : Context) {
        val jsonData : String = this.getDataAsJson()
        context.openFileOutput("${this.name}.json", Context.MODE_PRIVATE).use {
            it.write(jsonData.toByteArray())
        }
    }

    @Throws(FileNotFoundException::class)
    fun readJsonFromStorage(context : Context) : String {
        return context.openFileInput("${this.name}.json").bufferedReader().readText()
    }
}
