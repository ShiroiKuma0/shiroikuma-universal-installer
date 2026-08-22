package app.pwhs.universalinstaller.review

import android.app.Activity

/**
 * The one seam between the app and Google's in-app review sheet.
 *
 * Same shape, and for the same reason, as `Telemetry`: the Play In-App Review library is a closed
 * Play Services artifact, so the `opensource` flavor binds this to [NoOpReviewPrompter] and ships
 * none of it. Both bindings come from `createReviewPrompter`, which each flavor source set
 * defines under this package. See docs/REVIEW.md.
 *
 * Consequence for callers: every call here has to be safe — and pointless — on a build that has
 * no sheet to show. Never branch on the result, and never make the user wait for it: Play decides
 * whether the sheet appears at all, and never tells us whether it did.
 */
interface ReviewPrompter {

    /**
     * Fetches the review flow ahead of time. Worth calling a moment before [launch] because the
     * request is network-backed, while the moment we want to launch on lasts a few hundred ms.
     */
    suspend fun prepare()

    /** Shows the sheet, if Play feels like it. Silently does nothing when it doesn't. */
    suspend fun launch(activity: Activity)
}

object NoOpReviewPrompter : ReviewPrompter {
    override suspend fun prepare() = Unit
    override suspend fun launch(activity: Activity) = Unit
}

object AppReview {

    @Volatile
    private var prompter: ReviewPrompter = NoOpReviewPrompter

    /** Binds the flavor's prompter. Called once from `App.onCreate`. */
    fun install(prompter: ReviewPrompter) {
        this.prompter = prompter
    }

    /**
     * True when this build can show a review sheet at all, i.e. it is a `play` build.
     *
     * This is about the build, not about whether Play will actually show anything — that we
     * cannot know. [ReviewGate] uses it to avoid keeping counters nobody will ever read.
     */
    val isAvailable: Boolean get() = prompter !== NoOpReviewPrompter

    suspend fun prepare() = prompter.prepare()

    suspend fun launch(activity: Activity) = prompter.launch(activity)
}
