package eu.depau.etchdroid.worker.impl

import android.content.Context
import eu.depau.etchdroid.worker.IProgressListener
import eu.depau.etchdroid.worker.dto.ProgressDoneDTO
import eu.depau.etchdroid.worker.dto.ProgressStartDTO
import eu.depau.etchdroid.worker.dto.ProgressUpdateDTO

class BroadcastProgressForwarder(
        val jobId: Long,
        val context: Context

        ): IProgressListener {
    override fun notifyStart(dto: ProgressStartDTO) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyProgress(dto: ProgressUpdateDTO) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun notifyDone(dto: ProgressDoneDTO) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun stopForwarding() {

    }
}