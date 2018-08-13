package eu.depau.ddroid.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns


fun Uri.getFileName(context: Context): String? {
    val TAG = "UriGetFileNameExt"
    var result: String? = null

    if (this.scheme == "content") {
        val cursor = context.getContentResolver().query(this, null, null, null, null)
        cursor.use {
            if (it != null && it.moveToFirst()) {
                result = it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
            }
        }
    }
    if (result == null) {
        result = this.getPath()
        val cut = result!!.lastIndexOf('/')
        if (cut != -1) {
            result = result!!.substring(cut + 1)
        }
    }

    return result
}