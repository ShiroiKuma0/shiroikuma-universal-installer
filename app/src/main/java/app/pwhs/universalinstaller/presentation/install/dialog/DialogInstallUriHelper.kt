package app.pwhs.universalinstaller.presentation.install.dialog

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import app.pwhs.universalinstaller.presentation.install.InstallViewModel
import app.pwhs.universalinstaller.presentation.install.util.InstallApkSplitsHelper
import app.pwhs.universalinstaller.util.extension.getDisplayName

object DialogInstallUriHelper {

    fun collectIncomingUris(source: Intent?): List<Uri> {
        if (source == null) return emptyList()
        val out = mutableListOf<Uri>()

        // 1. Data URI (VIEW / INSTALL_PACKAGE)
        source.data?.takeIf { it.scheme == "content" || it.scheme == "file" }?.let(out::add)

        // 2. EXTRA_STREAM (SEND / SEND_MULTIPLE)
        @Suppress("DEPRECATION")
        when (source.action) {
            Intent.ACTION_SEND ->
                (source.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri)?.let(out::add)
            Intent.ACTION_SEND_MULTIPLE ->
                source.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    ?.filterNotNull()
                    ?.let(out::addAll)
        }

        // 3. ClipData (Alternative for some file managers)
        source.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) {
                val u = clip.getItemAt(i).uri ?: continue
                if (u.scheme == "content" || u.scheme == "file") out.add(u)
            }
        }

        return out.distinct()
    }

    fun forwardIncomingUris(source: Intent?, target: Intent) {
        if (source == null) return
        val uris = collectIncomingUris(source)
        if (uris.isEmpty()) return
        if (uris.size == 1) {
            target.data = uris.first()
        } else {
            val clip = ClipData.newRawUri("", uris.first())
            for (i in 1 until uris.size) {
                clip.addItem(ClipData.Item(uris[i]))
            }
            target.clipData = clip
        }
        target.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    fun parseAndPush(context: Context, uri: Uri, viewModel: InstallViewModel) {
        val displayName = context.contentResolver.getDisplayName(uri)
        val ext = displayName.substringAfterLast('.', "").lowercase()
        val splitProvider = InstallApkSplitsHelper.buildSplitProvider(context, uri, ext)
        viewModel.parseApkInfo(context, uri, splitProvider, displayName)
    }
}
