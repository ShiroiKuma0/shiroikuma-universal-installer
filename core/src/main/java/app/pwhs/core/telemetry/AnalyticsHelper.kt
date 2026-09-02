package app.pwhs.core.telemetry

import java.util.Locale
import kotlin.math.round

/**
 * Type-safe analytics logging helpers conforming to the Universal Installer Tracking Plan.
 */
object AnalyticsHelper {

    // ── Giai đoạn 1: Onboarding & Cấp Quyền ────────────────────────────────
    fun logPermissionRequested(permissionType: String, source: String) {
        Telemetry.event(
            TelemetryEvents.EVENT_PERMISSION_REQUESTED,
            TelemetryEvents.PARAM_PERMISSION_TYPE to permissionType,
            TelemetryEvents.PARAM_SOURCE to source
        )
    }

    fun logPermissionResult(permissionType: String, status: String) {
        Telemetry.event(
            TelemetryEvents.EVENT_PERMISSION_RESULT,
            TelemetryEvents.PARAM_PERMISSION_TYPE to permissionType,
            TelemetryEvents.PARAM_STATUS to status
        )
    }

    fun logOnboardingComplete(stepCount: Int, durationSec: Long) {
        Telemetry.event(
            TelemetryEvents.EVENT_ONBOARDING_COMPLETE,
            TelemetryEvents.PARAM_STEP_COUNT to stepCount,
            TelemetryEvents.PARAM_DURATION_SEC to durationSec
        )
    }

    // ── Giai đoạn 2: Phễu Cài đặt Cốt lõi ──────────────────────────────────
    fun logFilePicked(fileType: String, fileCount: Int, source: String) {
        Telemetry.event(
            TelemetryEvents.EVENT_FILE_PICKED,
            TelemetryEvents.PARAM_FILE_TYPE to fileType.normalizeFileType(),
            TelemetryEvents.PARAM_FILE_COUNT to fileCount,
            TelemetryEvents.PARAM_SOURCE to source
        )
        updatePreferredFileType(fileType)
    }

    fun logPackageParseResult(
        fileType: String,
        status: String,
        hasObb: Boolean,
        targetSdk: Int
    ) {
        Telemetry.event(
            TelemetryEvents.EVENT_PACKAGE_PARSE_RESULT,
            TelemetryEvents.PARAM_FILE_TYPE to fileType.normalizeFileType(),
            TelemetryEvents.PARAM_STATUS to status,
            TelemetryEvents.PARAM_HAS_OBB to hasObb,
            TelemetryEvents.PARAM_TARGET_SDK to targetSdk
        )
    }

    fun logInstallStarted(
        fileType: String,
        installMode: String,
        isSplit: Boolean,
        fileSizeBytes: Long
    ) {
        val sizeMb = if (fileSizeBytes > 0) round((fileSizeBytes.toDouble() / (1024 * 1024)) * 10.0) / 10.0 else 0.0
        Telemetry.event(
            TelemetryEvents.EVENT_INSTALL_STARTED,
            TelemetryEvents.PARAM_FILE_TYPE to fileType.normalizeFileType(),
            TelemetryEvents.PARAM_INSTALL_MODE to installMode,
            TelemetryEvents.PARAM_IS_SPLIT to isSplit,
            TelemetryEvents.PARAM_FILE_SIZE_MB to sizeMb
        )
        updateInstallerMode(installMode)
    }

    fun logInstallResult(
        fileType: String,
        status: String,
        errorCode: String? = null,
        installMode: String,
        durationMs: Long
    ) {
        val params = mutableMapOf<String, Any?>(
            TelemetryEvents.PARAM_FILE_TYPE to fileType.normalizeFileType(),
            TelemetryEvents.PARAM_STATUS to status,
            TelemetryEvents.PARAM_INSTALL_MODE to installMode,
            TelemetryEvents.PARAM_DURATION_MS to durationMs
        )
        if (!errorCode.isNullOrBlank()) {
            params[TelemetryEvents.PARAM_ERROR_CODE] = errorCode.take(40)
        }
        Telemetry.event(TelemetryEvents.EVENT_INSTALL_RESULT, params)
    }

    // ── Giai đoạn 3: Tính năng Bổ trợ & Quản lý ───────────────────────────
    fun logAppManagementAction(actionType: String) {
        Telemetry.event(
            TelemetryEvents.EVENT_APP_MANAGEMENT_ACTION,
            TelemetryEvents.PARAM_ACTION_TYPE to actionType
        )
    }

