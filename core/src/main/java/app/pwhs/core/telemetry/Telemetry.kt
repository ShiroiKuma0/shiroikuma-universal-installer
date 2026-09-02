package app.pwhs.core.telemetry

/**
 * The unified abstraction between the app/tv modules and whatever reports analytics and crashes.
 *
 * Open-source builds bind this to [NoOpTelemetrySink] so that no proprietary tracking SDKs run.
 * Play builds or configured builds bind it to a Firebase-backed sink.
 */
interface TelemetrySink {
    /** Records a named event. Values must be [String], [Boolean], or a number. */
    fun logEvent(name: String, params: Map<String, Any?>)

    /** Sets a user property, e.g. which installer backend is preferred. */
    fun setUserProperty(name: String, value: String?)

    /** Reports a handled failure. */
    fun recordException(throwable: Throwable)

    /** Adds a breadcrumb line to the next crash report. */
    fun breadcrumb(message: String)

    /** Enables or disables collection dynamically (e.g. from user consent/settings). */
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

    fun install(sink: TelemetrySink) {
        this.sink = sink
    }

    val isCollecting: Boolean get() = sink !== NoOpTelemetrySink

    fun event(name: String, vararg params: Pair<String, Any?>) {
        sink.logEvent(name, if (params.isEmpty()) emptyMap() else params.toMap())
    }

    fun event(name: String, params: Map<String, Any?>) {
        sink.logEvent(name, params)
    }

    fun feature(feature: String) {
        event(TelemetryEvents.FEATURE_USED, TelemetryEvents.PARAM_FEATURE to feature)
    }

    fun setUserProperty(name: String, value: String?) = sink.setUserProperty(name, value)

    fun recordException(throwable: Throwable) = sink.recordException(throwable)

    fun breadcrumb(message: String) = sink.breadcrumb(message)

    fun setCollectionEnabled(enabled: Boolean) = sink.setCollectionEnabled(enabled)
}
