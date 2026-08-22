package app.pwhs.universalinstaller.telemetry

/**
 * Every event and parameter name the app reports, in one place.
 *
 * Two rules hold for anything added here. Names are stable — Firebase keys its reports off the
 * literal string, so renaming one splits a metric in half. And nothing may describe *what* the
 * user installed: no package names, no app names, no file names, no URIs. What we want to learn
 * is which install backends work and where they fail, and that needs none of it.
 */
object TelemetryEvents {

    /** An install session was created. Pairs with [INSTALL_RESULT] via [PARAM_METHOD]. */
    const val INSTALL_STARTED = "install_started"

    /** An install session finished, successfully or not. See [PARAM_RESULT]. */
    const val INSTALL_RESULT = "install_result"

    /** Which backend ran the install: `default`, `shizuku`, `root`, `dhizuku`, `manual`. */
    const val PARAM_METHOD = "method"

    /** [RESULT_SUCCESS], [RESULT_FAILURE] or [RESULT_CANCELLED]. */
    const val PARAM_RESULT = "result"

    /** Stable failure kind from `InstallErrorHelper.failureKey`, absent on success. */
    const val PARAM_FAILURE = "failure"

    /** How many APKs the session committed — 1 for a plain APK, more for a split. */
    const val PARAM_APK_COUNT = "apk_count"

    const val RESULT_SUCCESS = "success"
    const val RESULT_FAILURE = "failure"
    const val RESULT_CANCELLED = "cancelled"

    /**
     * A privileged backend the user turned on was checked at startup. Reported once per cold
     * start per enabled backend, with [PARAM_METHOD] and [PARAM_HEALTHY].
     *
     * This is the number the install events can't give us. `install_result` only ever describes
     * installs that happened; someone who set up Shizuku, had it break, and gave up never
     * appears in it at all.
     */
    const val BACKEND_HEALTH = "backend_health"

    /** Whether the backend was actually usable, `true` or `false`. */
    const val PARAM_HEALTHY = "healthy"

    /**
     * The user tried to take over (or hand back) the system installer role, which needs Shizuku
     * or root and fails in device-specific ways. Carries [PARAM_METHOD], [PARAM_ENABLED] and
     * [PARAM_RESULT] — with [RESULT_BLOCKED] when no backend was available to even try.
     */
    const val DEFAULT_INSTALLER_SET = "default_installer_set"

    /** Whether the user was switching the role on or off. */
    const val PARAM_ENABLED = "enabled"

    /** No privileged backend was ready, so nothing was attempted. */
    const val RESULT_BLOCKED = "blocked"

    /**
     * A secondary feature was actually used — not merely opened. One event name with a
     * [PARAM_FEATURE] rather than one name per feature, so the whole surface reads as a single
     * comparable report, and so adding a feature doesn't burn another of Firebase's 500 event
     * names.
     */
    const val FEATURE_USED = "feature_used"

    /** One of the `FEATURE_*` constants below. */
    const val PARAM_FEATURE = "feature"

    const val FEATURE_LAN_SHARE = "lan_share"
    const val FEATURE_VIRUSTOTAL = "virustotal_scan"
    const val FEATURE_APK_BACKUP = "apk_backup"
    const val FEATURE_INSTALLER_PROFILE = "installer_profile"
    const val FEATURE_BATCH_INSTALL = "batch_install"
    const val FEATURE_OBB_COPY = "obb_copy"
    const val FEATURE_URL_DOWNLOAD = "url_download"
    const val FEATURE_UNINSTALL = "uninstall"

    /**
     * The in-app review sheet was asked for. Counts opportunities, not reviews: Play
     * decides whether anything is shown and never reports back, so there is no outcome to
     * record. Useful only for telling "the gate never opens" apart from "it opens and
     * nothing comes of it".
     */
    const val FEATURE_REVIEW_PROMPT = "review_prompt"

    /** User property: the install backend the user has selected in Settings. */
    const val PROPERTY_INSTALL_METHOD = "install_method"
}
