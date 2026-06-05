# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Fork model (READ FIRST)

This repo is **白い熊's personal fork** of `pass-with-high-score/universal-installer`, kept installable
side-by-side with the official app. The fork follows the same model as 白い熊's other Android forks.

| Item | Value |
|------|-------|
| Upstream | `pass-with-high-score/universal-installer` (remote `upstream`, HTTPS, **fetch-only**) |
| Fork | `git@github.com:ShiroiKuma0/shiroikuma-universal-installer` (remote `origin`, SSH — push here) |
| Mirror branch | `main` — mirrors `upstream/main`, never carries our changes, fast-forward only |
| Dev branch | `custom` — carries all our commits, rebased onto each new upstream release |
| Fork applicationId | `shiroikuma.universalinstaller` (`APP_ID` in `gradle.properties`) |
| Code namespace (UNCHANGED) | `app.pwhs.universalinstaller` (R/BuildConfig/AIDL/FileProvider class names — never touch) |
| App label | `白い熊 Universal installer` (`app_name` in `values/strings.xml` + `values-ro/strings.xml`) |
| Flavor we ship | **`full`** (root + all features) → task `:app:buildFork` (`assembleFullRelease`) |
| Fork version | `versionName = "<VERSION_NAME>+<BUILD_NUMBER>"`, `versionCode = VERSION_CODE*10000+BUILD_NUMBER` (`gradle.properties`) |
| Signing keystore | `~/.android-keystores/shiroikuma-universalinstaller.jks` (alias `universalinstaller`), via gitignored `key.properties` |
| Build JDK | OpenJDK 21 at `/usr/lib/jvm/java-21-openjdk-amd64` (default `java` here is 11) |
| APK output | `~/tmp/shiroikuma-universal-installer_<versionName>.apk`; on phone → `/sdcard/tmp/` |

