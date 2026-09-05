package app.pwhs.tv.presentation.settings

import android.app.Application
import android.content.pm.PackageManager
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.pwhs.core.data.local.dataStore
import app.pwhs.tv.R
import app.pwhs.tv.install.TvShizuku
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class ShizukuSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    val enabled = context.dataStore.data.map { it[TvShizuku.enabledKey] ?: false }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    private val _status = MutableStateFlow(TvShizuku.status(context))
    val status: kotlinx.coroutines.flow.StateFlow<TvShizuku.Status> = _status.asStateFlow()
    private val _message = MutableStateFlow<Int?>(null)
    val message = _message.asStateFlow()
    private var requesting = false
    private val received = Shizuku.OnBinderReceivedListener { refresh() }
    private val dead = Shizuku.OnBinderDeadListener { refresh() }
    private val permission = Shizuku.OnRequestPermissionResultListener { code, grant ->
        if (code == TvShizuku.PERMISSION_REQUEST && requesting) {
            requesting = false
            refresh()
            if (grant == PackageManager.PERMISSION_GRANTED) setEnabled(true)
            else _message.value = R.string.tv_shizuku_denied
        }
    }

    init {
        Shizuku.addBinderReceivedListenerSticky(received)
        Shizuku.addBinderDeadListener(dead)
        Shizuku.addRequestPermissionResultListener(permission)
    }

    fun refresh() { _status.value = TvShizuku.status(context) }

    fun toggle() {
        _message.value = null
        refresh()
        if (enabled.value) {
            setEnabled(false)
        } else when (status.value) {
            TvShizuku.Status.Ready -> setEnabled(true)
            TvShizuku.Status.PermissionRequired -> {
                if (requesting) return
                requesting = true
                try {
                    Shizuku.requestPermission(TvShizuku.PERMISSION_REQUEST)
                } catch (_: Exception) {
                    requesting = false
                    _message.value = R.string.tv_shizuku_error
                }
            }
            else -> Unit
        }
    }

    private fun setEnabled(value: Boolean) {
        viewModelScope.launch { context.dataStore.edit { it[TvShizuku.enabledKey] = value } }
    }

    override fun onCleared() {
        Shizuku.removeBinderReceivedListener(received)
        Shizuku.removeBinderDeadListener(dead)
        Shizuku.removeRequestPermissionResultListener(permission)
    }
}
