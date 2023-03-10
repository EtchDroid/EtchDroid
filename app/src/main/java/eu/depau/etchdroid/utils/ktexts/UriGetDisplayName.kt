package eu.depau.etchdroid.utils.ktexts

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns

const val TAG = "UriGetFileNameExt"

fun Uri.getFileName(context: Context): String? {
    var result: String? = null

    if (this.scheme == "content") {
        val cursor = context.contentResolver.query(this, null, null, null, null)
        cursor.use {
            if (it != null && it.moveToFirst()) {
                val colIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                result = it.getString(colIndex)
            }
        }
    }
    if (result == null) {
        result = this.path
        val cut = result!!.lastIndexOf('/')
        if (cut != -1) {
            result = result!!.substring(cut + 1)
        }
    }

    return result
}