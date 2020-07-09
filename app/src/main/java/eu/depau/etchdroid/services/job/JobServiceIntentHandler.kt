package eu.depau.etchdroid.services.job

import android.app.Notification
import android.app.Service
import android.content.IntentFilter
import android.util.Log
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import eu.depau.etchdroid.AppBuildConfig
import eu.depau.etchdroid.broadcasts.JobProgressUpdateBroadcast
import eu.depau.etchdroid.db.EtchDroidDatabase
import eu.depau.etchdroid.db.model.Job
import eu.depau.etchdroid.notification.impl.JobServiceNotificationBuilder
import eu.depau.etchdroid.repositories.MainRepository
import eu.depau.etchdroid.services.job.dto.JobServiceIntentDTO
import eu.depau.etchdroid.services.job.exception.JobActionFailedException
import eu.depau.etchdroid.utils.job.IJobAction
import eu.depau.etchdroid.utils.job.IJobProcedure
import eu.depau.etchdroid.utils.job.enums.SharedDataType
import eu.depau.etchdroid.utils.job.impl.AbstractJobProgressSender
import eu.depau.etchdroid.utils.worker.IAsyncWorker
import javax.inject.Inject

class JobServiceIntentHandler(
        val context: JobService,
        jobDTO: JobServiceIntentDTO
) : AbstractJobProgressSender() {

    var repository: MainRepository? = null
        @Inject set

    private val jobId = jobDTO.jobId
    private val svcNotificationBuilder = JobServiceNotificationBuilder(context)
    private lateinit var job: Job
    private val broadcastForwarder = ProgressBroadcastForwarder(jobId, context)
    private val sharedWorkerData = mutableMapOf<SharedDataType, Any?>()

    // Only build notification if we're not in test mode
    private val notification = if (!AppBuildConfig.TEST_BUILD)
        svcNotificationBuilder.getBuilder().build()
    else
        Notification()

    init {
        // TODO better determination if usage in unit test
        if (repository == null) {
            repository = MainRepository(EtchDroidDatabase.getDatabase(context).jobDao())
        }

        assert(jobId >= 0) { "Invalid jobId (jobId < 0)" }
    }

    fun handle() = context.runForeground(jobId.toInt(), notification, true) {

        job = repository!!.getById(jobId).also {
            assert(!it.completed) { "Job $jobId already completed" }
        }

        // Add service context to shared worker data
        sharedWorkerData[SharedDataType.CONTEXT] = context

        try {
            withNotificationBroadcastReceiver {
                job.attachAndRun { procedure ->
                    procedure.forEachAction(sharedWorkerData) { _, _, worker ->
                        while (worker.runStep()) {
                        }
                    }
                }

                job.completed = true
                repository!!.update(job)
            }
        } catch (e: Throwable) {
            Log.e(TAG, "JobProcedure failed", e)
        }
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
            sharedWorkerData: MutableMap<SharedDataType, Any?>,
            body: (index: Int, action: IJobAction, worker: IAsyncWorker) -> Unit
    ) {
        val startIndex = job.checkpointActionIndex

        var errorIndex: Int? = null
        var errorAction: IJobAction? = null
        var error: Throwable? = null

        this
                .filterIndexed { index, action ->
                    index >= startIndex || action.runAlways
                }
                .forEachIndexed { index, action ->
                    // Only run "runAlways" actions if there's an error
                    if (error != null && !action.runAlways) {
                        return@forEachIndexed
                    }

                    val worker = action.getWorker(sharedWorkerData)
                    worker.attachWorkerProgressListener(broadcastForwarder)

                    try {
                        broadcastForwarder.onActionStart(index)
                        body(index, action, worker)
                        broadcastForwarder.onActionDone()
                    } catch (e: Throwable) {
                        errorIndex = index
                        errorAction = action
                        error = e
                    } finally {
                        worker.detachWorkerProgressListener(broadcastForwarder)
                    }
                }

        if (error != null) {
            throw JobActionFailedException(errorAction!!, errorIndex!!, error!!)
        }
    }

    private fun <T> Service.runForeground(id: Int, notification: Notification, removeNotification: Boolean, body: () -> T): T {
        this.startForeground(id, notification)
        try {
            return body()
        } finally {
            stopForeground(removeNotification)
        }
    }

    private inline fun withNotificationBroadcastReceiver(body: () -> Unit) {
        if (AppBuildConfig.TEST_BUILD) {
            return body()
        }

        val intentFilter = IntentFilter(JobProgressUpdateBroadcast.ACTION)
        val broadcastReceiver = JobServiceNotificationBroadcastReceiver(jobId.toInt(), job, svcNotificationBuilder)

        if (AppBuildConfig.USE_LOCAL_BROADCASTS) {
            LocalBroadcastManager
                    .getInstance(context)
                    .registerReceiver(broadcastReceiver, intentFilter)
        } else {
            context.registerReceiver(broadcastReceiver, intentFilter)
        }

        try {
            body()
        } finally {
            if (AppBuildConfig.USE_LOCAL_BROADCASTS) {
                LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver)
            } else {
                context.unregisterReceiver(broadcastReceiver)
            }
        }
    }

    companion object {
        const val TAG = ".s.j.JobInHandl"
    }
}