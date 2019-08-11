package eu.depau.etchdroid.utils.worker

interface IProgressSender {
    fun attachProgressListener(listener: IProgressListener): Boolean
    fun detachProgressListener(listener: IProgressListener): Boolean
}