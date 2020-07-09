package eu.depau.etchdroid.repositories

import eu.depau.etchdroid.db.dao.JobDao
import eu.depau.etchdroid.db.model.Job
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MainRepository @Inject constructor(private var jobDao: JobDao) {
    fun getAll() = jobDao.getAll()
    fun getById(jobId: Long) = jobDao.getById(jobId)
    fun insert(job: Job) = jobDao.insert(job)
    fun insertAll(jobs: List<Job>) = jobDao.insertAll(jobs)
    fun update(job: Job) = jobDao.update(job)
    fun delete(job: Job) = jobDao.delete(job)
}