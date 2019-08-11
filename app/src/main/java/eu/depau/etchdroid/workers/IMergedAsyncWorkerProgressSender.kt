package eu.depau.etchdroid.workers

import eu.depau.etchdroid.utils.worker.IAsyncWorker
import eu.depau.etchdroid.utils.worker.IProgressSender


internal interface IMergedAsyncWorkerProgressSender: IAsyncWorker, IProgressSender