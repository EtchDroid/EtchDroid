package eu.depau.etchdroid.utils.reviews

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory

class WriteReviewHelper(private val mActivity: Activity) : IWriteReviewHelper {
    override val isGPlayFlavor: Boolean
        get() = true

    private var mReviewInfo: ReviewInfo? = null
    private var mLaunchASAP = false
    private var mFailed = false
    private var mReviewed = false

    private val mManager = ReviewManagerFactory.create(mActivity).apply {
        requestReviewFlow().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                mReviewInfo = task.result
            } else {
                mFailed = true
            }
            if (mLaunchASAP) {
                launchReviewFlow()
            }
        }
    }

    override fun launchReviewFlow() {
        if (mReviewInfo == null) {
            mLaunchASAP = true
            return
        }
        if (!mFailed && !mReviewed) {
            mManager.launchReviewFlow(mActivity, mReviewInfo!!)
            mReviewed = true
        } else {
            openFallback(mActivity)
        }
    }

    private fun openFallback(context: Context) {
        context.startActivity(
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=eu.depau.etchdroid")
            )
        )
    }
}