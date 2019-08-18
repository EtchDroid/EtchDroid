package eu.depau.etchdroid.testutils.job

import android.os.Parcel
import eu.depau.etchdroid.testutils.worker.MockJobWorker
import eu.depau.etchdroid.utils.StringResBuilder
import eu.depau.etchdroid.utils.job.IJobAction
import eu.depau.etchdroid.utils.job.enums.SharedDataType
import eu.depau.etchdroid.utils.worker.IAsyncWorker
import eu.depau.kotlet.android.parcelable.KotletParcelable
import eu.depau.kotlet.android.parcelable.parcelableCreator
import eu.depau.kotlet.android.parcelable.readBool
import eu.depau.kotlet.android.parcelable.writeBool
import org.mockito.Mockito
import java.io.Serializable

open class MockJobAction(
        override val name: StringResBuilder,
        override val progressWeight: Double,
        private val startAt: Int,
        private val steps: Int,
        override val showInGUI: Boolean,
        override val runAlways: Boolean
) : IJobAction, KotletParcelable {
    override val checkpoint: Serializable?
        get() = TODO("not implemented")

    constructor(parcel: Parcel) : this(
            parcel.readTypedObject(StringResBuilder.CREATOR)!!,
            parcel.readDouble(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readBool(),
            parcel.readBool()
    )

    internal open val lazyWorker: IAsyncWorker by lazy {
        Mockito.spy(MockJobWorker(startAt, steps))
    }

    override fun getWorker(sharedData: MutableMap<SharedDataType, Any?>): IAsyncWorker {
        println("Retrieved worker for Action with nameResId $name")
        return lazyWorker
    }

    /**
     * Check action preconditions and return a list of StringResBuilders, which will then be
     * built and rendered in the UI.
     *
     * @return a list of failed preconditions descriptions
     */
    override fun checkPreconditions(): List<StringResBuilder> {
        TODO("not implemented")
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.apply {
            writeTypedObject(name, flags)
            writeDouble(progressWeight)
            writeInt(startAt)
            writeInt(steps)
            writeBool(showInGUI)
            writeBool(runAlways)
        }
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::MockJobAction)
    }
}