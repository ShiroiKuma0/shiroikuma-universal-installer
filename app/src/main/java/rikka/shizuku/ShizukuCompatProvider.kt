package rikka.shizuku

import android.os.Bundle
import timber.log.Timber

/**
 * Our `ShizukuProvider`, replacing the library's in the manifest so the app can accept **either**
 * flavour of binder hand-off.
 *
 * A Shizuku server pushes the binder by calling this provider once per envelope flavour it knows,
 * stopping at the first call that returns a reply. 白い熊 雫 sends `rikka.shizuku.BinderContainer`
 * (key `rikka.shizuku.intent.extra.BINDER`) before the legacy `moe.shizuku.api.BinderContainer`
 * (key `moe.shizuku.privileged.api.intent.extra.BINDER`). The library — `dev.rikka.shizuku
 * :provider:13.1.5`, the newest ever published — only reads the legacy key, and worse, reading ANY
 * key unparcels the WHOLE Bundle, so the modern envelope blew up with a ClassNotFoundException
 * before the legacy path could ever run. The binder never arrived and Shizuku looked permanently
 * "not running" even with the service up and the permission granted.
 *
 * So: handle the modern envelope here (its class now exists — see [BinderContainer]) and delegate
 * everything else, legacy envelope included, to the library. The class lives in `rikka.shizuku`
 * only to sit beside the container it belongs with; [Shizuku.onBinderReceived] is public API.
 *
 * Not handled: `af.shizuku.plus.api.intent.extra.BINDER`, which 白い熊 雫 sends only to its own
 * manager app — we are never that package.
 */
class ShizukuCompatProvider : ShizukuProvider() {

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        if (method == METHOD_SEND_BINDER && extras != null) {
            val binder = try {
                extras.classLoader = BinderContainer::class.java.classLoader
                @Suppress("DEPRECATION")
                (extras.getParcelable(EXTRA_BINDER_RIKKA) as? BinderContainer)?.binder
            } catch (t: Throwable) {
                // Not this flavour (or a container class we don't carry) — let the library try.
                Timber.d(t, "sendBinder: not a rikka.shizuku.BinderContainer envelope")
                null
            }
            if (binder != null) {
                Timber.d("Shizuku binder received (rikka.shizuku.BinderContainer)")
                Shizuku.onBinderReceived(binder, context?.packageName)
                return Bundle()
            }
        }
        return super.call(method, arg, extras)
    }

    private companion object {
        const val METHOD_SEND_BINDER = "sendBinder"
        const val EXTRA_BINDER_RIKKA = "rikka.shizuku.intent.extra.BINDER"
    }
}
