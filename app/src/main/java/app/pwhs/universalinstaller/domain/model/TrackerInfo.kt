package app.pwhs.universalinstaller.domain.model

data class TrackerInfo(
    val id: Int,
    val name: String,
    val categories: List<String> = emptyList(),
    val website: String? = null,
)
