package eu.depau.etchdroid.utils.job

import android.os.Parcelable
import eu.depau.etchdroid.utils.StringResBuilder
import java.io.Serializable

interface IJobProcedure : List<IJobAction>, Parcelable, Serializable {
    val name: StringResBuilder
}