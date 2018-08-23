package eu.depau.termux_wrapper

import android.content.Context
import android.os.Build
import org.apache.commons.io.FileUtils
import org.kamranzafar.jtar.TarEntry
import org.kamranzafar.jtar.TarInputStream
import java.io.*
import java.nio.file.attribute.PosixFilePermission


@Throws(IOException::class)
fun InputStream.readString(): String {
    val baos = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var length = this.read(buffer)

    while (length != -1) {
        baos.write(buffer, 0, length)
        length = this.read(buffer)
    }
    return baos.toString("UTF-8")
}


class TermuxWrapper(val context: Context) {
    public var path: MutableList<String> = mutableListOf(
            "/usr/local/sbin",
            "/usr/local/bin",
            "/usr/sbin",
            "/usr/bin",
            "/sbin",
            "/bin"
    )

    val arch: String
        get() {
            val abi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                Build.SUPPORTED_ABIS[0]
            else
                Build.CPU_ABI

            return when {
                abi.contains("armeabi-v7a") -> "armeabi-v7a"
                abi.contains("x86_64") -> "x86_64"
                abi.contains("x86") -> "x86"
                abi.contains("arm64-v8a") -> "arm64-v8a"
                else -> null!!
            }
        }

    val needsUpdate: Boolean
        get() {
            val assetManager = context.assets
            val hashAssets = assetManager.open("termux/$arch/pkg_fprint.txt").readString()
            val hashFilesFile = File("${context.filesDir}/termux/pkg_fprint.txt")

            if (!hashFilesFile.exists())
                return true

            val hashFiles = FileInputStream(hashFilesFile).readString()

            return hashAssets != hashFiles
        }

    private fun applyPerms(tarEntry: TarEntry, file: File) {
        //  u  2  g 1   o 0
        // [rwx] [rwx] [rwx]

        val mode = tarEntry.header.mode
        val permSet: MutableSet<PosixFilePermission> = mutableSetOf()

        // Get owner permissions. On Android API < 26 there's no easy way to set the others
        val stepInt = (mode shr (6)) and 0xFFF8

        // Iterate over permission bits
        for (bit in listOf(1, 2, 4)) {
            val bool = (stepInt and bit) > 0
            when (bit) {
                1 -> file.setExecutable(bool, true)
                2 -> file.setWritable(bool, true)
                4 -> file.setReadable(bool, true)
                else -> null!!
            }
        }
    }

    fun doUpdate() {
        val oldTarDir = File("${context.filesDir}/termux")
        if (oldTarDir.exists())
            FileUtils.deleteDirectory(oldTarDir)

        val destDir = context.filesDir!!

        val assetManager = context.assets
        val termuxTar = TarInputStream(assetManager.open("termux/$arch/packages.tar").buffered())
        var tarEntry = termuxTar.nextEntry

        termuxTar.use { tis ->
            while (tarEntry != null) {
                val data = ByteArray(2048)
                val destFile = File("$destDir/${tarEntry.name}")
                val fos = FileOutputStream(destFile)
                val dest = BufferedOutputStream(fos)
                var count = tis.read(data)

                while (count != -1) {
                    dest.write(data, 0, count)
                    count = tis.read(data)
                }

                dest.close()
                applyPerms(tarEntry, destFile)

                tarEntry = tis.nextEntry
            }
        }
    }

    fun getFile(path: String) = File("${context.filesDir}/termux/$path")

    fun which(cmd: String): File? {
        for (p in path) {
            val file = getFile("$p/$cmd")

            if (file.exists() && file.isFile && file.canExecute())
                return file
        }
        return null
    }
}