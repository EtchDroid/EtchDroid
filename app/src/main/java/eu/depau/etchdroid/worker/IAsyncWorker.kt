package eu.depau.etchdroid.worker

interface IAsyncWorker: IProgressSender {
    fun run()
    fun runStep(): Boolean
}