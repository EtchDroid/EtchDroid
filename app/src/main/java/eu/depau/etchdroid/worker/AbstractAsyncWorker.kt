package eu.depau.etchdroid.worker

abstract class AbstractAsyncWorker : IAsyncWorker, AbstractProgressSender() {
    override suspend fun run() {
        while (runStep()) {
        }
    }
}