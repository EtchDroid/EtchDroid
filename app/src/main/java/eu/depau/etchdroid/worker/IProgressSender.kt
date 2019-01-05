package eu.depau.etchdroid.worker

interface IProgressSender {
    fun attachProgressListener(listener: IProgressListener): Boolean
    fun detachProgressListener(listener: IProgressListener): Boolean
}