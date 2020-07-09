package eu.depau.etchdroid.di.module

import android.app.Application
import androidx.room.Room
import dagger.Module
import dagger.Provides
import eu.depau.etchdroid.db.EtchDroidDatabase
import eu.depau.etchdroid.db.dao.JobDao
import javax.inject.Singleton

@Module
class DatabaseModule {

    private lateinit var database: EtchDroidDatabase

    @Singleton
    @Provides
    fun providesDatabase(application: Application): EtchDroidDatabase {
        database = Room.databaseBuilder(application, EtchDroidDatabase::class.java, "etchdroid_db").build()
        return database
    }

    @Singleton
    @Provides
    fun providesJobDao(database: EtchDroidDatabase): JobDao = database.jobDao()

}