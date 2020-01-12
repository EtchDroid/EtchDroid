package eu.depau.etchdroid.utils.imagetypes

import eu.depau.etchdroid.utils.Partition
import eu.depau.etchdroid.utils.enums.PartitionTableType
import eu.depau.etchdroid.utils.streams.AbstractSizedInputStream

class RawImage: Image {
    override val partitionTable: List<Partition>?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val tableType: PartitionTableType?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val size: Long?
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
    override val inputStream: AbstractSizedInputStream
        get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
}