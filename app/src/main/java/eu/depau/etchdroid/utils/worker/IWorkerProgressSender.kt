package eu.depau.etchdroid.utils.worker

interface IWorkerProgressSender {
    fun attachWorkerProgressListener(listener: IWorkerProgressListener): Boolean
    fun detachWorkerProgressListener(listener: IWorkerProgressListener): Boolean
}