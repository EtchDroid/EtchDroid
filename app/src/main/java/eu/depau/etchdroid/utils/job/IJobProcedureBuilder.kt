package eu.depau.etchdroid.utils.job

import android.content.Context
import android.os.Parcelable

interface IJobProcedureBuilder : Parcelable {
    fun build(context: Context): IJobProcedure
}