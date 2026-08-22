package app.pwhs.universalinstaller.review

import android.app.Activity
import android.content.Context
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import timber.log.Timber

/**
 * The Play build asks through Google's in-app review sheet. The `opensource` source set defines
 * the same function against [NoOpReviewPrompter]; keep the signatures identical.
 */
fun createReviewPrompter(context: Context): ReviewPrompter = PlayReviewPrompter(context)

private class PlayReviewPrompter(context: Context) : ReviewPrompter {

    private val manager = ReviewManagerFactory.create(context)

    /**
     * Single-use: Play invalidates a [ReviewInfo] once it has been launched with, so this is
     * cleared on use rather than kept around for the next ask.
     */
    @Volatile
    private var pending: ReviewInfo? = null

    override suspend fun prepare() {
        if (pending != null) return
        pending = request()
    }

    override suspend fun launch(activity: Activity) {
        // Falls back to requesting inline when nothing was pre-warmed — a slower ask beats a
        // skipped one, and the sheet is Play's to schedule anyway.
        val info = pending ?: request() ?: return
        pending = null
        runCatching { manager.launchReview(activity, info) }
            // Includes the ordinary case of Play declining to show anything: it reports success
            // either way, so a failure here is a real one and worth a breadcrumb.
            .onFailure { Timber.w(it, "In-app review flow did not run") }
    }

    private suspend fun request(): ReviewInfo? = runCatching { manager.requestReview() }
        .onFailure { Timber.w(it, "Could not request the in-app review flow") }
        .getOrNull()
}
