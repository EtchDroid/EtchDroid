package eu.depau.etchdroid.utils.job

import android.os.Parcelable
import eu.depau.etchdroid.utils.StringResBuilder
import eu.depau.etchdroid.utils.worker.IAsyncWorker
import java.io.Serializable

interface IJobAction : Parcelable, Serializable {
    val nameResId: Int
    val progressWeight: Double

    /**
     * Build a worker instance that performs the action encoded in this object
     *
     * @return a worker instance that performs the encoded action
     */
    fun getWorker(): IAsyncWorker

    /**
     * Check action preconditions and return a list of StringResBuilders, which will then be
     * built and rendered in the UI.
     *
     * @return a list of failed preconditions descriptions
     */
    fun checkPreconditions(): List<StringResBuilder>
}