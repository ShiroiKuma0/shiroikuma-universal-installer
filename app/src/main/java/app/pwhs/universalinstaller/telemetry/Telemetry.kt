package app.pwhs.universalinstaller.telemetry

/**
 * The one seam between the app and whatever is reporting analytics and crashes.
 *
 * Everything under `main` calls [Telemetry] and nothing else. The `opensource` flavor binds it
 * to [NoOpTelemetrySink] so that build ships no Google libraries at all; the `play` flavor binds
 * it to a Firebase-backed sink. Both bindings come from `createTelemetrySink`, which each flavor
 * source set defines under this same package.
 *
 * Consequence for callers: every call here has to be safe — and pointless — on a build that
 * collects nothing. Never branch on telemetry, never wait for it, and never pass anything that
 * identifies a user or a file they opened.
 */
interface TelemetrySink {

    /** Records a named event. Values must be [String], [Boolean], or a number. */
    fun logEvent(name: String, params: Map<String, Any?>)

    /** Sets a slow-moving property of the install, e.g. which backend it uses. */
    fun setUserProperty(name: String, value: String?)

    /** Reports a handled failure. Fatal crashes are captured by the reporter itself. */
    fun recordException(throwable: Throwable)

    /** Adds a line to the log attached to the next crash report. */
    fun breadcrumb(message: String)

    /**
     * Turns collection on or off wholesale, from the Settings toggle and from onboarding.
     *
     * Firebase persists this itself, so it survives a restart without us re-applying it —
     * we re-apply at startup anyway so that the preference, not Firebase's own store, is the
     * thing that decides.
     */
    fun setCollectionEnabled(enabled: Boolean)
}

object NoOpTelemetrySink : TelemetrySink {
    override fun logEvent(name: String, params: Map<String, Any?>) = Unit
    override fun setUserProperty(name: String, value: String?) = Unit
    override fun recordException(throwable: Throwable) = Unit
    override fun breadcrumb(message: String) = Unit
    override fun setCollectionEnabled(enabled: Boolean) = Unit
}

object Telemetry {

    @Volatile
    private var sink: TelemetrySink = NoOpTelemetrySink

    /**
     * Binds the flavor's sink. Called once from `App.onCreate`; until then — and forever on
     * `opensource` — every call below goes nowhere.
     */
    fun install(sink: TelemetrySink) {
        this.sink = sink
    }

    /**
     * True when this build has a reporting sink at all, i.e. it is a `play` build.
     *
     * This is about the build, not the user's choice — it stays true when the user turns
     * collection off. The UI uses it to decide whether a reporting toggle should exist,
     * because on `opensource` there is nothing to toggle.
     */
    val isCollecting: Boolean get() = sink !== NoOpTelemetrySink

    fun event(name: String, vararg params: Pair<String, Any?>) {
        sink.logEvent(name, if (params.isEmpty()) emptyMap() else params.toMap())
    }

    /** Shorthand for [TelemetryEvents.FEATURE_USED], which is most of the call sites. */
    fun feature(feature: String) {
        event(TelemetryEvents.FEATURE_USED, TelemetryEvents.PARAM_FEATURE to feature)
    }

    fun setUserProperty(name: String, value: String?) = sink.setUserProperty(name, value)

    fun recordException(throwable: Throwable) = sink.recordException(throwable)

    fun breadcrumb(message: String) = sink.breadcrumb(message)

    fun setCollectionEnabled(enabled: Boolean) = sink.setCollectionEnabled(enabled)
}
