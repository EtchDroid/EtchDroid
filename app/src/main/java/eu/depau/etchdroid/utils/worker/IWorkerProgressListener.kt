package eu.depau.etchdroid.utils.worker

import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO

interface IWorkerProgressListener {
    /**
     * Called by the current worker to update the progress listener on its status relative to
     * the current JobAction that describes its task.
     *
     * @param dto a ProgressUpdateDTO describing the current status of the worker
     */
    fun onWorkerProgress(dto: ProgressUpdateDTO)
}