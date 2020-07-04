package eu.depau.etchdroid.services.job

import android.app.IntentService
import android.content.Intent
import eu.depau.etchdroid.services.job.dto.JobServiceIntentDTO

/**
 * Job service. Creates an instance of and runs another class to prevent Android
 * from reusing data in another thread
 */
class JobService(name: String) : IntentService(name) {
    override fun onHandleIntent(intent: Intent?) {
        intent ?: return

        if (intent.action != ACTION)
            return

        val jobDTO = intent.getParcelableExtra<JobServiceIntentDTO>(JobServiceIntentDTO.EXTRA)
        JobServiceIntentHandler(this, jobDTO).handle()
    }

    companion object {
        const val ACTION = "eu.depau.etchdroid.services.JOB_SERVICE_START"
    }
}