package eu.depau.etchdroid.testutils.job

import android.os.Parcel
import eu.depau.etchdroid.testutils.worker.MockJobWorker
import eu.depau.etchdroid.utils.StringResBuilder
import eu.depau.etchdroid.utils.job.IJobAction
import eu.depau.etchdroid.utils.worker.IAsyncWorker
import eu.depau.etchdroid.utils.worker.dto.ProgressUpdateDTO
import eu.depau.etchdroid.utils.worker.enums.RateUnit
import eu.depau.kotlet.android.parcelable.KotletParcelable
import eu.depau.kotlet.android.parcelable.parcelableCreator
import org.mockito.Mockito

class MockJobAction(
        override val nameResId: Int,
        override val progressWeight: Double
) : IJobAction, KotletParcelable {

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readDouble()
    )

    private val lazyWorker by lazy {
        Mockito.spy(
                MockJobWorker(
                        // TODO: fix update dto so it doesn't have excess fields
                        ProgressUpdateDTO(
                                -1, 0, 0.0,
                                0.0, 0, 0.0,
                                RateUnit.FURLONGS_PER_FORTNIGHT
                        )
                )
        )
    }

    override fun getWorker(): IAsyncWorker = lazyWorker

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
            writeInt(nameResId)
            writeDouble(progressWeight)
        }
    }

    companion object {
        @JvmField
        val CREATOR = parcelableCreator(::MockJobAction)
    }
}