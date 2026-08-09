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

    /** User property: the install backend the user has selected in Settings. */
    const val PROPERTY_INSTALL_METHOD = "install_method"
}
