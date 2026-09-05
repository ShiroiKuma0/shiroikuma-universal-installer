# 白い熊 Universal installer — changelog

Everything this fork adds on top of stock **Universal Installer**
([pass-with-high-score/universal-installer](https://github.com/pass-with-high-score/universal-installer)).
Installs side-by-side with the official app (app id `shiroikuma.universalinstaller`).

## 1.14.0+001

**New in this build:** the fork moves from upstream **1.13.0** to **1.14.0**, skipping straight past
1.13.1. This is the largest upstream jump the fork has taken — 39 commits bringing an encrypted
whole-app backup, two new install backends, and an embedded-tracker scanner — and the fork layer was
replayed on top of it in full. No new fork features; everything below is either upstream's or the
work of keeping ours intact across it.

### ⬆️ What upstream brought (1.13.1 + 1.14.0, versionCode 38)
- **Full backup and encrypted restore.** Settings, profiles, source API tokens, tracked apps and
  uninstall logs export to one file, optionally sealed with a password (AES-256-GCM); restore reads
  it back, and Obtainium exports too. This is upstream's own backup, and it is not the fork's
  automation contract — the two are independent and both ship.
- **Custom Authorizer**, a new install backend that runs a user-supplied shell command, with
  validation and an execution guard, configured from a redesigned bottom sheet with quick presets.
- **microG installer mode**, and **Dhizuku folded into the single `InstallMode`** rather than living
  as a parallel flag, with Profile Owner limitations handled.
- **Embedded tracker scanning.** A DEX scanner matched against a bundled exodus tracker database
  runs asynchronously and reports into a Security tab on the install dialog.
- **Auto-approve install requests from chosen apps**, so a trusted caller no longer prompts.
- **Keep the APK after installing**, as a per-install toggle on the install sheet — and the sheets
  no longer dismiss on an accidental swipe, gaining explicit cancel buttons.
- **A rebuilt onboarding** with 3D parallax icon animations and a VirusTotal slide that skips itself
  when it has nothing to ask.
- **Android TV gains Shizuku**, privileged install options, and a set of focus-visibility fixes.
- **QR scanning validates the host** and verifies connectivity before connecting, so a malicious QR
  code can no longer point the app at an untrusted server.
- Readable extractor output paths, exported APKs visible in Downloads, and a Karing update-detection
  fix.

### 🧩 Keeping the fork layer on top of it
- **All 82 fork commits replayed**, nine conflicts resolved by porting our change to upstream's new
  structure rather than forcing the old diff back in:
- **Chips are themed in one place now.** Upstream folded the install dialog's three `AssistChip`s
  into a single `DialogPill` component; the fork's per-text-category **`chip`** style moved inside
  it. Every pill — including upstream's new ones — now picks up the fork styling from one line
  instead of three copies that could drift.
- **The keep-APK choice flows through our themed buttons.** Upstream's `onDone` / `onOpen` callbacks
  now carry it; the fork's `DialogActionButton`s were rewired to pass it on, so the success stage
  keeps both its styling and the new behaviour.
- **The engine badge stays black-and-yellow** and now labels upstream's two new backends —
  **Custom** and **microG** — in the same pill, rather than reverting to upstream's
  privileged-versus-plain container colours.
- **Shizuku: our fix and upstream's, together.** The fork waits for Shizuku to *push* its binder
  before judging the state (an instant verdict reports "not running" on a healthy cold start) and
  names **白い熊 雫** when telling you what to open. Upstream added mutual exclusion between the
  backends. Both now apply: enabling Shizuku waits for the binder, and clears root, Dhizuku, the
  custom authorizer and microG in the same write.
- **The watch button** moved inside upstream's new `WearApkSender.isAvailable` gate, taking the
  fork's top-bar icon tint with it.
- **Locales: upstream rewrote `values-fr` and `values-hi` wholesale** (~1,800 lines each) and re-added
  its own `app_name` to both. The fork's three edits were re-applied to the new files, and the
  **白い熊 Universal installer** label is asserted across all **19** `values*/strings.xml` again —
  this is the fourth upstream release in a row that has had to be undone.
- The same-version **Reinstall** state, the stacked long-version-name layout, the dropped false
  **"data may be wiped"** downgrade warning and the whole theming engine all survive unchanged.

### 📦 Unchanged
- **arm64-v8a only**, `shiroikuma-universal-installer_<version>_arm64-v8a.apk`, as since `1.13.0+003`.
- **No telemetry.** Upstream's `play` flavor still needs a `google-services.json` this fork does not
  ship, so every `play` variant is disabled at configuration time and `buildFork` stays pinned to
  the `opensource` flavor.
- versionCode `380001` (`VERSION_CODE 38 × 10000 + 1`), so this line sits above every 1.13.0 build.

## 1.13.0+003

**New in this build:** the APK is now **arm64-v8a only**, and its filename says so. No app-code
change — same 1.13.0 (versionCode 36) with the automation contract v2 of `+002`.

### 📦 One ABI, and a filename that means it
- `ndk.abiFilters` restricts the build to **arm64-v8a**. Every fork build from here is arm64 only,
  rather than the name merely claiming it.
- The artefact is now `shiroikuma-universal-installer_<version>_arm64-v8a.apk`, matching the family
  convention that sister forks already use, so all of 白い熊's APKs sort and read alike in one
  directory. Both `archivesName` and the `buildFork` copy carry the suffix, so the intermediate
  output and the delivered file can never drift apart.
- **The suffix describes the file, it does not decorate it.** Until this build the APK shipped all
  four ABIs — `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64` — so adding the suffix alone would have
  mislabelled it. The native payload is two AndroidX libraries (`libandroidx.graphics.path`,
  `libdatastore_shared_counter`), both with pure-Java fallbacks; dropping the other three ABIs takes
  **97,888 bytes** off the APK and changes nothing on an arm64 device.
- **Consequence:** this APK will not install on a 32-bit (`armeabi-v7a`) or x86 device. Builds up to
  and including `1.13.0+002` remain universal, and keep their un-suffixed names.

## 1.13.0+002

**New in this build:** the sister-app **automation contract v2**. The token stops being the gate, a
second door opens that can move this app's configuration through a file descriptor, and the export
learns to be stopped. No upstream change — this build is 1.13.0 (versionCode 36) with fork work only.

### 🔓 The token becomes opt-in, and the switch ships on
- **`automation_enabled` now defaults to ON** and a new **`automation_require_token` defaults to
  OFF**, so the app answers automation out of the box. The reason is a clean phone: a pasted secret
  cannot survive a wipe, and the case this contract exists to serve is restoring an app *and its
  data* onto a device where nothing has been configured yet.
- **A token sent to the app when none is required is ignored, never refused.** Tokens outlive the
  setting they were pasted for, and a caller still sending one — because another app on the batch
  wants one — has to be served rather than failed.
- Both checks now live in **one** `AutomationAuth.refuse()` rather than being written out at each
  entry point, which is how "automation disabled" and "bad token" drift apart across sister apps.
  They stay distinct errors; they debug differently.
- The Export/Import section gains a **「Use authorization token?」** row, and the 48-character token
  row is now **hidden unless it is on** — a secret sitting under an off switch only invites being
  pasted somewhere it does nothing.

### 🚪 A data door: `ContentProvider` at `<pkg>.automation`
- A new exported provider answers **`describe`**, **`export`**, **`import`** and **`cancel`**, all
  synchronous, all short, none carrying the payload. Answers use the same `OK:` / `ERROR:` grammar
  as the broadcast contract, so a caller has one vocabulary rather than two, and a refusal is
  *returned* rather than thrown — an exception across a binder tells the caller rather more than it
  should.
- **The caller is identified three ways**, because a broadcast cannot say who sent it: an **exact
  package name** (never a prefix — a name cannot be taken while the real package is installed, but
  any sideloaded app may call itself `shiroikuma.evil`), a **uid cross-check** against
  `getPackagesForUid`, which cannot be borrowed, and a **pinned signing certificate**, which is what
  closes the real gap — whichever caller is absent from a device is a name anyone can take, and a
  clean phone is exactly a device where not everything is installed yet.
- **The payload moves through a caller-supplied `ParcelFileDescriptor`**, duplicated before it
  leaves the provider call and closed in a `finally`. Not a path and not a `content://` URI: the
  destination is renamed on commit, encrypted per known file and checksummed per known file, so a
  file dropped in from outside would be renamed away, sit in plaintext inside an encrypted backup
  and be unverified rather than verified. It also means this app no longer needs
  `MANAGE_EXTERNAL_STORAGE` to be backed up.
- **`import` exists only here** and never gets a broadcast action. It overwrites configuration, and
  the broadcast receiver is exported with no permission.
- The work runs in a **`specialUse` foreground service**, not in the binder call — which returns in
  milliseconds while this can take longer, and a backgrounded app writing for minutes is frozen
  mid-stream on EMUI, yielding a truncated archive underneath a success reply.
- **`describe` reports `"format": 3`, not 1.** The format is *this app's archive version*
  (`UiConfigBackup.VERSION`), not a contract constant, with `min_format_readable` at 1 because the
  importer still accepts every older ZIP and the pre-ZIP single-JSON export. A caller must read the
  value rather than assume it.

### ⏹ The export can now be stopped, and never leaves half a backup
- **`…action.CANCEL_EXPORT`** joins the exported receiver, so a long export can be stopped from
  where it was started. It is fire-and-forget — it sends no reply of its own; the terminal
  `ERROR:cancelled` belongs to the export it stopped — and it is a silent no-op when nothing is
  running or the run already finished.
- **Backups are now written atomically.** The archive goes to `<final-name>.part` and is renamed
  only once it is closed and complete; a failure, a cancellation or a kill deletes the partial on
  the way out. Previously a killed export left a file indistinguishable from a real backup until
  someone tried to restore it. The part-file deliberately does not end in `.zip`, so the panel's
  "latest export" scan can never mistake one for the newest backup.
- A **process-local single-flight guard** replaces having none, which is also what makes a cancel
  without a `reply_id` unambiguous. It is never persisted — an "export in progress" flag written to
  disk wedges the app for good after one crash.
- Progress broadcasts now carry the **`item`** extra (the category id being written), so a caller's
  panel highlights the row actually in progress instead of inferring a position from the count.

### 📜 Manifest
- **A `<queries>` element, which was missing entirely**, naming both automation callers. Without
  package visibility a reply broadcast's `setPackage` fails silently on Android 11+ — the export
  runs, writes correctly and is never heard of. This app's `QUERY_ALL_PACKAGES` was already
  supplying the visibility, so replies did arrive; the element is now declared so the contract does
  not rest on an unrelated permission held for the package-manager features.
- The provider, the data service, `CANCEL_EXPORT`, and three `shiroikuma.automation.*`
  `<meta-data>` entries (`contract` 2, `format` 3, `min_format` 1) that let a caller judge this app
  **without waking it**, which matters because a frozen package cannot be asked anything. They
  compile as integers, not strings.
- **`FOREGROUND_SERVICE_SPECIAL_USE`**, which `startForeground` requires on API 34+.

### 🔍 What the automation surface can reach
Stated plainly, because the token no longer gates it by default:
- **The broadcast export is unauthenticated by default** and writes where the caller says. Its
  archive includes the `security` and `sync` categories, which carry the **VirusTotal API key** and
  the **Sync & Share PIN**.
- **A provider `import` writes install policy**, since `install_behavior` carries
  `auto_confirm_external_install` — which makes the install dialog skip its preview step — and
  `engines` carries the root and Shizuku flags including grant-all-permissions. The provider's
  caller check gates this; the exported install activity it affects has no such check.

## 1.13.0+001

**New in this build:** rebased onto **upstream 1.13.0** (versionCode 36), tagged the same day this build was made. All **75** fork commits replay on top. No new fork features — upstream 1.13.0 is essentially one very large PR (#120, *Fix/wearos apk transfer*) that rebuilds the send-to-watch flow end to end, plus a packaging change that folds the TV and watch builds into the phone's Play listing.

### ⌚ Send-to-watch, rebuilt end to end (upstream #120)
- **The transport had never actually worked.** `WearReceiverService`'s `CHANNEL_EVENT` intent filter carried no `data` element, while GMS dispatches channel events with a `wear://<node>/<path>` URI — so the service never resolved, nothing read the channel, and the phone's writes blocked forever on a full buffer, sitting at 0% until GMS gave up with "Channel closed unexpectedly". With that fixed the bytes arrived but the work still died: GMS unbinds the listener service as soon as `onChannelOpened` returns, so `onDestroy` cancelled the service scope mid-copy and the APK landed on disk unparsed, with no notification and no list entry.
- **The phone now offers only watches that actually have the app.** `NodeClient.connectedNodes` reports every connected watch, so the phone used to stream an APK into a channel nobody listened on — and still report success. It now queries `CapabilityClient` for an `apk_receiver` capability the watch declares.
- **A send sheet replaces the one overloaded toolbar button**, which used to do two different things depending on hidden state. The sheet shows which watch would receive the file (or why none is reachable, with a retry, instead of a greyed-out button that explains nothing), the currently open APK as a one-tap send, every scanned package with icons and install state behind a **Wear OS** filter chip, and the raw file picker demoted to an escape hatch with no mime filter. Availability is re-polled on resume, so putting the watch on after opening the app registers.
- **A transfer now survives leaving the screen.** `WearTransferService` owns it as a `dataSync` foreground service, cancel lands within 8 KB, all five `Tasks.await` calls got timeouts, and a watchdog closes a channel whose byte count has not moved for a minute — the only thing that frees a blocked `write()`.
- **A phone-only APK raises a confirm step** instead of transferring in full and failing at install time on the watch, split bundles keep their extension so the watch knows to unpack them, the payload size rides in the channel path so the watch can check free space and detect a truncated write, and the launcher **icon is sent ahead of the payload** over `MessageClient` so the watch can show what is arriving.
- Leaks fixed along the way: the channel now closes in a `finally` block and the `ParcelFileDescriptor` is wrapped in `use {}` — both leaked on every failed send. Progress is emitted once per whole percent rather than once per 8 KB.

### ⌚ The watch app itself
- **Received packages stop vanishing.** They lived only in a `MutableStateFlow`, so anything that killed the watch between transfer and opening the app dropped them from the list while the file stayed on disk forever. The cache directory is now the source of truth.
- **Installing reports the real result.** It treated `commit()` as synchronous, so a non-privileged installer's `STATUS_PENDING_USER_ACTION` was never read — the confirmation dialog never appeared, yet the UI claimed success and deleted the cached file. It now goes through core's `ApkInstaller`.
- A partial wake lock plus a foreground service hold the receive together, free space is checked before writing and the received size verified, `.xapk` parses through `ApkMetadataReader` instead of resolving to null and being silently deleted, and `POST_NOTIFICATIONS` is finally requested rather than merely declared.
- **The UI was rebuilt**: the app's own palette (it had been rendering in default Material purple), APK icons in the list and detail, a single badge that says the worst thing first, live receive progress with a bar that actually moves, swipe-to-delete, an empty state, storage on the home screen, and new **Manage**, **Settings** and **About** screens — with a language picker, accent presets and the same 18 locales as the phone.
- The Android Studio template tile ("Hello, Tile!") and day-of-week complication are gone, taking their whole dependency stack with them — the debug APK drops from 93.9 MB to 84.0 MB.
- `WatchAppCheck` reads the manifest **inside** a split bundle now; it used to hand an `.apks`/`.xapk` straight to `getPackageArchiveInfo`, get null, and brand every bundle a phone app.

### 📦 Packaging: one listing, three form factors
`:wearos` (versionCode 1036) and `:tv` (2027) both **adopt the phone's `applicationId`** and ride the phone's Play listing as additional form factors rather than separate listings, in version bands — phone 1–999, watch 1000+, TV 2000+ — so a device is handed the highest code it is eligible for. `:tv` gains a real 320×180 leanback banner in place of the square launcher icon it had been pointing at.

### 🧩 What the rebase needed, and what it means here
- **A small rebase.** Three conflicts, all additive: the version literals in `app/build.gradle.kts` (ours kept, upstream's values moved into `gradle.properties`), upstream's new `watch_*` strings next to our 白い熊 UI strings, and our `ThemedSurface(AppSurface.Main)` wrapper next to the new watch-state collection in `InstallScreen`. Unlike 1.12.0, the theming layer needed no porting at all.
- **Send-to-watch cannot pair from this build.** The Wearable Data Layer only routes a channel between nodes running the **same package name and signing certificate** — which is exactly why upstream moved `:wearos` onto `app.pwhs.universalinstaller`. This fork's phone app is `shiroikuma.universalinstaller`, signed with 白い熊's own key, so no watch will ever answer the `apk_receiver` capability query and the button reports "No watch found". Making it work would mean building and shipping a matching fork watch APK; this build does not.
- **The Polish locale joined the rest.** `values-pl/strings.xml` was the one locale file with no `app_name` override — Polish fell back to the default so the label was already right, but the fork invariant is that *every* `values*/strings.xml` pins it, so a future upstream that adds a Polish `app_name` cannot regress the launcher label unnoticed. All **19** locale files now carry `白い熊 Universal installer`.
- `buildFork` still builds `:app` alone, on the `opensource` flavor — the fork ships exactly one APK, as always. Our theming layer touches `:tv`'s theme and launcher icon, but that module is not built either.

### 🕵️ Still no telemetry
Unchanged: with no `google-services.json`, every `play` variant is disabled at configuration time, the Firebase plugins are never applied, and the `opensource` telemetry factory returns a no-op sink. Not a line of Analytics or Crashlytics is linked into this APK.

## 1.12.0+001

**New in this build:** rebased onto **upstream 1.12.0** (versionCode 35) plus the seven commits pushed after the tag — skipping the whole **1.11.0** line, so this build swallows *two* upstream releases at once. All **68** fork commits replay on top. No new fork features: this build is the port, and 1.11.0's "modularize everything under 500 lines" campaign landed directly on the files our theming layer lives in.

### 🧩 Upstream moved the theme into `:core`, and split the settings ViewModel apart
- **`app/…/ui/theme/Theme.kt` is now a forwarding shim** onto `app.pwhs.core.ui.theme` — upstream duplicated the whole theme into `:core` so `:tv` and the new `:wearos` module can share it. Nothing else under `app/src` imports the core theme, so **our theming engine stays where it is** and keeps replacing that shim; the fork layer did not have to move.
- **`AppThemePreset.Yellow` did have to follow**, because the enum lives in `:core`: without a branch in core's own `getPresetColorScheme()` the module fails to compile on a non-exhaustive `when`, and `:tv` — which shares the `theme_preset` key — would have no scheme to draw the fork's default preset with. The black/yellow dark and light schemes are now defined in `:core` as well.
- **`SettingViewModel` went from 1149 lines to 288**, split into `PreferencesKeys.kt`, `SettingModels.kt`, `sections/` and four `util/*Delegate.kt` files. Our layer was rethreaded through all of it: the twelve fork `UI_*` DataStore keys moved to `PreferencesKeys.kt`, the `ForkUiDefaults` black/yellow defaults to `SettingModels.kt`, and the **entire 白い熊 雫 Shizuku fix** — `UiMessage`, `ShizukuManagerApp`, the permission-name lookup, `openShizukuManager()`, the 2-second binder wait and the finally-reachable `NOT_INSTALLED` state — into `SettingPrivilegeDelegate`, which gained an `emitMessage` channel for the messages that carry the manager's name.
- **The install dialog's body moved out of `DialogInstallActivity`** into a new `dialog/DialogInstallContent.kt`. `ThemedSurface(AppSurface.Dialog)`, the themable card border and the dropped "data may be wiped" downgrade gate went with it.
- **`ApkInfoContent` was split into `components/`**, so the monospace-for-technical font was re-applied in `ApkDetailCards.kt` and the **Same version / Reinstall** treatment — green button, `Autorenew` icon, chip — re-applied in `ApkInfoFooter.kt`.
- The install-dialog progress line, the success badge, the per-surface overrides, the top-bar icon colour and the Kōjiki export/import panel all survived the move intact.

### 📥 Upstream 1.12.0 — installing from anywhere
- **Download and install straight from a URL.** A new install-from-URL button in the top bar, **link sharing from browsers**, network APK download with **streaming install** and SMB path decoding. Downloads run in a **process-scoped background downloader** with a status-bar progress notification and a cancel action, so progress keeps going after you dismiss the dialog and tapping the notification reopens the install flow. The Download tab shows live percentage and size.
- **Install history got a memory.** Richer metadata per entry plus a **HistoryDetailSheet** — status, operation kind (new / update / downgrade), error diagnostics, and re-install / launch / app-info / copy actions.
- **Advanced install options (#95).** Five new flags for both Shizuku and root: allow restricted permissions, don't kill the app, disable verification, enable rollback, request update ownership — mirrored across the `SHIZUKU_*` / `ROOT_*` key pairs exactly like the existing ones.
- **Insufficient storage is caught before the install starts** rather than failing halfway, and two `ActivityNotFoundException` crashes are fixed — launching an app that has no launcher activity, and picking a file on a device with no DocumentsUI.
- **TV transfer overhauled**: zero-config discovery, PIN-less connect, a security fix, live upload *and* receive progress on both ends, device-presence indication, disconnect buttons and a centered hero-card pairing screen.
- **Wear OS.** Upstream added a `:wearos` module — an APK installer for the watch — and a **send-to-watch** action on the phone.

### 🔄 Upstream 1.11.0 — the app updater
- **A whole `:updater` module**, Obtainium-shaped: track apps from **GitHub, GitLab, Codeberg, F-Droid or a direct APK URL**, compare with a real SemVer comparator, and update them through this app's own install dialog. Category tagging and filter chips, sort options, pull-to-refresh, a detail sheet, batch update, an app picker, a periodic background update check, a global **Source API Tokens** dialog, and **JSON backup export/import that is Obtainium-compatible**.
- **VirusTotal scanning reached the install dialog itself** (#115) — a scanner prompt and status badge in both the dialog and the bottom sheet.
- **Ackpine 0.25.4** brings `TargetUser` and system-app uninstall; **Gradle 9.3.1 / AGP 9.1.0**; **R8 full mode** with optimized resource shrinking and class repackaging.
- Fixes: the install dialog no longer shows up in recents (#113), zipped APKs install on TV (#114), a stuck notification on rapid completion (#116), delete-source-after-install got a multi-tier fallback, and auto-open-after-install works in the bottom sheet.
- Reproducible-build groundwork: `compileSdk` minor API level pinned and native stripping disabled (#117).

### 🕵️ Still no telemetry, and no Wear OS fork
- 1.12.0 added a **comprehensive Firebase Analytics tracking plan** — an `AnalyticsHelper` with events for onboarding, permissions, installs, Shizuku state and more. It routes through `:core`'s `Telemetry` sink, which **defaults to `NoOpTelemetrySink`**, and this fork's `opensource` factory returns exactly that. With no `google-services.json` every `play` variant stays disabled at configuration time, the Firebase plugins are never applied, and **not a line of it is linked into the APK**. The in-app review prompt is still a no-op here for the same reason.
- The new **`:wearos` module is left completely stock** — its own `applicationId app.pwhs.universalinstaller.wearos`, its own label, its own `versionCode 1`. `:app` does not depend on it and `buildFork` does not build it, so the fork ships exactly one APK as before.

## 1.10.0+001

**New in this build:** rebased onto **upstream 1.10.0** (versionCode 33) plus the five commits pushed after the tag. All **65** fork commits replay on top of it. No new fork features — this build is the port itself, and it was a substantial one.

### 🧩 Upstream de-monolithed the three files our theming layer lives in
- 1.10.0 is mostly a refactor, and it landed squarely on our customizations: **`DialogMenuContent.kt` lost 927 lines**, `ManageScreen.kt` **2136**, `SettingScreen.kt` **701**, and `UninstallUi.kt` was split apart — all of it moved into new `tabs/`, `components/`, `sections/`, `sheets/` and `dialogs/` packages. Git cannot follow a hunk into a file that did not exist, so five of our commits had to be **ported by hand** rather than merged.
- **Per-text-category dialog styling survived intact.** All eleven `dialogTextStyle(…)` call sites were re-applied at their new homes — `tabs/InfoTab.kt` (the ABI list and the SHA-256 hash), and `components/AdvancedToggle.kt`, `DetailRow.kt`, `MenuCard.kt` and `PermissionRowList.kt`. Because those helpers are shared, the categories they cover — option title/description, detail label/value, section title/description, permission rows — keep styling every text that flows through them.
- **The 白い熊 Installer UI entry point moved with the Interface section** into `sections/InterfaceSection.kt`, sitting above the Theme row exactly as before, divider and all.
- **The Shizuku manager fix was rethreaded through the new component boundary.** `InstallModeSelector` now lives in `components/SettingComponents.kt` and the install block in `sections/InstallSection.kt`, so `shizukuManagerLabel` and `onOpenShizukuManager` are passed down explicitly: the **"Open 白い熊 雫"** button and the named *"… is installed but its service isn't running"* subtitle both still appear. The R8 keep rules for `rikka.shizuku.BinderContainer` were merged beside upstream's new Play-review `-dontwarn`.
- **Long version names still wrap** — the weighted, `TextAlign.End` value row followed `DetailRow` into its own file.
- **Our Same version chip now sits beside upstream's three new ones** rather than displacing them: the conflict cut through the middle of a chip block, and both sets were kept whole.

### 📦 What upstream 1.10.0 brings with it
- **Android Auto awareness.** Apps are checked against Android Auto's actual rule — the installing package must be the Play Store, and an ADB-initiated install is blocked — across scans, the install sheet and the Manage screen, with a **Compatible with Android Auto** chip, a "Requested by / Installed by" readout and a shortcut to Android Auto's settings. A **Install (Spoof Play Store)** action sends the install through the system package installer with the Play Store as the claimed source, no root required.
- **Warning chips for apps that ask for Root or Shizuku** — an APK requesting either now says so on the install sheet before you commit to it.
- **The install dialog's Retry and System Installer buttons work again.** The System Installer button used to fire a bare `ACTION_VIEW`, which resolved straight back to our own dialog the moment you made this app the default installer — so nothing happened at all. It now resolves an explicit component belonging to somebody else, and falls back to the older `ACTION_INSTALL_PACKAGE` for OEM installers that only advertise that.
- **Fixed duplicate history records for notification installs**, a settings screen that reported a default-installer change that never happened, and opening extracted backup folders in a system file manager.
- **Full Ukrainian translation** (#108), and the Manage screen's missing *open app* action restored.

### 🚫 The in-app review prompt is not in this build
- Upstream added a Play in-app review sheet after a successful install, gated behind a deliberately conservative `ReviewGate`. It is a **Play Store feature**: the `play` flavor wires it to Google's `ReviewManager`, the `opensource` flavor resolves it to a **no-op prompter**.
- This fork builds `opensource` and ships no Play Services, so **nothing ever asks you to rate it** — the same reason it carries no Firebase Analytics or Crashlytics.

## 1.9.12+002

**New in this build:**

### 🔁 Re-installing the same version is no longer dressed up as an update
- Opening an APK whose `versionCode` is **already on the device** was presented exactly like an upgrade: the dialog drew the yellow *"installed → new"* arrow transition between two identical version strings and offered an **Update** button. Nothing on screen said the version was not moving — the one case where you most want to be sure, because a re-install is what you reach for when something is broken, not when something is new.
- That state is now its own. `isUpdate` is restricted to a **strictly newer** `versionCode`; equal and older each have their own branch. A same-version file shows **`1.9.12 (32) — already installed`** in place of the arrow line, a **Same version** chip with a circular-arrows glyph, and a **Reinstall** button.
- **The colour had to come from outside the theme.** The fork's default **白い熊 Yellow** scheme overrides `primary`, `secondary` *and* `tertiary` to yellow, so no Material accent role could have signalled *"this is not an update"* — every one of them is the colour the state must be distinguished from. The chip and button use the semantic **success green** instead: it lives in `ExtendedColors`, outside the preset system, so it stays green under every preset, and it is already the app's established non-accent tint (the VirusTotal *Clean* chip). `onSuccess` and `successContainer` were added beside the existing `success`, mirroring the `warning` trio that was already there.
- Applied to **both** preview surfaces — the external-open install dialog (Chrome, Telegram, a file manager) and the in-app APK-info sheet — so the two never disagree about what an install is about to do. The downgrade path is untouched: still the red ⚠ line, chip and **Downgrade** button.

## 1.9.12+001

**New in this build:** rebased onto **upstream 1.9.12** (versionCode 32) — the largest upstream release the fork has absorbed. All 58 fork commits replay on top of it.

### 🕵️ Upstream added telemetry — this build has none of it
- Upstream 1.9.12 ships **Firebase Analytics and Crashlytics**, confined to a new `play` product flavor that only activates when an untracked `app/src/play/google-services.json` is present. A second flavor, `opensource`, is the default and carries neither.
- **The fork has no Firebase config**, so upstream's `beforeVariants` hook disables every `play` variant outright: the Google Services and Crashlytics Gradle plugins are never applied, and `TelemetrySinkFactory` resolves to the `opensource` **no-op sink**. Nothing measures anything.
- Our `buildFork` task now depends on **`assembleOpensourceRelease`** rather than the aggregate `assembleRelease`, and reads `build/outputs/apk/opensource/release`. Pinning the flavor by name means the telemetry-free build stays guaranteed rather than merely incidental — if a `google-services.json` ever appeared in a checkout, the fork build would still be the open-source one.
- `CLAUDE.md` records the rule: **never add a `google-services.json` to this fork.**

### 🧩 Fork customizations re-seated on rewritten upstream code
- **The false downgrade warning stays gone.** Upstream rewrote the risk dialog around a new shared `isDowngrade()` (so the warning and the actual `INSTALL_REQUEST_DOWNGRADE` decision can't disagree) and added a genuine `SignatureMismatch` risk. Both were kept; our removal of the alarmist *"existing app data may be wiped"* gate was re-applied on top, and the three upstream call sites that filtered on the now-absent `InstallRisk.Downgrade` were adapted. A downgrade is still signalled by the ⚠ subtitle, the chip and the red Downgrade button — it just doesn't stop you.
- **Settings messages keep their format arguments.** Upstream introduced a plain `emitEvent(Int)` helper on the settings event channel, which our Shizuku work had already widened to a `UiMessage(res, args)` so a toast could name *"白い熊 雫"*. The wider channel wins and `emitEvent` routes through it, so every upstream message still fires and ours keep their arguments.
- **The yellow dialog border survives the new bottom-sheet style.** Upstream can now render the install UI as an edge-attached sheet with its own container color, elevation and shape; the fork's themable border is applied to both presentations rather than only to the floating dialog.
- **The install button keeps its theming while gaining upstream's blocked-package state**, so a blacklisted package greys the button out without losing the per-slot color/border/font overrides or the downgrade red.
- **The app label was re-asserted across all 19 locales.** Upstream rewrote `values-pt-rBR` and `values-vi` wholesale (1426 and 1324 changed lines); those files were taken fresh and the fork's `app_name` and `白い熊 Yellow` preset string re-applied, so the launcher never regresses to *"Universal Installer"* on a non-default device language.
- Two settings pages now sit side by side: upstream's own **Install UI** screen (picks dialog vs. bottom-sheet presentation) and the fork's **白い熊 Installer UI** theming page. They do different jobs and both are reachable.

### 📦 What upstream 1.9.12 brings with it
- **Dhizuku as a fourth install backend** (via ackpine's Dhizuku plugin), presented as a switch rather than a fourth mode, offered only when Dhizuku is actually installed and the device is API 26+, and re-probed on resume so a grant made in Dhizuku's own app is noticed. ackpine **0.22.9 → 0.25.4**.
- **Install straight from a notification** — confirm or cancel without a window opening at all, with APKs staged before the URI grant expires and a check that the prompt can actually post before committing to it.
- **Bottom-sheet install UI** as an alternative to the dialog, with its own settings screen, plus APK-info sheet height capping measured from real constraints.
- **Package blacklist** with its own screen, and the dead VirusTotal button fixed.
- **Normal vs Strict security levels** — VirusTotal no longer owns the install button; Strict is the level that treats an unscanned file as a risk. Rejected API keys and spent quota are now reported as such, scan verdicts link to the report, and the onboarding page lets you paste the key where you get it.
- **Signature-mismatch warning up front**, with the conflicting app removable through whichever backend the install will use.
- **Help section** in Settings with a tutorial replay, and onboarding pages for VirusTotal and analytics, translated into the 17 core locales.
- **Android TV**: a generated baseline profile, R8 enabled, D-pad moves no longer recompose the whole app list, and package file types registered so *"Open with"* reaches the TV app.
- Fixes: Shizuku session writes routed correctly for targeted installs (#58), failed installs can be dismissed (#93), app names containing symbols no longer mangled on extract (#96), the source APK is deleted on paths that previously couldn't (#100), Retry works for sessions this controller didn't create, MIUI optimization guidance for Xiaomi devices, and 96 missing strings filled in across 18 locales.

## 1.9.11+019

**New in this build:**

### 🔻 The launcher glyph sits smaller in its tile
- The black/yellow download mark spanned **68.4% of the icon's height** (and 57.8% of its width) — just over the **66.7% safe zone** (72dp of the 108dp canvas) that Android reserves for adaptive icons, the region a launcher guarantees it will not crop. Under an aggressive mask, the circular one above all, the arrow and tray ran close to the edge.
- The mark is now scaled to **85.74%** of its former size — three successive 5% passes, `0.95³` — about the icon centre, bringing it to 58.6% of the height and 49.6% of the width, comfortably inside the safe zone.
- The transform lives on a `<group>`, so the **stroke weight scales with the geometry**: the yellow outline stays proportional instead of reading heavier as the mark shrinks.
- Applied to the adaptive **foreground** and the **monochrome/themed** vector, then re-rendered into every raster from the same path data — the legacy square and round mipmaps at all five densities, their foreground siblings, and the Play-Store image. Each legacy tile reuses **its own alpha channel** as the mask, so the rounded-square and circle silhouettes are byte-identical to before: only the glyph inside them moved.
- The splash screen draws that same foreground vector, so its logo shrinks to match within its fixed 140dp box.

## 1.9.11+016

**New in this build:**

### 🔑 Shizuku: the binder hand-off finally lands (the real fix)
- On a phone running **白い熊 雫** with the service **up** and the permission **granted**, the app still reported *"Shizuku installed but not running"* — forever, on every launch.
- Cause, straight from logcat: `BadParcelableException: ClassNotFoundException when unmarshalling: rikka.shizuku.BinderContainer at rikka.shizuku.ShizukuProvider.call`. A Shizuku server pushes the binder into the client's `ContentProvider` wrapped in a Parcelable whose **class name travels on the wire**. Current servers send `rikka.shizuku.BinderContainer` first and the legacy `moe.shizuku.api.BinderContainer` second — but `dev.rikka.shizuku:provider:13.1.5`, **the newest version ever published** (September 2023), only knows the legacy class. Reading *any* key unparcels the *whole* Bundle, so the modern envelope threw before the legacy path could run, the binder was dropped, and `pingBinder()` stayed false. No library upgrade exists to fix it.
- Fix: the fork **declares `rikka.shizuku.BinderContainer` itself** (one strong binder — the same parcel layout in every flavour) and swaps the manifest's provider for **`ShizukuCompatProvider`**, which reads that envelope and delegates everything else, legacy envelope included, to the library. The authority is unchanged, so the server addresses us exactly as before. Both classes are resolved by name under R8, hence explicit `-keepnames` rules — a rename would silently resurrect the dropped binder.

### 🔍 Shizuku: name the manager you actually have
- The Shizuku client API is **package-agnostic** — it can't pick or ask a manager app, it only waits for a binder to be pushed to it. The app now at least identifies the installed manager by whichever package **defines** `af.shizuku.plus` / `af.shizuku.manager` / `moe.shizuku.manager` `.permission.API_V23` — the same three names the server's own `BinderSender` matches.
- Selecting Shizuku now **waits ~2s for the binder** before judging, instead of toasting on the same frame: the push is asynchronous, so an instant verdict was often a false negative right after a cold start.
- `NOT_INSTALLED` is finally reachable — it was unreachable in the state machine, so *"not running"* was the catch-all for every possible failure.
- Messages read **"白い熊 雫 is installed but its service isn't running"**, the status line reads "白い熊 雫 installed but not running", and Settings offers an **"Open 白い熊 雫"** button that launches it. Events carry a resource + format args instead of a bare string id so a message can name the app.

### ⚫🟡 Black and yellow is now the compiled-in default
- Upstream ships every UI preference as "unset → inherit Material", so a **fresh install came up in upstream's orange** — and on Android 12+ in wallpaper-derived **Material You** colors — while the black/yellow theme existed only as DataStore values configured by hand and **lost on every clean install**.
- New `ForkUiDefaults` holds the palette and the theme defaults, and every preference *resolves* to them when no key is stored. Nothing is seeded: the Installer UI page shows real values instead of blanks, and its **"reset" lands on the 白い熊 look** rather than on stock Material. A stored preference still wins over all of it.
- `SurfaceTheme` defaults: yellow accent and title text, dimmed yellow secondary text, black background and cards, a yellow 2dp dialog border, yellow top-bar icons. `danger` / `success` stay inherited — those colors carry meaning, not identity.
- `BottomBarTheme` defaults: black bar, yellow selection, dimmed yellow unselected.
- New **`白い熊 Yellow` color preset**, now the default, with dark and light schemes, wired through the preset picker and the **Android TV** module (which shares the `theme_preset` key). Every accent-carrying role is overridden — **`surfaceTint` above all**, since Material tints every elevated surface with it and orange was leaking through *whatever* preset was selected — plus true black at every container level, so the scheme is black on its own rather than depending on AMOLED mode staying on.
- **Material You is off by default.** This also fixes a split where `dynamicColor` defaulted to `true` in one reader and `false` in another, so a fresh install painted itself with wallpaper colors while the Settings toggle read "off".
- `window_background` was upstream's light grey (navy at night); with the fork defaulting to Dark regardless of the system, a light-mode system **flashed white on every Activity transition**. Black in both.
- The logo art still carried upstream's orange gradient while the launcher icon was already yellow-on-black — recolored, TV module included. The **"(Default)" marker moves from Orange to Yellow in all 18 translated locales**, each in its own wording.

### 📐 Long version names no longer mangle the install dialog
- The dialog's "old → new" line was a plain `Row`: the installed version was measured first, ate the whole dialog width, and left the new version a few pixels in which to wrap itself into a **one-character-per-line vertical ribbon spilling past the card edge**. Version names like `6.3.0-alpha.2026-07-30.g5c0ed6a3+002` hit it every time.
- Both strings are now measured against the width actually available: they fit side by side → the row is unchanged; they don't → the pair **stacks with a downward arrow** and each version soft-wraps over as many lines as it needs. The measurement re-runs each composition on purpose, so a custom dialog font that resolves late corrects the verdict instead of freezing a stale one.
- The Details card in the menu ellipsised the same version mid-string; the value is now weighted and wrapping, so the **full version reads out** there too.

### 📦 Packaging: the build counter is zero-padded
- `versionName` is now `<VERSION_NAME>+<NNN>` — `1.9.11+016` — and so are the APK filename and the release tag. Unpadded, file lists sort lexicographically wrong (`+10` before `+3`), burying the newest build in the middle of the downloads list and of the phone's file manager.
- `versionCode` keeps the plain integer (`VERSION_CODE * 10000 + BUILD_NUMBER` = `310016`) and `gradle.properties` keeps `BUILD_NUMBER` unpadded; the padding is applied where the version string is built. **Earlier releases keep their unpadded tags** — nothing was renamed, so `1.9.11+016` sorts before `1.9.11+11` for a while.

## 1.9.11+11

**New in this build:**

### 🔑 Shizuku authorization now works against 白い熊 雫
- Granting Shizuku access from inside the app used to do **nothing at all** on a phone running **白い熊 雫** (`shiroikuma.shizuku`, the ShizukuPlus fork) without its optional Compat Hub — no dialog, no error, no log line.
- Cause: the only `API_V23` permission this app requested was `moe.shizuku.manager.permission.API_V23`, merged in from `dev.rikka.shizuku:provider`. 白い熊 雫 deliberately **does not define** that name (defining it is what would stop it installing beside stock Shizuku), so the server's `grantRuntimePermission()` targeted a permission **no installed package defines**, threw inside the server process, and was caught and logged only there — nothing propagated back, so the prompt looked like it never happened.
- Fix: the manifest declares **both** names — `af.shizuku.plus.permission.API_V23` and `moe.shizuku.manager.permission.API_V23`. `ShizukuService.updateFlagsForUid` grants the first name the caller requests in a fixed priority order (`af.shizuku.plus.*` → `moe.shizuku.manager.*` → `af.shizuku.manager.*`), so **ours wins on 白い熊 雫 while stock Shizuku still resolves to the `moe.*` name exactly as before**. A `uses-permission` naming a permission no installed package defines is inert, so one build works against either manager and the Compat Hub stub is no longer needed.
- Manifest only: no code change, no dependency change, no `<queries>` entry (`QUERY_ALL_PACKAGES` already covers visibility). Verify with `adb shell dumpsys package shiroikuma.universalinstaller | grep API_V23`.

## 1.9.11+10

**New in this build:**

### 🤖 保存復元 state-export automation contract
- The app now implements the **sister-app state-export contract**, so 白い熊 自由作業盤's 保存復元 project can back it up headlessly in the same run as every other app: two exported broadcast actions, `shiroikuma.universalinstaller.action.EXPORT_STATE` and `…action.LIST_CATEGORIES`.
- **Token-gated**: a master switch (**default off**) plus a 24-byte shared secret, compared constant-time, with "automation disabled" and "bad token" reported as distinct errors. The token lives in its own device-local preferences file, so it **can never travel inside a backup**.
- Both rows sit **inside the Export / Import section**, directly below the existing export rows: an **Automation export** switch and an **Automation token** row that copies the full token on tap and carries a **Regenerate** action.
- `EXPORT_STATE` runs the normal export with no UI: an optional absolute `path` overrides the configured directory (All-Files-Access, plain file I/O), optional `items` selects categories, and the reply broadcast carries `OK:<path>|<bytes>|<human size>|<n> categories` — exactly one reply per request, sent as a fresh broadcast (no binders, no ordered-result reliance — the only channel EMUI carries reliably).
- **Progress broadcasts with real numbers, never a percentage** (`区分 3/9 — Shizuku & Root options`), throttled to one per 500 ms with the completing one always sent, alongside structured `current` / `total` / `unit` extras.
- `LIST_CATEGORIES` enumerates the exportable ids as `id⇥label`, with sub-options carrying their parent id — the imported fonts are now a **selectable sub-option** of the Installer UI theme category.
- Every reply is echoed to logcat under the tag `StateExport` (the token never is), so the contract can be traced on the signed build with `adb logcat -s StateExport`.

### 💾 Export is now one ZIP per app (family file-name convention)
- The backup is a **category ZIP** — `manifest.json` plus one `<category>.json` per selected category, with the imported fonts stored alongside as real files instead of base64 — written both by the Export/Import panel and by the automation path.
- File name follows the family convention: `shiroikuma-universal-installer_2026-07-25_18-58-23.zip` — **no version, no infix, no suffix** — so every 白い熊 app's backups sort and read uniformly in one shared directory.
- **Older single-JSON exports still import** unchanged, and the "latest export" line recognises both.

## 1.9.11+8

**New in this build:**

### 💾 Whole-app config Export/Import (Kōjiki-style)
- **Export / Import is the first section of the 白い熊 Installer UI page**, opening a black, yellow-bordered panel.
- **Settable export directory** (SAF, grant persisted; device-local — never itself exported): set it once and export is **one tap**; without it, export falls back to the system Save-As picker.
- The directory is **queried on opening the page** and shows the **latest export** (`Last export: <timestamp>`); warnings ("no directory set", "no export yet") in red, statuses in yellow.
- Coverage grew from UI-only to **everything settable in the app**, split into **8 selectable categories**: Installer UI theme (imported fonts embedded), App theme (mode & preset), Install behavior, Shizuku & Root options, Installer profiles, Security & VirusTotal, Sync & Share, Manage & APK extractor — with a **Select all** master checkbox.
- **Old UI-only export files still import** (key-based filtering, versioned format).
- **Arcanechat-style button row**: round black pills with yellow text and border — Cancel separate on the left, Import and Export on the right.
- **Finished-info dialogs** (black, yellow border, pill buttons): export success shows the file name and its **OK closes the info dialog, the panel and the UI page in one go**; import success lists restored categories and offers **Restart now** (in-place app relaunch) or **Later** (closes the whole chain). Failures ("Export failed…", "No categories selected.") keep the panel open.

### 🖤💛 kxkb-style UI page & black/yellow accents
- The **whole UI page restyled to the kxkb look**: flat sections, 20 sp yellow headings underlined **exactly text-wide** (1.5 dp for sub-headings), thin full-width hairline separators between sections, yellow page title, **all page texts in yellow** (secondary text dimmed yellow).
- All selector chips (weight, per-button, per-text, inherit) are now **black pills with yellow text and border**, thicker border + bold when selected.
- **Install page**: storage-fill line always in the accent (the old fill-level lavender/red tinting is gone; a per-surface progress override still wins), **Local/Download as black/yellow pills** (thicker border + bold on the selected one), **yellow borders on the select-file cards**, and the **bottom bar** gains a yellow top border with yellow unselected Manage/Settings icons and labels (still themable per element).

### ⬆️ Upstream sync (post-1.9.11 main)
- Rebased onto upstream `main` past 1.9.11: **Android TV UI/UX overhaul** (10-foot type, focus, privileged Manage, theming, kept-alive tabs), **new Polish locale** (fork app label asserted there too), translation fills across ~17 locales, a **strict VirusTotal** install option, a split-ABI selection fix, and compiler-warning cleanups.

Tracks upstream **1.9.11 (versionCode 31)** with the full 白い熊 fork layer on top — cumulative:

## 1.9.11+3

**New in this build:** the install-backend badge (the “Using Shizuku / Root” pill) is now a **black pill
with yellow text, icon and border** by default — and fully configurable per-surface — and the
**install-engine picker dialog gains a yellow border**.

Tracks upstream **1.9.11 (versionCode 31)** — inherits its Android TV root install, privileged
Manage actions, and install-progress work — with the full 白い熊 fork layer on top:

### 🎨 Theming engine (白い熊 Installer UI page)
- New **白い熊 Installer UI** settings screen, reachable from Settings, with pickers and entry points.
- **Typeface**: use the system font, monospace, or **import your own `.ttf` / `.otf` fonts**; applied app-wide to every Material text style.
- Font **weight** (100–900) and **size scale** controls.
- **"Monospace for technical text"** toggle — package names, versions and sizes render in a mono face.
- Global **accent color** with a custom color picker and **recent-color hotpicks** (most-recent-first, deduped).
- Global **corner roundness** (shape) scale.

### 🪟 Per-surface & per-element styling
- **Per-surface overrides** for the install dialog and the main page — accent, title text, secondary text, card, background, danger, success and highlight colors, plus border and font — each independent, inheriting the global theme where unset (applied via a nested Material 3 theme so every element restyles live).
- **Per-button** styling in the install dialog (menu / install / cancel — background, content, border color & width).
- **Per-text-category** styling in the install dialog (app label, version, size, … — color, weight, size).
- **Themable install-dialog progress line** (color & thickness).
- **Custom success badge** in the install dialog (circle color, tick color, ring & tick thickness).
- **Backend badge** (“Using Shizuku / Root / PackageInstaller” pill): black pill with yellow text, icon and border by default, with per-surface overrides for background, text/icon, border color & width (install dialog + main page).
- **Yellow border on the install-engine picker dialog** (opened by tapping the badge).
- **Bottom navigation bar** theme.
- **Main-page top-bar action-icon tint.**
- **Continuous sliders** with **live per-text style preview** while dragging.

### 💾 Configuration backup
- **Export / import** the entire UI configuration — all preferences **plus** imported font files — as a single JSON file.

### ⚫🟡 Branding
- Custom **black/yellow download launcher icon** (all densities, round, monochrome + Play-store icon).
- **Splash screen**: black background, black/yellow icon, yellow title + tagline.
- App label **"白い熊 Universal installer"** across all 18 locales (`translatable="false"`).

### 🔧 Behavior & fixes
- **Removed the false "data may be wiped" downgrade warning** stock shows on downgrades where it doesn't apply.

### 📦 Packaging
- Distinct **applicationId `shiroikuma.universalinstaller`** so it coexists with the official app; code namespace unchanged (`app.pwhs.universalinstaller`), so R / BuildConfig / AIDL / FileProvider are untouched.
- Fork versioning: `versionName = "<upstream>+<build>"`, monotonic `versionCode`; release artifacts named `shiroikuma-universal-installer_<version>.apk`.
- Removed upstream `FUNDING.yml` (the fork doesn't inherit upstream funding).
- Ships the full single release — real root installs via libsu alongside Shizuku and the default system installer — signed for GitHub distribution.
