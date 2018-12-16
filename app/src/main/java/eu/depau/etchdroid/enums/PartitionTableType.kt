package eu.depau.etchdroid.enums

import android.content.Context
import eu.depau.etchdroid.R

enum class PartitionTableType {
    AIX,
    AMIGA,
    BSD,
    DVH,
    GPT,
    LOOP,
    MAC,
    MSDOS,
    PC98,
    SUN;

    fun getString(context: Context): String {
        return when (this) {
            PartitionTableType.AIX -> context.getString(R.string.ptt_aix)
            PartitionTableType.AMIGA -> context.getString(R.string.ptt_amiga)
            PartitionTableType.BSD -> context.getString(R.string.ptt_bsd)
            PartitionTableType.DVH -> context.getString(R.string.ptt_dvh)
            PartitionTableType.GPT -> context.getString(R.string.ptt_gpt)
            PartitionTableType.LOOP -> context.getString(R.string.ptt_loop)
            PartitionTableType.MAC -> context.getString(R.string.ptt_mac)
            PartitionTableType.MSDOS -> context.getString(R.string.ptt_msdos)
            PartitionTableType.PC98 -> context.getString(R.string.ptt_pc98)
            PartitionTableType.SUN -> context.getString(R.string.ptt_sun)
        }
    }
}
