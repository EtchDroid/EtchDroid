package eu.depau.etchdroid.workers

import eu.depau.etchdroid.utils.worker.IAsyncWorker
import eu.depau.etchdroid.utils.worker.IWorkerProgressSender


internal interface IMergedAsyncWorkerProgressSender: IAsyncWorker, IWorkerProgressSender