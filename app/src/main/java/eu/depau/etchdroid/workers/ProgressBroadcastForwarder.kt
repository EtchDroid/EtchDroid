package eu.depau.etchdroid.workers

import android.content.Context
import android.util.Log
import eu.depau.etchdroid.db.EtchDroidDatabase
import eu.depau.etchdroid.utils.worker.IProgressForwarder
import eu.depau.etchdroid.utils.worker.IProgressListener
import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO


const val TAG = ".w.ProgBcastFwd"

// TODO: implement class stubs

/**
 * This class is subscribed to IAsyncWorker+IProgressSender events by the worker service.
 * It is aware of the current JobProcedure (in fact, it retrieves it by ID from the database).
 *
 * It tracks
 */
class ProgressBroadcastForwarder(private val context: Context) : IProgressListener, IProgressForwarder {

    var jobId: Long = -1

    private val db = EtchDroidDatabase.getDatabase(context)
    private val jobEntity = db.jobRepository().getById(jobId)

    private val totalProgressWeights = jobEntity.jobProcedure.map { it.progressWeight }.sum()

    private var currentActionIdx: Int? = null
    private var prevActionsProgressWeight: Double? = null

    private fun assertProcedureStarted() {
        if (currentActionIdx == null || prevActionsProgressWeight == null) {
            throw IllegalStateException("Call to a .notify*() method without a call to .notifyProcedureStart()")
        }
    }

    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * is starting.
     */
    override fun notifyProcedureStart(startActionIndex: Int) {
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
    override fun notifyProcedureDone() {
        assertProcedureStarted()
        // TODO: stub
        Log.d(TAG, "notifyProcedureDone job $jobId ACTION $currentActionIdx")
    }

    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * has failed with the provided exception.
     */
    override fun notifyProcedureError(error: Exception) {
        assertProcedureStarted()
        // TODO: stub
        Log.d(TAG, "notifyProcedureError job $jobId ACTION $currentActionIdx", error)
    }

    /**
     * Called by the current worker to update the progress listener on its status relative to
     * the current JobAction that describes its task.
     *
     * @param dto a ProgressUpdateDTO describing the current status of the worker
     */
    override fun notifyActionProgress(dto: ProgressUpdateDTO) {
        assertProcedureStarted()
        // TODO: stub
        Log.d(TAG, "notifyActionProgress job $jobId ACTION $currentActionIdx dto $dto")
    }

    /**
     * Called by the worker service to update the forwarder on the current ACTION index.
     *
     * @param actionIndex the current ACTION index within the JobProcedure
     */
    override fun notifyActionStart(actionIndex: Int) {
        assertProcedureStarted()
        currentActionIdx = actionIndex
        prevActionsProgressWeight = prevActionsProgressWeight!!.plus(jobEntity.jobProcedure[actionIndex].progressWeight)

        // TODO: stub
        Log.d(TAG, "notifyActionStart job $jobId ACTION $currentActionIdx")
    }

    /**
     * Called by the worker service to notify the forwarder that the current ACTION has finished
     * running. Whether it has finished successfully or not is unspecified.
     *
     * If it failed, a call to notifyProcedureError will follow with an Exception reference.
     */
    override fun notifyActionDone() {
        assertProcedureStarted()
        // TODO: stub
        Log.d(TAG, "notifyActionDone job $jobId ACTION $currentActionIdx")
    }


}