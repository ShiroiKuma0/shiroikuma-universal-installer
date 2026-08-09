package app.pwhs.universalinstaller.telemetry

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * The Play build reports to Firebase. The `opensource` source set defines the same function
 * against [NoOpTelemetrySink]; keep the signatures identical.
 */
fun createTelemetrySink(context: Context): TelemetrySink = FirebaseTelemetrySink(context)

private class FirebaseTelemetrySink(context: Context) : TelemetrySink {

    private val analytics = FirebaseAnalytics.getInstance(context)
    private val crashlytics = FirebaseCrashlytics.getInstance()

    override fun logEvent(name: String, params: Map<String, Any?>) {
        analytics.logEvent(name, if (params.isEmpty()) null else params.toBundle())
    }

    override fun setUserProperty(name: String, value: String?) {
        analytics.setUserProperty(name, value)
        // Mirrored onto the crash report: knowing a crash only happens on one install backend
        // is usually the whole diagnosis.
        crashlytics.setCustomKey(name, value ?: "")
    }

    override fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    override fun breadcrumb(message: String) {
        crashlytics.log(message)
    }

    override fun setCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
        // Method call rather than the synthesised property: Crashlytics overloads this on
        // Boolean? as well, and the explicit form is unambiguous about which one we mean.
        crashlytics.setCrashlyticsCollectionEnabled(enabled)
    }

    /**
     * Analytics parameters are limited to strings and numbers. Booleans and anything unexpected
     * go in as their string form rather than being dropped, so a mistyped call still shows up in
     * the console instead of vanishing.
     */
    private fun Map<String, Any?>.toBundle(): Bundle = Bundle(size).also { bundle ->
        for ((key, value) in this) {
            when (value) {
                null -> Unit
                is String -> bundle.putString(key, value)
                is Int -> bundle.putLong(key, value.toLong())
                is Long -> bundle.putLong(key, value)
                is Float -> bundle.putDouble(key, value.toDouble())
                is Double -> bundle.putDouble(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
    }
}
