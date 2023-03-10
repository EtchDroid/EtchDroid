package eu.depau.etchdroid.utils.reviews

interface IWriteReviewHelper {
    val isGPlayFlavor: Boolean
    fun launchReviewFlow()
}