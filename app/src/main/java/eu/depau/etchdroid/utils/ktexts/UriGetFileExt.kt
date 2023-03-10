package eu.depau.etchdroid.utils.ktexts

import android.content.ContentResolver
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File

fun Uri.getExtension(contentResolver: ContentResolver): String {
    return when (scheme) {
        ContentResolver.SCHEME_CONTENT -> {
            val mime = MimeTypeMap.getSingleton()
            mime.getExtensionFromMimeType(contentResolver.getType(this))!!
        }
        else -> MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(File(path!!)).toString())
    }
}