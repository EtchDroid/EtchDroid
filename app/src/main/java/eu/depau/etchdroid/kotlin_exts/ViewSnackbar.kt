package eu.depau.etchdroid.kotlin_exts

import com.google.android.material.snackbar.Snackbar
import android.view.View

fun View.snackbar(message: CharSequence, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(this, message, duration).show()
}