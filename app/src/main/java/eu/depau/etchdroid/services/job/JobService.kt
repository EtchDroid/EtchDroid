package eu.depau.etchdroid.services.job

import android.app.IntentService
import android.content.Intent
import eu.depau.etchdroid.services.job.dto.JobServiceIntentDTO

/**
 * Job service. Creates an instance of and runs another class to prevent Android
 * from reusing data in another thread
 */
open class JobService(name: String) : IntentService(name) {
    override fun onHandleIntent(intent: Intent?) {
        val jobDTO = intent!!.getParcelableExtra<JobServiceIntentDTO>(JobServiceIntentDTO.EXTRA)
        JobIntentHandler(this, jobDTO).handle()
    }
}