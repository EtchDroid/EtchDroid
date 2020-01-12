package eu.depau.etchdroid.utils.job

import android.os.Parcelable
import eu.depau.etchdroid.utils.StringResBuilder
import eu.depau.etchdroid.utils.job.enums.SharedDataType
import eu.depau.etchdroid.utils.worker.IAsyncWorker
import java.io.Serializable

interface IJobAction : Parcelable, Serializable {
    /**
     * Name of the action instance
     */
    val name: StringResBuilder

    /**
     * Weight of this action's progress on the total progress
     */
    val progressWeight: Double

    /**
     * Checkpoint data
     */
    val checkpoint: Serializable?

    /**
     * Whether the GUI should mention it
     */
    val showInGUI: Boolean

    /**
     * Whether this action always needs to be run, regardless of whether there was a checkpoint or
     * not, or whether the previous workers have failed or not
     */
    val runAlways: Boolean

    /**
     * Build a worker instance that performs the action encoded in this object
     *
     * @param sharedData a mutable map that workers can use to share data among each other
     * @return a worker instance that performs the encoded action
     */
    fun getWorker(sharedData: MutableMap<SharedDataType, Any?>): IAsyncWorker

    /**
     * Check action preconditions and return a list of StringResBuilders, which will then be
     * built and rendered in the UI.
     *
     * @return a list of failed preconditions descriptions
     */
    fun checkPreconditions(): List<StringResBuilder>
}