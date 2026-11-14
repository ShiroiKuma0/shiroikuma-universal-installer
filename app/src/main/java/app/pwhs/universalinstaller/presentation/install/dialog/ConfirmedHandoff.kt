package app.pwhs.universalinstaller.presentation.install.dialog

import app.pwhs.universalinstaller.domain.model.ApkInfo
import app.pwhs.universalinstaller.domain.model.SplitType

/**
 * The install the user already confirmed in 応用管理 (`shiroikuma.extra.INSTALL_CONFIRMED`, see
 * `VtHandoff`; 白い熊, 2026-11-14). 応用管理's VirusTotal dialog shows the same facts as the Prepare
 * screen here — icon, label, package, installed version → new version, size, and whether this is a
 * new install, an update, the same version or a downgrade — and its main button is the install
 * decision. Asking again here would be the same question twice, so a confirmed hand-off goes from
 * loading straight to installing.
 *
 * Seamless means "no redundant question", not "no question ever". Everything below is a decision
 * 応用管理 did not put to the user; any one of them shows the normal Prepare screen instead, exactly
 * as for an install that did not come through the hand-off.
 *
 * @param unprivilegedBackend the install would go through the system package installer rather
 *   than Shizuku / root / Dhizuku / a custom authorizer.
 * @return the reasons to stop and ask, for the log; empty means install without asking.
 */
fun confirmedHandoffBlockers(
    apkInfo: ApkInfo,
    risks: List<InstallRisk>,
    unprivilegedBackend: Boolean,
    canInstallPackages: Boolean,
    hasSufficientStorage: Boolean,
): List<String> = buildList {
    // Signature mismatch (needs an uninstall first), trackers under "block on trackers", and the
    // VirusTotal risks of a file whose review was not vouched for.
    risks.forEach { add("risk ${it::class.simpleName}") }
    if (apkInfo.isBlocked) add("package is on the never-install list")
    // A bundle whose splits can be chosen: the choice is made on the Prepare screen.
    if (apkInfo.splitEntries.any { it.type != SplitType.Base }) add("bundle with selectable splits")
    // OBB data: attaching files and the one-time directory grant are offered on the Prepare screen.
    if (apkInfo.obbFileNames.isNotEmpty()) add("OBB data")
    // The system installer refuses a downgrade outright; only a privileged backend can do one in
    // place. The Menu is where another method is picked, or the app removed first.
    if (isDowngrade(apkInfo) && unprivilegedBackend) add("downgrade without a privileged backend")
    if (!canInstallPackages) add("install permission not granted")
    if (!hasSufficientStorage) add("not enough storage")
}
