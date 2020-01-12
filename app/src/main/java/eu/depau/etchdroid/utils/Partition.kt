package eu.depau.etchdroid.utils

import eu.depau.etchdroid.utils.enums.FilesystemType
import eu.depau.etchdroid.utils.enums.PartitionType

data class Partition(
        val number: Int?,
        val offset: Int?,
        val size: Long?,
        val partType: PartitionType?,
        val partLabel: String?,
        val fsType: FilesystemType?,
        val fsLabel: String?
)