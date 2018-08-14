package eu.depau.ddroid.utils

private val timeStrings = arrayOf("s", "m", "h", "d")
private val timeDivs = arrayOf(60, 60, 24)

fun <T> humanReadableTimeDelta(time: T): String where T : Number {
    var dbTime = time.toDouble() / 1000.0
    var outString = ""

    for (i in 0..(timeDivs.size - 1)) {
        val div = timeDivs[i]
        val str = timeStrings[i]

        outString = "${(dbTime % div).toInt()}$str$outString"

        if (dbTime < div)
            return outString

        outString = " $outString"
        dbTime /= div
    }

    return "${dbTime.toInt()}${timeStrings[-1]} $outString"
}

fun Long.toHRTime() = humanReadableTimeDelta(this)
fun Float.toHRTime() = humanReadableTimeDelta(this)
fun Double.toHRTime() = humanReadableTimeDelta(this)
fun Int.toHRTime() = humanReadableTimeDelta(this)
fun Byte.toHRTime() = humanReadableTimeDelta(this)
fun Short.toHRTime() = humanReadableTimeDelta(this)