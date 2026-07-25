# 白い熊 Universal installer — changelog

Everything this fork adds on top of stock **Universal Installer**
([pass-with-high-score/universal-installer](https://github.com/pass-with-high-score/universal-installer)).
Installs side-by-side with the official app (app id `shiroikuma.universalinstaller`).

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
