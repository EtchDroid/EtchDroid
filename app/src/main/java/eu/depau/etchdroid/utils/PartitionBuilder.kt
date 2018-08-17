package eu.depau.etchdroid.utils

import eu.depau.etchdroid.enums.FilesystemType
import eu.depau.etchdroid.enums.PartitionType

class PartitionBuilder {
    var number: Int? = null
    var offset: Int? = null
    var size: Long? = null
    var partType: PartitionType? = null
    var partLabel: String? = null
    var fsType: FilesystemType? = null
    var fsLabel: String? = null

    fun build(): Partition = Partition(number, offset, size, partType, partLabel, fsType, fsLabel)
}