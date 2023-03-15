package eu.depau.etchdroid.utils.ktexts

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

// https://github.com/android-rcs/rcsjta/blob/master/RI/src/com/gsma/rcs/ri/utils/FileUtils.java#L214

fun Uri.getFileSize(context: Context): Long {
    when (this.scheme) {
        ContentResolver.SCHEME_FILE -> {
            val f = File(this.getFilePath(context)!!)
            return f.length()
        }

        ContentResolver.SCHEME_CONTENT -> {
            val cursor: Cursor? = context.contentResolver.query(this, null, null, null, null)

            cursor.use {
                if (it == null) {
                    throw SQLException("Failed to query file $this")
                }
                return if (it.moveToFirst()) {
                    java.lang.Long.valueOf(it.getString(it
                            .getColumnIndexOrThrow(OpenableColumns.SIZE)))
                } else {
                    throw IllegalArgumentException(
                            "Error in retrieving this size form the URI")
                }
            }
        }

        else -> throw IllegalArgumentException("Unsupported URI scheme")
    }
}