package eu.depau.etchdroid.utils.imagetypes

import android.content.Context
import android.net.Uri
import eu.depau.etchdroid.utils.enums.FilesystemType
import eu.depau.etchdroid.utils.enums.PartitionTableType
import eu.depau.etchdroid.utils.ktexts.getBinary
import eu.depau.etchdroid.utils.ktexts.readString
import eu.depau.etchdroid.utils.Partition
import eu.depau.etchdroid.utils.PartitionBuilder
import java.io.File

val SECTOR_SIZE = 512
private val partRegex = Regex("partition (\\d+): begin=(\\d+), size=(\\d+), decoded=(\\d+), firstsector=(\\d+), sectorcount=(\\d+), blocksruncount=(\\d+)\\s+(.*) \\((.+) : \\d+\\)", RegexOption.MULTILINE)

private fun readPartitionTable(dmg2img: File, libDir: String, file: File): Triple<PartitionTableType?, List<Partition>?, Long?> {
    val pt = ArrayList<Partition>()
    var ptType: PartitionTableType? = null
    var imgSize = 0L

    val pb = ProcessBuilder(dmg2img.path, "-v", "-l", file.path)
    pb.environment()["LD_LIBRARY_PATH"] = libDir
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
            "MBR" -> {
                ptType = PartitionTableType.MSDOS
                continue@matchloop
            }
        }

        pb.fsType = when {
            type == "Apple_Empty" || type == "Apple_Scratch" || type == "Apple_Free" -> FilesystemType.FREE
            type == "Apple_HFS" -> FilesystemType.HFSPLUS
            type == "Apple_HFSX" -> FilesystemType.HFSPLUS
            type == "Windows_FAT_32" -> FilesystemType.FAT32
            type.startsWith("Apple_") || type == "DDM" -> FilesystemType.APT_DATA
            else -> FilesystemType.UNKNOWN
        }

        pt.add(pb.build())
    }

    return Triple(ptType, pt, imgSize)
}

class DMGImage(private val uri: Uri, private val context: Context) : Image {
    private val dmg2img: File = context.getBinary("dmg2img")
    private val libDir: String = context.applicationInfo.nativeLibraryDir
    private var loaded: Boolean = false
    private var partTable: List<Partition>? = null
    private var partTableType: PartitionTableType? = null
    private var imgSize: Long? = null

    private fun readInfo() {
        if (loaded)
            return
        val triple = readPartitionTable(dmg2img, libDir, File(uri.path))
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

}