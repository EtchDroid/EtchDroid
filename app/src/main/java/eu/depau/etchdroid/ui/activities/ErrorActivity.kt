package eu.depau.etchdroid.ui.activities

import android.os.Build
import android.os.Bundle
import eu.depau.etchdroid.BuildConfig
import eu.depau.etchdroid.R
import kotlinx.android.synthetic.main.activity_error.*

class ErrorActivity : ActivityBase() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_error)
        val msg = intent.getStringExtra("error")
        error_message.text = msg
        gh_issue_template.text = """
            ### Error message
            
            ```
            $msg
            ```
            
            ### Device info
            
            - Android version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            - Manufacturer: ${Build.BRAND} (${Build.MANUFACTURER})
            - Model: ${Build.MODEL} (${Build.PRODUCT}, hw name: ${Build.HARDWARE}, board: ${Build.BOARD})
            - Build fingerprint: ${Build.FINGERPRINT}
            
            ### App build info
            
            - Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})
            - Installed from: ${packageManager.getInstallerPackageName(BuildConfig.APPLICATION_ID)}
            - Build type: ${BuildConfig.BUILD_TYPE} (debug: ${BuildConfig.DEBUG})
            
            ### Stack trace
            
            ```
            """.trimIndent() +
                intent.getStringExtra("stacktrace") +
                "```"
    }
}
