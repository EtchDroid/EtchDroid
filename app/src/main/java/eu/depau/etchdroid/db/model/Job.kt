package eu.depau.etchdroid.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import eu.depau.etchdroid.utils.job.IJobProcedure

@Entity
data class Job(
        @PrimaryKey(autoGenerate = true) val jobId: Long = 0,
        var jobProcedure: IJobProcedure,
        var checkpointActionIndex: Int = 0,
        var checkpointData: String? = null,
        var completed: Boolean = false
)