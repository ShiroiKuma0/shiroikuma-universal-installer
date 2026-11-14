package app.pwhs.universalinstaller.domain.model

enum class VtStatus {
    CLEAN,
    MALICIOUS,
    SUSPICIOUS,
    NOT_FOUND,
    /**
     * VirusTotal has the file but no engine has reported on it yet: right after an upload,
     * `GET /files/{sha}` answers 200 with empty stats. Never a verdict — read as "not analysed".
     */
    NOT_ANALYZED,
    NO_API_KEY,
    /** HTTP 401/403 — the key is wrong, revoked, or lacks access. Distinct from a missing key. */
    INVALID_API_KEY,
    /** HTTP 429 — free tier allows 4 requests/minute and 500/day. Retrying later works. */
    RATE_LIMITED,
    ERROR,
    TOO_LARGE,
    SCANNING,    // hashing + hash lookup
    UPLOADING,   // posting bytes to VirusTotal
    QUEUED,      // VT received the file and queued it
    ANALYZING,   // VT engines are running
}

/**
 * Statuses that say nothing yet about whether the file is safe. Auto-approve waits on these
 * rather than reading them as "no risk" — an unknown file must never install silently. `null`
 * counts too: it is the moment between parse and the hash lookup starting.
 */
fun VtStatus?.isPendingForAutoApprove(): Boolean = this == null || this in setOf(
    VtStatus.SCANNING,
    VtStatus.NOT_FOUND,
    VtStatus.NOT_ANALYZED,
    VtStatus.UPLOADING,
    VtStatus.QUEUED,
    VtStatus.ANALYZING,
)

/**
 * Per-engine scan result from VirusTotal's `last_analysis_results`.
 * @param engineName  e.g. "Kaspersky", "Avira", "ClamAV"
 * @param category    one of "malicious", "suspicious", "harmless", "undetected", "type-unsupported", "timeout", "failure"
 * @param result      detection label when positive, e.g. "Trojan.GenericKD.12345"; null/blank when clean
 */
data class VtEngineResult(
    val engineName: String,
    val category: String,
    val result: String?,
)

data class VtResult(
    val malicious: Int = 0,
    val suspicious: Int = 0,
    val harmless: Int = 0,
    val undetected: Int = 0,
    val status: VtStatus = VtStatus.NO_API_KEY,
    val errorMessage: String = "",
    val uploadProgress: Int = 0,   // 0..100, meaningful only when status = UPLOADING
    val analysisId: String = "",
    val engineResults: List<VtEngineResult> = emptyList(),
)
