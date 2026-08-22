package app.pwhs.universalinstaller.review

import android.content.Context

/**
 * The open-source build never asks for a review. There is nothing to ask through: the in-app
 * review sheet belongs to the Play Store app, and this build ships no Play Services libraries.
 *
 * The `play` source set defines the same function against Google's ReviewManager; keep the
 * signatures identical so `App.onCreate` compiles unchanged on both flavors.
 */
fun createReviewPrompter(context: Context): ReviewPrompter = NoOpReviewPrompter
