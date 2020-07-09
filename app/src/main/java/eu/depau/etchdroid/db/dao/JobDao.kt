package eu.depau.etchdroid.db.dao

import androidx.room.*
import eu.depau.etchdroid.db.model.Job

@Dao
interface JobDao {
    @Query("SELECT * FROM job")
    fun getAll(): List<Job>

    @Query("SELECT * FROM job WHERE jobId == :jobId LIMIT 1")
    fun getById(jobId: Long): Job

    @Insert
    fun insert(job: Job): Long

    @Insert
    fun insertAll(jobs: List<Job>)

    @Update
    fun update(job: Job)

    @Delete
    fun delete(job: Job)
}
