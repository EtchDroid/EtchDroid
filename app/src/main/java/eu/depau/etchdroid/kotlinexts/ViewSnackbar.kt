package eu.depau.etchdroid.kotlinexts

import android.view.View
import com.google.android.material.snackbar.Snackbar

fun View.snackbar(message: CharSequence, duration: Int = Snackbar.LENGTH_LONG) {
    Snackbar.make(this, message, duration).show()
}