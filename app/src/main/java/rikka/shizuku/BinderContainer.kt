package rikka.shizuku

import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable

/**
 * The `rikka.shizuku.BinderContainer` flavour of Shizuku's binder envelope — declared HERE because
 * no released client library has it.
 *
 * When a Shizuku server hands an app its binder it calls the app's `ShizukuProvider` with a Bundle
 * holding one Parcelable that wraps the binder, and the *class name* travels on the wire. Current
 * Shizuku servers (白い熊 雫 among them) send this class first; the newest published client library,
 * `dev.rikka.shizuku:provider:13.1.5` from 2023, only knows the older `moe.shizuku.api`
 * .BinderContainer. Unparcelling therefore threw inside our provider:
 *
 *     BadParcelableException: ClassNotFoundException when unmarshalling: rikka.shizuku.BinderContainer
 *         at rikka.shizuku.ShizukuProvider.call
 *
 * and the binder was dropped — which is why the app kept reporting "Shizuku isn't running" while the
 * service was up. With this class present the parcel resolves and [ShizukuCompatProvider] can pull
 * the binder out. The parcel layout is a single strong binder, identical in every flavour, so this
 * stays wire-compatible with both the old and the new senders.
 *
 * The class name is load-bearing: `Parcel.readParcelableCreator` resolves it by name, so R8 must not
 * rename it (see the `-keepnames` rule in `proguard-rules.pro`).
 */
class BinderContainer(@JvmField val binder: IBinder?) : Parcelable {

    private constructor(source: Parcel) : this(source.readStrongBinder())

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeStrongBinder(binder)
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<BinderContainer> = object : Parcelable.Creator<BinderContainer> {
            override fun createFromParcel(source: Parcel): BinderContainer = BinderContainer(source)
            override fun newArray(size: Int): Array<BinderContainer?> = arrayOfNulls(size)
        }
    }
}
