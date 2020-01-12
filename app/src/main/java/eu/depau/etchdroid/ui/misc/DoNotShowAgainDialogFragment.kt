package eu.depau.etchdroid.ui.misc

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import eu.depau.etchdroid.R
import kotlinx.android.synthetic.main.do_not_show_again.view.*

@SuppressLint("ValidFragment")
class DoNotShowAgainDialogFragment(nightMode: Boolean) : DialogFragment() {
    var title: String? = null
    var positiveButton: String? = null
    var negativeButton: String? = null
    var message: String? = null
    var listener: DialogListener? = null
    val dialogTheme: Int = if (nightMode) R.style.DialogThemeDark else R.style.DialogThemeLight

    constructor() : this(false)

    init {
        setStyle(STYLE_NORMAL, dialogTheme)
    }

    interface DialogListener {
        fun onDialogPositive(dialog: DoNotShowAgainDialogFragment, showAgain: Boolean)
        fun onDialogNegative(dialog: DoNotShowAgainDialogFragment, showAgain: Boolean)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Build the dialog and set up the button click handlers
        val builder = AlertDialog.Builder(activity!!, dialogTheme)
        val inflater = LayoutInflater.from(this.context)
        val dnsaLayout = inflater.inflate(R.layout.do_not_show_again, null)
        val doNotShowAgainCB = dnsaLayout.do_not_show_again

        builder
                .setTitle(title)
                .setMessage(message)
                .setView(dnsaLayout)
                .setPositiveButton(positiveButton) { _, _ ->
                    listener?.onDialogPositive(this@DoNotShowAgainDialogFragment, !doNotShowAgainCB.isChecked)
                }

        if (negativeButton != null)
            builder.setNegativeButton(negativeButton) { _, _ ->
                listener?.onDialogNegative(this@DoNotShowAgainDialogFragment, !doNotShowAgainCB.isChecked)
            }

        return builder.create()
    }

}