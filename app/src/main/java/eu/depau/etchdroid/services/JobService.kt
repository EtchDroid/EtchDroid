package eu.depau.etchdroid.services

import android.app.IntentService
import android.content.Intent
import eu.depau.etchdroid.db.EtchDroidDatabase
import eu.depau.etchdroid.notification.impl.JobServiceNotificationHandler
import eu.depau.etchdroid.services.dto.JobServiceIntentDTO

open class JobService(name: String) : IntentService(name) {
    constructor() : this("TestJobService")

    private fun startProcedure(jobId: Long) {

    }

    private fun startAction(jobId: Long, actionIndex: Int) {

    }

    override fun onHandleIntent(intent: Intent?) {
        intent!!  // Assert intent != null
        val jobDTO = intent.getParcelableExtra<JobServiceIntentDTO>(JobServiceIntentDTO.EXTRA)
        val jobId = jobDTO.jobId

        assert(jobId >= 0) { "Invalid jobId (jobId < 0)" }

        val svcNotificationHandler = JobServiceNotificationHandler(this)
        val notification = svcNotificationHandler.build()
        startForeground(jobId.toInt(), notification)

        // TODO: Implement notification broadcast receiver and bind it here
        // TODO: add and notify BroadcastProgressForwarder

        try {
            val db = EtchDroidDatabase.getDatabase(this)
            val job = db.jobRepository().getById(jobId)
            assert(!job.completed) { "Job $jobId already completed" }

            val procedure = job.jobProcedure
            // TODO: notify BroadcastProgressForwarder
            startProcedure(jobId)

            for (i in 0 until procedure.size) {
                val action = procedure[i]
                startAction(jobId, i)
                // TODO: notify BroadcastProgressForwarder

                val worker = action.getWorker()
                while (worker.runStep()) {
                }

                // TODO: notify BroadcastProgressForwarder
            }

        } finally {
            stopForeground(true)
        }
    }
}