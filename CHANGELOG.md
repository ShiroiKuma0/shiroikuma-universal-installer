# 白い熊 Universal installer — changelog

Everything this fork adds on top of stock **Universal Installer**
([pass-with-high-score/universal-installer](https://github.com/pass-with-high-score/universal-installer)).
Installs side-by-side with the official app (app id `shiroikuma.universalinstaller`).

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
