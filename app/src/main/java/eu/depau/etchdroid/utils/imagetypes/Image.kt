package eu.depau.etchdroid.utils.imagetypes

import android.content.Context
import eu.depau.etchdroid.utils.Partition
import eu.depau.etchdroid.utils.enums.PartitionTableType
import eu.depau.etchdroid.utils.streams.AbstractSizedInputStream

interface Image {
    val partitionTable: List<Partition>?
    val tableType: PartitionTableType?
    val size: Long?
    val inputStream: AbstractSizedInputStream
}