package eu.depau.etchdroid.ui.activities

import android.os.Bundle
import eu.depau.etchdroid.R
import kotlinx.android.synthetic.main.activity_error.*

class ErrorActivity : ActivityBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)
        val msg = intent.getStringExtra("error")
        error_message.text = msg

        if ("errno 88" in msg)
            troubleshooting_info.text = getString(R.string.troubleshoot_sock_op_on_non_sock)
        else
            troubleshooting_info.text = getString(R.string.unknown_error)
    }
}
