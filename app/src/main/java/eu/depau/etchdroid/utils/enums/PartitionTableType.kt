package eu.depau.etchdroid.utils.enums

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
            AIX -> context.getString(R.string.ptt_aix)
            AMIGA -> context.getString(R.string.ptt_amiga)
            BSD -> context.getString(R.string.ptt_bsd)
            DVH -> context.getString(R.string.ptt_dvh)
            GPT -> context.getString(R.string.ptt_gpt)
            LOOP -> context.getString(R.string.ptt_loop)
            MAC -> context.getString(R.string.ptt_mac)
            MSDOS -> context.getString(R.string.ptt_msdos)
            PC98 -> context.getString(R.string.ptt_pc98)
            SUN -> context.getString(R.string.ptt_sun)
        }
    }
}
