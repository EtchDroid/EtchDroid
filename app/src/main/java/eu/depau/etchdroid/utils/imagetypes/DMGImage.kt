package eu.depau.etchdroid.utils.imagetypes

import android.content.Context
import android.net.Uri
import eu.depau.etchdroid.utils.Partition
import eu.depau.etchdroid.utils.PartitionBuilder
import eu.depau.etchdroid.utils.enums.FilesystemType
import eu.depau.etchdroid.utils.enums.PartitionTableType
import eu.depau.etchdroid.utils.ktexts.getBinary
import eu.depau.etchdroid.utils.ktexts.readString
import eu.depau.etchdroid.utils.streams.AbstractSizedInputStream
import eu.depau.etchdroid.utils.streams.SizedInputStream
import java.io.File

val SECTOR_SIZE = 512
private val partRegex = Regex("partition (\\d+): begin=(\\d+), size=(\\d+), decoded=(\\d+), firstsector=(\\d+), sectorcount=(\\d+), blocksruncount=(\\d+)\\s+(.*) \\((.+) : \\d+\\)", RegexOption.MULTILINE)
private val partitionListRegex = Regex("\\s*partition (\\d+): begin=(\\d+), size=(\\d+), decoded=(\\d+), firstsector=(\\d+), sectorcount=(\\d+), blocksruncount=(\\d+)\\s*")

class DMGImage(private val uri: Uri, private val context: Context) : Image {
    private val libDir: String = context.applicationInfo.nativeLibraryDir
    private var loaded: Boolean = false
    private var partTable: List<Partition>? = null
    private var partTableType: PartitionTableType? = null
    private var imgSize: Long? = null

    private fun readInfo() {
        if (loaded)
            return
        val triple = readPartitionTable(context, libDir, File(uri.path))
        loaded = true
        partTableType = triple.first
        partTable = triple.second
        imgSize = triple.third
    }

    override val partitionTable: List<Partition>?
        get() {
            readInfo()
            return partTable
        }


    override val tableType: PartitionTableType?
        get() {
            readInfo()
            return partTableType
        }

    override val size: Long?
        get() {
            readInfo()
            return imgSize
        }

    override val inputStream: AbstractSizedInputStream
        get() = getRawImageInputStream(context, uri)

    companion object {
        private fun getDmg2ImgProcessBuilder(context: Context, vararg args: String): ProcessBuilder =
                ProcessBuilder(context.getBinary("dmg2img").path, *args)
                        .apply {
                            environment()["LD_LIBRARY_PATH"] = context.applicationInfo.nativeLibraryDir
                        }

        private fun readPartitionTable(context: Context, libDir: String, file: File): Triple<PartitionTableType?, List<Partition>?, Long?> {
            val pt = ArrayList<Partition>()
            var ptType: PartitionTableType? = null
            var imgSize = 0L

            val pb = getDmg2ImgProcessBuilder(context, "-v", "-l", file.path)
            pb.redirectErrorStream(true)

            val p = pb.start()
            val out = p.inputStream.readString()
            System.err.println(out)
            p.waitFor()
            val matches = partRegex.findAll(out)

            matchloop@ for (m in matches) {
                val (
                        number, begin, size,
                        decoded, firstsector,
                        sectorcount, blocksruncount,
                        label, type
                ) = m.destructured

                val pb = PartitionBuilder()

                pb.number = number.toInt()
                pb.size = SECTOR_SIZE * sectorcount.toLong()
                imgSize += pb.size!!

                if (label.isNotEmpty())
                    pb.fsLabel = label

                pb.partLabel = type

                when (type) {
                    "Apple_partition_map" -> {
                        ptType = PartitionTableType.MAC
                    }
                    "MBR"                 -> {
                        ptType = PartitionTableType.MSDOS
                        continue@matchloop
                    }
                }

                pb.fsType = when {
                    type == "Apple_Empty" || type == "Apple_Scratch" || type == "Apple_Free" -> FilesystemType.FREE
                    type == "Apple_HFS"                                                      -> FilesystemType.HFSPLUS
                    type == "Apple_HFSX"                                                     -> FilesystemType.HFSPLUS
                    type == "Windows_FAT_32"                                                 -> FilesystemType.FAT32
                    type.startsWith("Apple_") || type == "DDM"                               -> FilesystemType.APT_DATA
                    else                                                                     -> FilesystemType.UNKNOWN
                }

                pt.add(pb.build())
            }

            return Triple(ptType, pt, imgSize)
        }

        fun getRawImageInputStream(
                context: Context,
                uri: Uri,
                sectorSize: Int = 512
        ): AbstractSizedInputStream {
            val pb = getDmg2ImgProcessBuilder(context, "-v", uri.path!!, "-")
            val process = pb.start()
            val errReader = process.errorStream.bufferedReader()

            // Read blocksruncount
            var matched = false
            var lastSector = 0L

            while (true) {
                val line = errReader.readLine() ?: break
                val match = partitionListRegex.find(line) ?: if (matched) break else continue
                matched = true

                val (begin, size, decoded, firstsector, sectorcount, blocksruncount) = match.destructured

                val partLastSector = firstsector.toLong() + sectorcount.toLong()
                if (partLastSector > lastSector)
                    lastSector = partLastSector
            }

            val bytesTotal = lastSector * sectorSize
            return SizedInputStream(bytesTotal, process.inputStream)
        }
    }

}