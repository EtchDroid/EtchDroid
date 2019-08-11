package eu.depau.etchdroid.services.job

import android.content.Context
import android.util.Log
import eu.depau.etchdroid.db.EtchDroidDatabase
import eu.depau.etchdroid.utils.job.IJobProgressListener
import eu.depau.etchdroid.utils.worker.IWorkerProgressListener
import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO

// TODO: implement class stubs

/**
 * This class is subscribed to IAsyncWorker+IProgressSender events by the worker service.
 * It is aware of the current JobProcedure (in fact, it retrieves it by ID from the database).
 *
 * It tracks
 */
class ProgressBroadcastForwarder(private val context: Context) : IJobProgressListener, IWorkerProgressListener {

    var jobId: Long = -1

    private val db = EtchDroidDatabase.getDatabase(context)
    private val jobEntity = db.jobRepository().getById(jobId)

    private val totalProgressWeights = jobEntity.jobProcedure.map { it.progressWeight }.sum()

    private var currentActionIdx: Int? = null
    private var prevActionsProgressWeight: Double? = null

    private fun assertProcedureStarted() {
        if (currentActionIdx == null || prevActionsProgressWeight == null) {
            throw IllegalStateException("Call to a .on*() method without a call to .onProcedureStart()")
        }
    }


    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * is starting.
     */
    override fun onProcedureStart(startActionIndex: Int) {
        currentActionIdx = startActionIndex
        prevActionsProgressWeight = jobEntity.jobProcedure
                .subList(0, currentActionIdx!!)
                .map { it.progressWeight }
                .sum()
    }


    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * has finished successfully.
     */
    override fun onProcedureDone() {
        assertProcedureStarted()
        // TODO: stub
        Log.d(TAG, "onProcedureDone job $jobId ACTION $currentActionIdx")
    }

    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * has failed with the provided exception.
     */
    override fun onProcedureError(error: Exception) {
        assertProcedureStarted()
        // TODO: stub
        Log.d(TAG, "onProcedureError job $jobId ACTION $currentActionIdx", error)
    }

    /**
     * Called by the current worker to update the progress listener on its status relative to
     * the current JobAction that describes its task.
     *
     * @param dto a ProgressUpdateDTO describing the current status of the worker
     */
    override fun onWorkerProgress(dto: ProgressUpdateDTO) {
        assertProcedureStarted()
        // TODO: stub
        Log.d(TAG, "onWorkerProgress job $jobId ACTION $currentActionIdx dto $dto")
    }

    /**
     * Called by the worker service to update the forwarder on the current ACTION index.
     *
     * @param actionIndex the current ACTION index within the JobProcedure
     */
    override fun onActionStart(actionIndex: Int) {
        assertProcedureStarted()
        currentActionIdx = actionIndex
        prevActionsProgressWeight = prevActionsProgressWeight!!.plus(jobEntity.jobProcedure[actionIndex].progressWeight)

        // TODO: stub
        Log.d(TAG, "onActionStart job $jobId ACTION $currentActionIdx")
    }

    /**
     * Called by the worker service to notify the forwarder that the current ACTION has finished
     * running. Whether it has finished successfully or not is unspecified.
     *
     * If it failed, a call to onProcedureError will follow with an Exception reference.
     */
    override fun onActionDone() {
        assertProcedureStarted()
        // TODO: stub
        Log.d(TAG, "onActionDone job $jobId ACTION $currentActionIdx")
    }

    companion object {
        const val TAG = ".w.ProgBcastFwd"
    }
}