package eu.depau.etchdroid.utils

import android.net.Uri

data class License(
        val name: String,
        val url: Uri,
        val license: String,
        val description: String? = null
)