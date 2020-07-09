package eu.depau.etchdroid.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import eu.depau.etchdroid.db.converter.JobProcedureConverter
import eu.depau.etchdroid.db.dao.JobDao
import eu.depau.etchdroid.db.model.Job

// TODO setup export schema
@Database(entities = [Job::class], version = 1)
@TypeConverters(JobProcedureConverter::class)
abstract class EtchDroidDatabase : RoomDatabase() {
    abstract fun jobDao(): JobDao

    // TODO refactor instance creation with DI (is using in JobServiceIntentHandlerTest.kt)
    companion object {
        @Volatile
        private var INSTANCE: EtchDroidDatabase? = null

        fun getDatabase(context: Context): EtchDroidDatabase {
            val tempInstance = INSTANCE
            if (tempInstance != null) {
                return tempInstance
            }
            synchronized(this) {
                val instance = Room.databaseBuilder(
                        context.applicationContext,
                        EtchDroidDatabase::class.java,
                        "etchdroid_db"
                ).build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
