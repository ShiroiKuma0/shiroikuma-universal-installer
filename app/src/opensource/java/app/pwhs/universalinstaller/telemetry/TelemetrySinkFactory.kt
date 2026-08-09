package app.pwhs.universalinstaller.telemetry

import android.content.Context

/**
 * The open-source build reports nothing, anywhere. This is the whole of its telemetry.
 *
 * The `play` source set defines the same function against Firebase; keep the signatures
 * identical so `App.onCreate` compiles unchanged on both flavors.
 */
fun createTelemetrySink(context: Context): TelemetrySink = NoOpTelemetrySink
