package app.pwhs.tv.install

/** Resolve once per install; never probe root when the chosen Shizuku service is ready. */
internal enum class TvInstallBackend {
    Shizuku, Root, System;

    companion object {
        suspend fun select(
            shizukuEnabled: Boolean,
            rootEnabled: Boolean,
            shizukuReady: () -> Boolean,
            rootReady: suspend () -> Boolean,
        ): TvInstallBackend = when {
            shizukuEnabled && shizukuReady() -> Shizuku
            rootEnabled && rootReady() -> Root
            else -> System
        }
    }
}