    fun logCacheCleanStarted(cacheSizeBytes: Long) {
        val sizeMb = round((cacheSizeBytes.toDouble() / (1024 * 1024)) * 10.0) / 10.0
        Telemetry.event(
            TelemetryEvents.EVENT_CACHE_CLEAN_STARTED,
            TelemetryEvents.PARAM_CACHE_SIZE_MB to sizeMb
        )
    }

    fun logDefaultInstallerAction(action: String) {
        Telemetry.event(
            TelemetryEvents.EVENT_DEFAULT_INSTALLER_ACTION,
            TelemetryEvents.PARAM_ACTION to action
        )
    }

    fun logShizukuStatusChanged(status: String) {
        Telemetry.event(
            TelemetryEvents.EVENT_SHIZUKU_STATUS_CHANGED,
            TelemetryEvents.PARAM_STATUS to status
        )
    }

    // ── Giai đoạn 4: Đánh giá & Giữ chân ──────────────────────────────────
    fun logReviewPromptTriggered(triggerReason: String, totalSuccessfulInstalls: Int) {
        Telemetry.event(
            TelemetryEvents.EVENT_REVIEW_PROMPT_TRIGGERED,
            TelemetryEvents.PARAM_TRIGGER_REASON to triggerReason,
            TelemetryEvents.PARAM_TOTAL_SUCCESSFUL_INSTALLS to totalSuccessfulInstalls
        )
    }

    fun logInAppReviewRequested(status: String, errorMessage: String? = null) {
        val params = mutableMapOf<String, Any?>(
            TelemetryEvents.PARAM_STATUS to status
        )
        if (!errorMessage.isNullOrBlank()) {
            params[TelemetryEvents.PARAM_ERROR_MESSAGE] = errorMessage.take(40)
        }
        Telemetry.event(TelemetryEvents.EVENT_IN_APP_REVIEW_REQUESTED, params)
    }

    fun logInAppReviewDismissed(durationSec: Long) {
        Telemetry.event(
            TelemetryEvents.EVENT_IN_APP_REVIEW_DISMISSED,
            TelemetryEvents.PARAM_DURATION_SEC to durationSec
        )
    }

    fun logManualRateClicked(source: String) {
        Telemetry.event(
            TelemetryEvents.EVENT_MANUAL_RATE_CLICKED,
            TelemetryEvents.PARAM_SOURCE to source
        )
    }

    // ── Giai đoạn 5: Chuyển đổi & Đơn hàng (Monetization) ─────────────────
    fun logPaywallViewed(source: String) {
        Telemetry.event(
            TelemetryEvents.EVENT_PAYWALL_VIEWED,
            TelemetryEvents.PARAM_SOURCE to source
        )
    }

    fun logInAppPurchase(productId: String, value: Double, currency: String = "USD") {
        Telemetry.event(
            TelemetryEvents.EVENT_IN_APP_PURCHASE,
            TelemetryEvents.PARAM_PRODUCT_ID to productId,
            TelemetryEvents.PARAM_VALUE to value,
            TelemetryEvents.PARAM_CURRENCY to currency
        )
    }

    // ── User Properties ───────────────────────────────────────────────────
    fun updateInstallerMode(mode: String) {
        Telemetry.setUserProperty(TelemetryEvents.PROPERTY_INSTALLER_MODE, mode)
    }

    fun updateIsDefaultInstaller(isDefault: Boolean) {
        Telemetry.setUserProperty(TelemetryEvents.PROPERTY_IS_DEFAULT_INSTALLER, isDefault.toString())
    }

    fun updateTotalInstallsTier(totalInstalls: Int) {
        val tier = when {
            totalInstalls == 0 -> TelemetryEvents.TIER_0
            totalInstalls in 1..5 -> TelemetryEvents.TIER_1_5
            totalInstalls in 6..20 -> TelemetryEvents.TIER_6_20
            else -> TelemetryEvents.TIER_20_PLUS
        }
        Telemetry.setUserProperty(TelemetryEvents.PROPERTY_TOTAL_INSTALLS_TIER, tier)
    }

    fun updatePreferredFileType(fileType: String) {
        Telemetry.setUserProperty(TelemetryEvents.PROPERTY_PREFERRED_FILE_TYPE, fileType.normalizeFileType())
    }

    private fun String.normalizeFileType(): String =
        substringAfterLast('.', "").lowercase(Locale.ROOT).ifEmpty { lowercase(Locale.ROOT) }
}
