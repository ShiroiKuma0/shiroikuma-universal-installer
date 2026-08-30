package app.pwhs.updater.domain.provider

import app.pwhs.updater.domain.model.ReleaseDetails

interface UpdateSourceProvider {
    fun canHandle(url: String): Boolean

    /**
     * Fetches the latest release information from [url].
     * Returns null in Result if HTTP 304 Not Modified (eTag matched).
     */
    suspend fun fetchLatestRelease(
        url: String,
        includePrereleases: Boolean = false,
        eTag: String? = null,
        apiToken: String? = null,
    ): Result<ReleaseDetails?>
}
