package eu.depau.etchdroid.services.job

import android.content.Context
import eu.depau.etchdroid.db.EtchDroidDatabase
import eu.depau.etchdroid.utils.job.IJobProgressListener
import eu.depau.etchdroid.utils.worker.IWorkerProgressListener
import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO

// TODO: implement class stubs

/**
 * This class is subscribed to IAsyncWorker+IWorkerProgressSender events by the worker service.
 * It is aware of the current JobProcedure (in fact, it retrieves it by ID from the database).
 *
 * It tracks
 */
class ProgressBroadcastForwarder(
        private val jobId: Long, private val context: Context
) : IJobProgressListener, IWorkerProgressListener {

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

        println("-> onProcedureStart job $jobId, startAt $startActionIndex, " +
                "currentAction $currentActionIdx, prevProgress: $prevActionsProgressWeight")
    }


    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * has finished successfully.
     */
    override fun onProcedureDone() {
        assertProcedureStarted()
        // TODO: stub
        println("-> onProcedureDone job $jobId action $currentActionIdx")
    }


    /**
     * Called by the worker service to notify the forwarder that the currently referenced procedure
     * has failed with the provided exception.
     */
    override fun onProcedureError(error: Throwable) {
        assertProcedureStarted()
        // TODO: stub
        println("-> onProcedureError job $jobId action $currentActionIdx")
        error.printStackTrace()
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
        println("  -> onWorkerProgress job $jobId action $currentActionIdx dto $dto")
    }


    /**
     * Called by the worker service to update the forwarder on the current action index.
     *
     * @param actionIndex the current action index within the JobProcedure
     */
    override fun onActionStart(actionIndex: Int) {
        assertProcedureStarted()
        currentActionIdx = actionIndex
        prevActionsProgressWeight = prevActionsProgressWeight!!.plus(jobEntity.jobProcedure[actionIndex].progressWeight)

        // TODO: stub
        println("  -> onActionStart job $jobId action $currentActionIdx")
    }


    /**
     * Called by the worker service to notify the forwarder that the current action has finished
     * running. Whether it has finished successfully or not is unspecified.
     *
     * If it failed, a call to onProcedureError will follow with an Exception reference.
     */
    override fun onActionDone() {
        assertProcedureStarted()
        // TODO: stub
        println("  -> onActionDone job $jobId action $currentActionIdx")
    }
}