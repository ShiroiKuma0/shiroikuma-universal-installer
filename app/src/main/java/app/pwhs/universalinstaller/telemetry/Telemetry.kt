package app.pwhs.universalinstaller.telemetry

typealias TelemetrySink = app.pwhs.core.telemetry.TelemetrySink
typealias NoOpTelemetrySink = app.pwhs.core.telemetry.NoOpTelemetrySink

object Telemetry {
    val isCollecting: Boolean get() = app.pwhs.core.telemetry.Telemetry.isCollecting
    fun install(sink: TelemetrySink) = app.pwhs.core.telemetry.Telemetry.install(sink)
    fun event(name: String, vararg params: Pair<String, Any?>) = app.pwhs.core.telemetry.Telemetry.event(name, *params)
    fun event(name: String, params: Map<String, Any?>) = app.pwhs.core.telemetry.Telemetry.event(name, params)
    fun feature(feature: String) = app.pwhs.core.telemetry.Telemetry.feature(feature)
    fun setUserProperty(name: String, value: String?) = app.pwhs.core.telemetry.Telemetry.setUserProperty(name, value)
    fun recordException(throwable: Throwable) = app.pwhs.core.telemetry.Telemetry.recordException(throwable)
    fun breadcrumb(message: String) = app.pwhs.core.telemetry.Telemetry.breadcrumb(message)
    fun setCollectionEnabled(enabled: Boolean) = app.pwhs.core.telemetry.Telemetry.setCollectionEnabled(enabled)
}
