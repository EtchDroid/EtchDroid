package eu.depau.etchdroid.db.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import eu.depau.etchdroid.db.entity.Job

@Dao
interface JobRepository {
    @Query("SELECT * FROM job")
    fun getAll(): List<Job>

    @Query("SELECT * FROM job WHERE jobId == :jobId LIMIT 1")
    fun getById(jobId: Long): Job

    @Insert
    fun insert(job: Job): Long

    @Insert
    fun insertAll(vararg jobs: Job)

    @Delete
    fun delete(job: Job)
}
