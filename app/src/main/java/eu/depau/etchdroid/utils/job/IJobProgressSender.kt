package eu.depau.etchdroid.utils.job

interface IJobProgressSender {
    fun attachJobProgressListener(listener: IJobProgressListener): Boolean
    fun detachJobProgressListener(listener: IJobProgressListener): Boolean
}