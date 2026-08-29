# 白い熊 Universal installer — changelog

Everything this fork adds on top of stock **Universal Installer**
([pass-with-high-score/universal-installer](https://github.com/pass-with-high-score/universal-installer)).
Installs side-by-side with the official app (app id `shiroikuma.universalinstaller`).

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
