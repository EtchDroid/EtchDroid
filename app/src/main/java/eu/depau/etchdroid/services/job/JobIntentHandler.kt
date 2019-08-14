package eu.depau.etchdroid.services.job

import eu.depau.etchdroid.db.EtchDroidDatabase
import eu.depau.etchdroid.db.entity.Job
import eu.depau.etchdroid.notification.impl.JobServiceNotificationHandler
import eu.depau.etchdroid.services.job.dto.JobServiceIntentDTO
import eu.depau.etchdroid.utils.job.IJobAction
import eu.depau.etchdroid.utils.job.IJobProcedure
import eu.depau.etchdroid.utils.job.impl.AbstractJobProgressSender
import eu.depau.etchdroid.utils.ktexts.runForeground
import eu.depau.etchdroid.utils.worker.IAsyncWorker

class JobIntentHandler(
        val context: JobService,
        private val jobDTO: JobServiceIntentDTO
) : AbstractJobProgressSender() {

    private val jobId = jobDTO.jobId
    private val svcNotificationHandler = JobServiceNotificationHandler(context)
    private val notification = svcNotificationHandler.build()
    private lateinit var job: Job
    private val broadcastForwarder = ProgressBroadcastForwarder(jobId, context)

    init {
        assert(jobId >= 0) { "Invalid jobId (jobId < 0)" }
    }

    fun handle() = context.runForeground(jobId.toInt(), notification, true) {
        val db = EtchDroidDatabase.getDatabase(context)

        job = db.jobRepository().getById(jobId).also {
            assert(!it.completed) { "Job $jobId already completed" }
        }

        job.attachAndRun { procedure ->
            procedure.forEachAction { index, action, worker ->
                while (worker.runStep()) {
                }
            }
        }

        job.completed = true
        db.jobRepository().update(job)
    }


    private inline fun <T> Job.attachAndRun(body: (procedure: IJobProcedure) -> T): T {
        attachJobProgressListener(broadcastForwarder)
        try {
            notifyJobProcedureStart(this.checkpointActionIndex ?: 0)
            val result = body(this.jobProcedure)
            notifyJobProcedureDone()
            return result
        } catch (t: Throwable) {
            notifyJobProcedureError(t)
            throw t
        } finally {
            detachJobProgressListener(broadcastForwarder)
        }
    }

    private inline fun IJobProcedure.forEachAction(
            body: (index: Int, action: IJobAction, worker: IAsyncWorker) -> Unit
    ) {
        val startIndex = job.checkpointActionIndex ?: 0

        (startIndex until size).forEach { index ->

            val action = this[index]
            val worker = action.getWorker()
            worker.attachWorkerProgressListener(broadcastForwarder)

            try {
                broadcastForwarder.onActionStart(index)
                body(index, action, worker)
                broadcastForwarder.onActionDone()
            } finally {
                worker.detachWorkerProgressListener(broadcastForwarder)
            }
        }
    }
}