package app.pwhs.universalinstaller.presentation.install.controller

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import app.pwhs.universalinstaller.util.MicroGCompat
import timber.log.Timber

class MicroGInstallActivity : Activity() {

    companion object {
        private const val REQUEST_CODE = 4201
        private const val EXTRA_URIS = "extra_uris"
        private const val EXTRA_PACKAGE_NAME = "extra_package_name"
        private const val EXTRA_SESSION_ID = "extra_session_id"

        fun start(context: Context, uris: List<Uri>, packageName: String, sessionId: String? = null) {
            val uriList = ArrayList(uris)
            for (uri in uriList) {
                try {
                    context.grantUriPermission(
                        MicroGCompat.PACKAGE_NAME_MICROG_VENDING,
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                } catch (t: Throwable) {
                    Timber.w(t, "Failed to grant URI permission to microG")
                }
            }
            val intent = Intent(context, MicroGInstallActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putParcelableArrayListExtra(EXTRA_URIS, uriList)
                putExtra(EXTRA_PACKAGE_NAME, packageName)
                putExtra(EXTRA_SESSION_ID, sessionId)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uris: ArrayList<Uri>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(EXTRA_URIS, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(EXTRA_URIS)
        }

        if (uris.isNullOrEmpty()) {
            Timber.e("MicroGInstallActivity: No URIs provided")
            finish()
            return
        }

        val microGIntent = MicroGCompat.buildInstallIntent(uris)
        try {
            startActivityForResult(microGIntent, REQUEST_CODE)
        } catch (t: Throwable) {
            Timber.e(t, "MicroGInstallActivity: failed to launch microG install intent")
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE) {
            val error = data?.getStringExtra("error")
            Timber.d("MicroGInstallActivity finished: resultCode=%d, error=%s", resultCode, error)
        }
        finish()
    }
}
