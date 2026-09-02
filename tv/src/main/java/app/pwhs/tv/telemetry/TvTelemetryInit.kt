package app.pwhs.tv.telemetry

import android.content.Context
import android.os.Bundle
import app.pwhs.core.telemetry.NoOpTelemetrySink
import app.pwhs.core.telemetry.Telemetry
import app.pwhs.core.telemetry.TelemetrySink

object TvTelemetryInit {
    fun init(context: Context) {
        val sink = createSink(context)
        Telemetry.install(sink)
    }

    private fun createSink(context: Context): TelemetrySink {
        return try {
            val faClass = Class.forName("com.google.firebase.analytics.FirebaseAnalytics")
            val fcClass = Class.forName("com.google.firebase.crashlytics.FirebaseCrashlytics")
            val getInstanceFa = faClass.getMethod("getInstance", Context::class.java)
            val getInstanceFc = fcClass.getMethod("getInstance")
            val fa = getInstanceFa.invoke(null, context) ?: return NoOpTelemetrySink
            val fc = getInstanceFc.invoke(null)
            ReflectiveFirebaseTelemetrySink(fa, fc)
        } catch (_: Throwable) {
            NoOpTelemetrySink
        }
    }

    private class ReflectiveFirebaseTelemetrySink(
        private val analytics: Any,
        private val crashlytics: Any?
    ) : TelemetrySink {
        private val logEventMethod = analytics.javaClass.getMethod("logEvent", String::class.java, Bundle::class.java)
        private val setUserPropertyMethod = analytics.javaClass.getMethod("setUserProperty", String::class.java, String::class.java)
        private val setAnalyticsEnabledMethod = analytics.javaClass.getMethod("setAnalyticsCollectionEnabled", Boolean::class.java)

        private val recordExceptionMethod = crashlytics?.javaClass?.getMethod("recordException", Throwable::class.java)
        private val logBreadcrumbMethod = crashlytics?.javaClass?.getMethod("log", String::class.java)
        private val setCrashlyticsEnabledMethod = crashlytics?.javaClass?.getMethod("setCrashlyticsCollectionEnabled", Boolean::class.java)
        private val setCustomKeyMethod = crashlytics?.javaClass?.getMethod("setCustomKey", String::class.java, String::class.java)

        override fun logEvent(name: String, params: Map<String, Any?>) {
            val bundle = if (params.isEmpty()) null else Bundle(params.size).apply {
                for ((k, v) in params) {
                    when (v) {
                        null -> Unit
                        is String -> putString(k, v)
                        is Int -> putLong(k, v.toLong())
                        is Long -> putLong(k, v)
                        is Float -> putDouble(k, v.toDouble())
                        is Double -> putDouble(k, v)
                        is Boolean -> putString(k, v.toString())
                        else -> putString(k, v.toString())
                    }
                }
            }
            runCatching { logEventMethod.invoke(analytics, name, bundle) }
        }

        override fun setUserProperty(name: String, value: String?) {
            runCatching { setUserPropertyMethod.invoke(analytics, name, value) }
            runCatching { setCustomKeyMethod?.invoke(crashlytics, name, value ?: "") }
        }

        override fun recordException(throwable: Throwable) {
            runCatching { recordExceptionMethod?.invoke(crashlytics, throwable) }
        }

        override fun breadcrumb(message: String) {
            runCatching { logBreadcrumbMethod?.invoke(crashlytics, message) }
        }

        override fun setCollectionEnabled(enabled: Boolean) {
            runCatching { setAnalyticsEnabledMethod.invoke(analytics, enabled) }
            runCatching { setCrashlyticsEnabledMethod?.invoke(crashlytics, enabled) }
        }
    }
}
