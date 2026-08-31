package app.pwhs.updater.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ReleaseDetails(
    val tagName: String,
    val versionName: String,
    val title: String? = null,
    val releaseNotes: String? = null,
    val publishedAt: Long? = null,
    val isPrerelease: Boolean = false,
    val assets: List<AssetArtifact> = emptyList(),
    val iconUrl: String? = null,
    val eTag: String? = null,
)
