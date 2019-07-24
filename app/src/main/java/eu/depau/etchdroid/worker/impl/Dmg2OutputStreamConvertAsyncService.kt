package eu.depau.etchdroid.worker.impl

import eu.depau.etchdroid.worker.abstractimpl.AbstractAutoProgressAsyncWorker
import eu.depau.etchdroid.worker.dto.ProgressUpdateDTO

class Dmg2OutputStreamConvertAsyncService(private val totalToDo: Long): AbstractAutoProgressAsyncWorker(totalToDo) {
    override val progressUpdateDTO: ProgressUpdateDTO
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

    override fun runStep(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}