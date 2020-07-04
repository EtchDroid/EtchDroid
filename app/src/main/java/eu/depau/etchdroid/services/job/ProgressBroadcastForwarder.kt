package eu.depau.etchdroid.services.job

import android.content.Context
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import eu.depau.etchdroid.AppBuildConfig
import eu.depau.etchdroid.R
import eu.depau.etchdroid.broadcasts.JobProgressUpdateBroadcast
import eu.depau.etchdroid.broadcasts.dto.JobProgressUpdateBroadcastDTO
import eu.depau.etchdroid.db.EtchDroidDatabase
import eu.depau.etchdroid.utils.job.IJobProgressListener
import eu.depau.etchdroid.utils.worker.IWorkerProgressListener
import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO

/**
 * This class is subscribed to IAsyncWorker+IWorkerProgressSender events by the worker service.
 * It is aware of the current JobProcedure.
 *
 * It receives the job progress and forwards it to the rest of the app.
 */
class ProgressBroadcastForwarder(
        private val jobId: Long, private val context: Context
) : IJobProgressListener, IWorkerProgressListener {

    private val db = EtchDroidDatabase.getDatabase(context)
    private val job = db.jobRepository().getById(jobId)
    private val procedure = job.jobProcedure

    private val totalProgressWeights = job.jobProcedure.map { it.progressWeight }.sum()

    private var currentActionIdx: Int? = null
    private var prevActionsProgressWeight: Double? = null


    private fun assertProcedureStarted() {
        if (currentActionIdx == null || prevActionsProgressWeight == null) {
            throw IllegalStateException("Call to a .on*() method without a call to .onProcedureStart()")
        }
    }


    private fun sendBroadcast(dto: JobProgressUpdateBroadcastDTO) {
        val intent = JobProgressUpdateBroadcast.getIntent(dto)

        when {
            AppBuildConfig.USE_LOCAL_BROADCASTS ->
                LocalBroadcastManager
                        .getInstance(context.applicationContext)
                        .sendBroadcast(intent)
            else                                ->
                context
                        .sendBroadcast(intent.apply { `package` = context.packageName })
        }
    }


    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * is starting.
     */
    override fun onProcedureStart(startActionIndex: Int) {
        currentActionIdx = startActionIndex
        prevActionsProgressWeight = job.jobProcedure
                .subList(0, currentActionIdx!!)
                .map { it.progressWeight }
                .sum()

        val isCheckpoint = currentActionIdx!! > 0
        val messageResId = if (isCheckpoint) R.string.resuming_job else R.string.starting_job

        println("$TAG -> onProcedureStart job $jobId, startAt $startActionIndex, " +
                "currentAction $currentActionIdx, prevProgress: $prevActionsProgressWeight")

        val dto = JobProgressUpdateBroadcastDTO(
                jobId = jobId,
                indefinite = true,
                step = currentActionIdx!!,
                currentMessage = context.getString(messageResId)
        )
        sendBroadcast(dto)
    }


    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * has finished successfully.
     */
    override fun onProcedureDone() {
        assertProcedureStarted()

        println("$TAG  -> onProcedureDone job $jobId action $currentActionIdx")

        // Ensure no more updates are sent after the procedure is done
        currentActionIdx = null

        val dto = JobProgressUpdateBroadcastDTO(
                jobId = jobId,
                showProgressBar = false,
                percentage = 100.0,
                completed = true,
                currentMessage = context.getString(R.string.done)
        )
        sendBroadcast(dto)
    }


    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * has failed with the provided exception.
     */
    override fun onProcedureError(error: Throwable) {
        assertProcedureStarted()

        println("$TAG -> onProcedureError job $jobId action $currentActionIdx")

        // Ensure no more updates are sent after the procedure failed
        currentActionIdx = null

        val dto = JobProgressUpdateBroadcastDTO(
                jobId = jobId,
                showProgressBar = false,
                completed = true,
                currentMessage = context.getString(R.string.job_failed),
                error = error
        )
        sendBroadcast(dto)
    }


    /**
     * Called by the current worker to update the progress listener on its status relative to
     * the current JobAction that describes its task.
     *
     * @param dto a ProgressUpdateDTO describing the current status of the worker
     */
    override fun onWorkerProgress(dto: ProgressUpdateDTO) {
        assertProcedureStarted()

        val currentActionWeight = procedure[currentActionIdx!!].progressWeight
        val indefinite = dto.progress == null
        val progress: Double? =
                if (!indefinite)
                    prevActionsProgressWeight!! + dto.progress!!.toDouble() * currentActionWeight
                else
                    null

        println("$TAG   -> onWorkerProgress job $jobId action $currentActionIdx dto $dto")

        val broadcastDTO = JobProgressUpdateBroadcastDTO(
                jobId = jobId,
                showProgressBar = true,
                indefinite = indefinite,
                percentage = if (!indefinite) progress!!.toDouble() / totalProgressWeights * 100 else null,
                step = currentActionIdx!!
        )
        sendBroadcast(broadcastDTO)
    }


    /**
     * Called by the worker service to update the forwarder on the current action index.
     *
     * @param actionIndex the current action index within the JobProcedure
     */
    override fun onActionStart(actionIndex: Int) {
        assertProcedureStarted()
        currentActionIdx = actionIndex

        println("$TAG  -> onActionStart job $jobId action $currentActionIdx")

        val dto = JobProgressUpdateBroadcastDTO(
                jobId = jobId,
                showProgressBar = true,
                indefinite = false,
                percentage = prevActionsProgressWeight!!.toDouble() / totalProgressWeights * 100,
                step = currentActionIdx!!
        )
        sendBroadcast(dto)
    }


    /**
     * Called by the worker service to notify the forwarder that the current action has finished
     * running. Whether it has finished successfully or not is unspecified.
     *
     * If it failed, a call to onProcedureError will follow with an Exception reference.
     */
    override fun onActionDone() {
        assertProcedureStarted()
        prevActionsProgressWeight = prevActionsProgressWeight!!.plus(job.jobProcedure[currentActionIdx!!].progressWeight)

        println("$TAG  -> onActionDone job $jobId action $currentActionIdx")

        // We don't really need to broadcast this event
    }

    companion object {
        const val TAG = ".s.j.PrgBcastFw"
    }
}