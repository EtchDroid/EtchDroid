package eu.depau.etchdroid.utils.reviews

import android.app.Activity
import android.content.Intent
import android.net.Uri

class WriteReviewHelper(private val mActivity: Activity) : IWriteReviewHelper {
    override val isGPlayFlavor: Boolean
        get() = false

    override fun launchReviewFlow() {
        mActivity.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://github.com/EtchDroid/EtchDroid/")
            )
        )
    }
}