package eu.depau.etchdroid.worker.abstractimpl

import eu.depau.etchdroid.worker.IAsyncWorker

abstract class AbstractAsyncWorker : IAsyncWorker, AbstractProgressSender() {
    override fun run() {
        while (runStep()) {
        }
    }
}