package eu.depau.etchdroid.utils.job

import android.os.Parcelable
import java.io.Serializable

interface IJobProcedure: List<IJobAction>, Parcelable, Serializable {
    val nameResId: Int
}