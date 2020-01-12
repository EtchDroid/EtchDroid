package eu.depau.etchdroid.db.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import eu.depau.etchdroid.utils.job.IJobProcedure

class JobProcedureConverter {
    @TypeConverter
    fun fromJobProcedure(value: IJobProcedure): String {
        val gson = Gson()
        return gson.toJson(value)
    }

    @TypeConverter
    fun fromString(value: String): IJobProcedure {
        val gson = Gson()
        return gson.fromJson(value, IJobProcedure::class.java)
    }
}