package eu.depau.etchdroid.worker

interface IAsyncWorker: IProgressSender {
    suspend fun run()
    suspend fun runStep(): Boolean
}