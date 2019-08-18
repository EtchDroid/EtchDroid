package eu.depau.etchdroid.services.job.exception

import eu.depau.etchdroid.utils.job.IJobAction

class JobActionFailedException(action: IJobAction, index: Int, cause: Throwable) :
        Exception(
                "JobAction $action (step $index) failed",
                cause
        )