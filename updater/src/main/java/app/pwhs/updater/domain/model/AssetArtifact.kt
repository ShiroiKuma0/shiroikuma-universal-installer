package app.pwhs.updater.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AssetArtifact(
    val name: String,
    val downloadUrl: String,
    val sizeBytes: Long = 0L,
    val contentType: String? = null,
)
