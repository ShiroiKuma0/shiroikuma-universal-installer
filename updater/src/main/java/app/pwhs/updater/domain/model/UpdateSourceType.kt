package app.pwhs.updater.domain.model

enum class UpdateSourceType {
    GITHUB,
    GITLAB,
    CODEBERG,
    DIRECT,
    UNKNOWN;

    companion object {
        fun fromUrl(url: String): UpdateSourceType {
            val lower = url.lowercase().trim()
            return when {
                lower.contains("github.com") -> GITHUB
                lower.contains("gitlab.com") -> GITLAB
                lower.contains("codeberg.org") -> CODEBERG
                lower.endsWith(".apk") -> DIRECT
                else -> UNKNOWN
            }
        }
    }
}
