# Android TV app + modularization plan

Modules already created: `:core` (android-library), `:mobile` (the existing app,
renamed from `:app`), `:tv` (Compose-for-TV app scaffold).

Goal: a TV (10-foot, D-pad) version of the installer that **reuses the install/
manage engine** via `:core`, without breaking the shipping `:mobile` app.

## Target architecture

```
:core   ← UI-agnostic engine: domain models + data/engine (no Compose, no Koin)
:mobile ← existing phone UI  (keeps working throughout; migrates onto :core later)
:tv     ← new 10-foot UI (tv-material, D-pad) on top of :core
```

`:mobile` is NOT migrated in early phases — that's a large, risky rewrite of 149
files. We grow `:core` incrementally and let `:tv` consume it now; `:mobile`
adopts `:core` piecemeal once the API has proven out on TV.

## Phases

### Phase 1 — runnable TV MVP: manage + uninstall  ← THIS PASS
- `:core`: `domain.InstalledApp` + `AppRepository.getInstalledApps()` (PackageManager
  query: label, system flag, size, enabled, installer — permission-free, no UsageStats).
- `:tv`: depend on `:core`; replace the template Greeting with a D-pad `ManageScreen`
  (TvLazyColumn of focusable app cards) + uninstall via the system `ACTION_DELETE`
  dialog (no privileged perm needed). Manifest: QUERY_ALL_PACKAGES + leanback.
- Acceptance: launches on a TV/emulator, lists user apps, focus moves with D-pad,
  selecting an app → system uninstall flow → list refreshes.

### Phase 2 — install on TV (QR transfer + local scan)  ← REVISED after device probes

**Why revised.** Probes on an Android TV (API 31) emulator showed the easy install
inputs are all dead on TV:
- No activity handles the apk `VIEW`/`INSTALL_PACKAGE` intent → can't open a system
  installer by intent. **But** `com.google.android.packageinstaller` is present, so a
  **PackageInstaller session** works (its confirm UI is launched via the session's
  `PENDING_USER_ACTION` intent, D-pad navigable).
- SAF file picker is a no-op **stub** (`frameworkpackagestubs`) → can't pick a file.
- No "All files access" settings screen on TV → can't read arbitrary Download/USB
  files under scoped storage.
- `MANAGE_UNKNOWN_APP_SOURCES` settings and `DownloadManager` are present.

So getting the APK *onto* the TV is the real problem. Chosen direction (user): the TV
runs a **receiver server + shows a QR**; a phone uploads the APK over LAN; the TV
installs it via a PackageInstaller session. Plus a best-effort local scan.

**Components**

- `:core`
  - `ApkInstaller` — PackageInstaller session install from a content/file `Uri`
    (write bytes → commit → handle `PENDING_USER_ACTION` → final status), suspend/Flow
    result. The one proven install path on TV.
  - `ApkReceiverServer` — NanoHTTPD (already a dep) listening on TV: serves a
    phone-friendly **upload page** at `GET /` and accepts `POST /upload` (multipart) →
    saves to TV cache → hands off to `ApkInstaller`. Token/PIN auth (reuse the mobile
    sync server's cookie/PIN pattern). Mirrors `ApkHttpServer` but **receives** instead
    of serving.
  - `LanAddress` + `QrEncoder` (QR bitmap from the receiver URL+token).
  - `DownloadsApkScanner` — MediaStore query for APKs already in Downloads.
- `:tv`
  - **Receive screen**: QR + IP + PIN ("scan to send an app"); live "receiving… / install?"
    state; D-pad friendly.
  - **Local screen**: list APKs found in Downloads → session install.
  - Nav shell (Install | Manage) built from proven `Surface`/`Card` (NOT tv-material3
    `TabRow` — same alpha cohort that crashed via tv-foundation; avoid the gamble).
  - Manifest: `REQUEST_INSTALL_PACKAGES`, `INTERNET`, foreground-service for the server
    if it must outlive the screen.
- `:mobile` (later sub-phase)
  - "Send to TV": QR **scanner** (camera) → parse TV URL/token → multipart upload the
    selected APK to the TV. (Any phone browser can already upload via the web page, so
    this is an enhancement, not a blocker.)

**Sub-phases**
- 2a: TV receiver server + web upload page + QR + session install (verify with any
  phone browser; no `:mobile` change).
- 2b: TV local Downloads scan → install.
- 2c: `:mobile` QR-scan + "Send to TV" integration.

### Phase 3 — network push + downloader on TV
- Reuse `:mobile`'s sync HTTP-server idea in `:core` so a phone can push an APK to
  the TV (text entry on TV is painful — this sidesteps it). Optional URL download.

### Phase 4 — privileged backends on TV
- Move Shizuku / Root (libsu) install paths into `:core`; expose in `:tv`. Many TV
  boxes are rooted, so this is high-value there.

### Phase 5 — migrate `:mobile` onto `:core`
- Replace `:mobile`'s duplicated engine pieces with `:core` calls, module by module
  (models → repository → installers), keeping the app green at each step. End state:
  one engine, two UIs.

## Notes / constraints
- TV input: no file-manager VIEW/SEND intents like phone — entry is USB browse,
  network push, or downloader (Phases 2–3).
- Text entry on TV is clunky → minimize text fields.
- `:core` stays Compose-free and Koin-free so both UIs (and tests) can use it plainly.
