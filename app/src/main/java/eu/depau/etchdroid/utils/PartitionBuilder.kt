package eu.depau.etchdroid.utils

import eu.depau.etchdroid.utils.enums.FilesystemType
import eu.depau.etchdroid.utils.enums.PartitionType

class PartitionBuilder {
    var number: Int? = null
    private var offset: Int? = null
    var size: Long? = null
    private var partType: PartitionType? = null
    var partLabel: String? = null
    var fsType: FilesystemType? = null
    var fsLabel: String? = null

    fun build(): Partition = Partition(number, offset, size, partType, partLabel, fsType, fsLabel)
}