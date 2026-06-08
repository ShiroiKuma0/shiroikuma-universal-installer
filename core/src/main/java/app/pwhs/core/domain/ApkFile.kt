package app.pwhs.core.domain

/** An installable APK/bundle discovered on the device (e.g. in Downloads via MediaStore). */
data class ApkFile(
    val uri: String,
    val displayName: String,
    val sizeBytes: Long,
    val isBundle: Boolean,
)
