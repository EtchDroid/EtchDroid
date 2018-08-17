package eu.depau.etchdroid.utils

import eu.depau.etchdroid.enums.FilesystemType
import eu.depau.etchdroid.enums.PartitionType

data class Partition(
        val number: Int?,
        val offset: Int?,
        val size: Long?,
        val partType: PartitionType?,
        val partLabel: String?,
        val fsType: FilesystemType?,
        val fsLabel: String?
)