package eu.depau.etchdroid.utils

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import eu.depau.etchdroid.R
import kotlinx.android.synthetic.main.do_not_show_again.view.*


class DoNotShowAgainDialogFragment() : DialogFragment() {
    var title: String? = null
    var closeButton: String? = null
    var message: String? = null
    var listener: DialogListener? = null

    interface DialogListener {
        fun onDialogClose(dialog: DoNotShowAgainDialogFragment, showAgain: Boolean)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Build the dialog and set up the button click handlers
        val builder = AlertDialog.Builder(activity!!)
        val inflater = LayoutInflater.from(this.context)
        val dnsaLayout = inflater.inflate(R.layout.do_not_show_again, null)
        val doNotShowAgainCB = dnsaLayout.do_not_show_again

        builder
                .setTitle(title)
                .setMessage(message)
                .setView(dnsaLayout)
                .setPositiveButton(closeButton) { _, _ ->
                    listener?.onDialogClose(this@DoNotShowAgainDialogFragment, !doNotShowAgainCB.isChecked)
                }

        return builder.create()
    }

}