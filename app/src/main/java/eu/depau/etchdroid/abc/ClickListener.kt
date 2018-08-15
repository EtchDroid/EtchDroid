package eu.depau.etchdroid.abc

import android.view.View

interface ClickListener {
    fun onClick(view: View, position: Int)

    fun onLongClick(view: View, position: Int)
}