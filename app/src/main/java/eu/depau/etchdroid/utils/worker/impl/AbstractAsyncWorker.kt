package eu.depau.etchdroid.utils.worker.impl

import eu.depau.etchdroid.utils.worker.IAsyncWorker

abstract class AbstractAsyncWorker : IAsyncWorker, AbstractWorkerProgressSender()