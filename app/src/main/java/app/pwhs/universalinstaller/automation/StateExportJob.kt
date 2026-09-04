package app.pwhs.universalinstaller.automation

import java.util.concurrent.atomic.AtomicReference

/**
 * The one [StateExportReceiver] export that may be running, and the flag that stops it.
 *
 * ## Why a single slot
 *
 * Contract §1 forbids two exports at once, and that is what makes `CANCEL_EXPORT` without a
 * `reply_id` unambiguous: "the export you are running" is only a meaningful phrase when there can
 * be at most one. The guard and the cancellation therefore belong to the same object.
 *
 * ## Why this is never persisted
 *
 * An "export in progress" flag written to disk wedges the app for good after a single crash: every
 * later request answers `ERROR:export already running` and no backup is possible until the process
 * is killed. It lives in memory, is claimed with a compare-and-set, and is released in a `finally`.
 */
object StateExportJob {

    /** One claimed slot. [cancelled] is polled at ZIP-entry boundaries, never mid-write. */
    class Run(val replyId: String) {
        @Volatile
        var cancelled: Boolean = false
            internal set
    }

    private val active = AtomicReference<Run?>(null)

    /** Claims the slot, or null when an export is already running. */
    fun begin(replyId: String): Run? {
        val run = Run(replyId)
        return if (active.compareAndSet(null, run)) run else null
    }

    /** Releases the slot. Always from a `finally` — see the class note on why. */
    fun finish(run: Run) {
        active.compareAndSet(run, null)
    }

    /**
     * Asks the running export to stop at its next write boundary.
     *
     * A blank [replyId] means whatever is running, per §1. A cancel that arrives when nothing is
     * running, or after the export already finished, or naming a different run, is a **silent
     * no-op** — not an error, not a reply, not a crash: 自由作業盤 fires it whenever 白い熊 presses
     * 中止, without knowing how far we got.
     */
    fun cancel(replyId: String?) {
        val run = active.get() ?: return
        if (replyId.isNullOrEmpty() || replyId == run.replyId) run.cancelled = true
    }
}
