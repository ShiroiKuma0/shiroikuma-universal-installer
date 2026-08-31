package app.pwhs.updater.domain.model

enum class UpdateSourceType {
    GITHUB,
    GITLAB,
    CODEBERG,
    FDROID,
    DIRECT,
    UNKNOWN;

    companion object {
        fun fromUrl(url: String): UpdateSourceType {
            val lower = url.lowercase().trim()
            return when {
                lower.contains("github.com") -> GITHUB
                lower.contains("gitlab.com") -> GITLAB
                lower.contains("codeberg.org") || lower.contains("gitea") -> CODEBERG
                lower.contains("f-droid.org") || lower.contains("apt.izzysoft.de") -> FDROID
                lower.endsWith(".apk") || lower.contains("/download") -> DIRECT
                else -> UNKNOWN
            }
        }
    }
}
