package eu.depau.etchdroid.img_types

import eu.depau.etchdroid.enums.PartitionTableType
import eu.depau.etchdroid.utils.Partition

interface Image {
    val partitionTable: List<Partition>?
    val tableType: PartitionTableType?
    val size: Long?
}