package eu.depau.etchdroid.utils.job

import android.os.Parcelable

interface IJobProcedureBuilder : Parcelable {
    fun build(): IJobProcedure
}