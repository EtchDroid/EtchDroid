package eu.depau.etchdroid.kotlin_exts

import android.content.Context
import android.os.Build
import java.io.File
import java.io.FileOutputStream


private val copiedBinaries: MutableMap<String, File> = HashMap()


fun Context.getBinary(name: String): File {
    if (name in copiedBinaries.keys)
        return copiedBinaries[name]!!

    val abi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
        Build.SUPPORTED_ABIS[0]
    else
        Build.CPU_ABI

    val arch = when {
        abi.contains("armeabi-v7a") -> "armeabi-v7a"
        abi.contains("x86_64") -> "x86_64"
        abi.contains("x86") -> "x86"
        abi.contains("arm64-v8a") -> "arm64-v8a"
        else -> null!!
    }

    val assetManager = assets
    val assetIn = assetManager.open("bin/$arch/$name")

    val bin = File("$filesDir/bin/$arch/$name")
    bin.parentFile?.mkdirs()
    if (!bin.exists())
        bin.createNewFile()
    val binOut = FileOutputStream(bin)

    // Copy executable
    var size: Long = 0
    val buff = ByteArray(1024)
    var nRead = assetIn.read(buff)

    while (nRead != -1) {
        binOut.write(buff, 0, nRead)
        size += nRead.toLong()
        nRead = assetIn.read(buff)
    }
    assetIn.close()
    binOut.close()

    bin.setExecutable(true)

    copiedBinaries[name] = bin

    return bin
}