package eu.depau.ddroid.utils

// https://stackoverflow.com/a/3758880/1124621

private fun <T> humanReadableByteCount(bytes: T, si: Boolean = false): String where T : Comparable<T>, T : Number {
    val unit: Long = if (si) 1000 else 1024
    if (bytes.toLong() < unit) return bytes.toString() + " B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit.toDouble())).toInt()
    val pre = (if (si) "kMGTPE" else "KMGTPE")[exp - 1] + if (si) "" else "i"
    return String.format("%.1f %sB", bytes.toDouble() / Math.pow(unit.toDouble(), exp.toDouble()), pre)
}

fun Long.toHRSize(si: Boolean = false) = humanReadableByteCount(this, si)
fun Float.toHRSize(si: Boolean = false) = humanReadableByteCount(this, si)
fun Double.toHRSize(si: Boolean = false) = humanReadableByteCount(this, si)
fun Int.toHRSize(si: Boolean = false) = humanReadableByteCount(this, si)
fun Byte.toHRSize(si: Boolean = false) = humanReadableByteCount(this, si)
fun Short.toHRSize(si: Boolean = false) = humanReadableByteCount(this, si)