`VERSION_NAME` / `VERSION_CODE` track upstream; `BUILD_NUMBER` is our increment — it **resets to 1** on each
new upstream version and **bumps by 1 on every build** (`buildFork` does the bump). Keep our customizations a
small, legible layer on top of upstream (rebase, don't merge).

**Hard rules (every session):** never `git commit` / `git push` unprompted — only when 白い熊 explicitly says
**"Push"** (then commit + `git push origin custom`). Never `adb install` / `pm install` — at most `adb push`
the APK to `/sdcard/tmp/` after asking, and 白い熊 installs it manually. See the two skills in
`.claude/skills/`: **build-apk** (build + optional push) and **upstream-new-version** (rebase onto a new
upstream release). These plus this section are fork infrastructure and live on `custom` only.

The rest of this document describes the upstream architecture (unchanged by the fork).

## What this is

Universal Installer (`app.pwhs.universalinstaller`) — an Android package manager that installs APK/APK+/APKS/XAPK/APKM (split APKs + OBB), downloads packages from URLs, manages installed apps, and silent-installs via Shizuku or root. Single-module Android app: Kotlin + Jetpack Compose + Material 3, MVVM, Koin DI. minSdk 24, target/compileSdk 36, Java/JVM 11.

## Build, test, run

```bash
# Build verification — MANDATORY before declaring any task complete (builds both flavors' debug).
./gradlew assembleDebug

# Per-flavor builds
./gradlew assembleStoreDebug      # store flavor (no root)
./gradlew assembleFullDebug       # full flavor (root via libsu)
./gradlew assembleStoreRelease assembleFullRelease
./gradlew bundleStoreRelease      # AAB for Play

# Tests
./gradlew test                                         # all unit-test variants
./gradlew testStoreDebugUnitTest --tests "app.pwhs.universalinstaller.SomeTest"
./gradlew connectedFullDebugAndroidTest                # instrumented (device/emulator)
./gradlew lint

# Fastlane (bundle install first; see README for full lane list)
bundle exec fastlane test
bundle exec fastlane build_debug          # both flavors
bundle exec fastlane bump_version version_name:"2.0"
```

Release builds only sign when a `key.properties` exists at repo root (gitignored, CI-supplied); otherwise they build unsigned. `local.properties` is also gitignored.

## Two product flavors (the central architectural constraint)

The `distribution` flavor dimension has two flavors sharing one codebase, same `applicationId`, and same signing key (so a user can sideload `full` over a Play install without data loss):

- **`store`** (default) — ships to Google Play / F-Droid. `BuildConfig.HAS_ROOT_SUPPORT = false`. **No libsu / root code is compiled in**, so Play's static analysis has nothing to flag as "device abuse."
- **`full`** — distributed on GitHub. `HAS_ROOT_SUPPORT = true`. Pulls in `ackpine-libsu` + `topjohnwu.libsu` for a real root install backend.

**All root/libsu code lives in `app/src/full/`** and is reached only through the `InstallerBackendFactory` interface (defined in `main`). The `store` source set provides `StoreInstallerBackendFactory` (no-op: returns `null` controllers and `Result.failure`); the `full` source set provides `FullInstallerBackendFactory` + `RootInstallController` + `PrivilegedRootService` (a libsu RootService running as UID 0 for hidden `IPackageManager` calls). Koin binds the right one via the flavor-specific `di/FlavorModule.kt`.

**Rules:** never add `libsu` imports or `BuildConfig.FLAVOR` branches in `main`. Anything root-related goes behind `InstallerBackendFactory` (`app/src/main/.../install/controller/InstallerBackendFactory.kt`) so `main` code stays flavor-agnostic.

## Architecture

Layered packages under `app/src/main/java/app/pwhs/universalinstaller/`: `data/` (Room `local/`, Ktor `remote/`, `repository/`), `domain/` (`model/`, `manager/`, `repository/`), `presentation/` (one package per feature, each with Activity + Compose `Screen` + `ViewModel`), `util/`. DI is split between `di/module.kt` (`appModule`, shared) and the per-flavor `flavorModule`; `App.onCreate` starts Koin with both.

**Install pipeline** — this is where most of the complexity is:

- **Ackpine** (`ru.solrudev.ackpine`) performs the actual install/uninstall, split-APK assembly, and hosts the Shizuku & libsu plugins.
- `BaseInstallController` (abstract) drives an ackpine `ProgressSession`: tracks active sessions, writes history, runs a post-success hook (e.g. OBB extraction) *before* optional source-file deletion, and persists in-flight sessions. Subclasses only implement `createSession()`:
  - `DefaultInstallController` — system installer, `Confirmation.IMMEDIATE`.
  - `ShizukuInstallController` — ackpine shizuku plugin; applies install options + installer-package spoofing from prefs.
  - `ManualInstallController` / `ManualTargetedInstaller` — installs targeted at a specific user id.
  - `RootInstallController` — **full flavor only**, via libsu.
- `InstallViewModel` (~1900 lines) is the orchestrator. `activeController(profileId)` selects the backend by precedence: **profile `preferredBackend` → targeted user → global `USE_ROOT`/root-spoof → `USE_SHIZUKU`/shizuku-spoof → default**. It probes root/Shizuku readiness (`probeRootState`, `isShizukuReadyForInstall`) before handing off and falls back to the default installer if the chosen backend isn't actually usable.
- `BackendSelfHeal.runOnce` runs once per process at startup to clear stale install-method prefs when root was revoked or Shizuku is gone.

**OBB handling**: `selectObbStrategy()` picks a write path in order — `Direct` (pre-Android 11) → `Shizuku` → saved `Saf` tree grant → `NeedSafGrant` (prompt user once per package). The copy runs in `ObbCopyWorker`, a WorkManager foreground worker that survives app process death.

**Persistence**: Room DB `universal_installer.db` (install / uninstall / download history DAOs; migrations registered in `appModule` — add a new `MIGRATION_x_y` there when bumping schema). DataStore (`PreferencesKeys`) holds all settings. `SessionDataRepositoryImpl` is backed by `SavedStateHandle`, so active install sessions survive config changes and process death.

**Shizuku/root options are stored as parallel `SHIZUKU_*` and `ROOT_*` preference keys** — when the user toggles an option, both keys are written together (see `InstallViewModel` ~line 460). Keep that mirroring intact.

**Other subsystems**: VirusTotal (`data/remote/`, SHA-256 lookup + optional large-file upload over Ktor with a 5-min request timeout); Sync/Share LAN server (NanoHTTPD `ApkHttpServer` + foreground `SyncService`, web UI in `assets/sync_*.html`, QR via zxing, optional PIN); installer profiles (`ProfileManager` serializes `InstallerProfile` list + per-app mapping as JSON in DataStore).

**Entry points** (manifest): `SplashActivity` (launcher) → `MainActivity`. `DialogInstallActivity` is exported and handles `VIEW` / `INSTALL_PACKAGE` / `SEND(_MULTIPLE)` intents — the "open an APK/XAPK from Chrome, Gmail, Telegram, a file manager" flow. Feature activities exist for manage / backups / permissions / sync / settings / download.

## Conventions & gotchas

- String resources: `fix_strings.py` escapes unescaped apostrophes inside `<string>` tags across `values-*/strings.xml`; `check_escapes.py` flags invalid backslash escapes. Run these after editing translation files. The app ships ~17 locales — keep keys in sync.
- Release changelogs live in `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` and **must be ≤ 500 characters** (Play API rejects longer). Play-facing fastlane lanes must use the `store` flavor — `full` ships libsu and would fail Play review.
- Hidden-API access uses `hiddenapibypass` + `rikka.stub` (`compileOnly`); logging is Timber.

## Gemini CLI skills (also usable as workflow references)

`GEMINI.md` + `.gemini/skills/` define established project workflows that document how recurring tasks are expected to be done here, even when working as Claude:

- **cook** — structured feature implementation (research → spec → implement → verify with `./gradlew assembleDebug`).
- **upgrade-app** — release flow: bump `versionCode`/`versionName` in `app/build.gradle.kts`, write the ≤500-char changelog, verify build, commit + tag `vX.Y.Z`, push, `gh release create`.
- **csv-translator** — chunked CSV translation → import into Android string resources.
- **github-issue-manager** — fetch/analyze issues and open fix PRs via `gh`.
