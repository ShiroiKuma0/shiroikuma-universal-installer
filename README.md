<div align="center">

<img src="app/src/main/ic_launcher-playstore.png" width="120" alt="白い熊 Universal installer icon" />

# 白い熊 Universal installer

**A package installer you can theme down to the last button, border and progress line.**

A fork of [pass-with-high-score/universal-installer](https://github.com/pass-with-high-score/universal-installer) with **major additions**: a full font/color/shape theming engine, per-surface and per-element styling of the install dialog, custom imported fonts, a categorized **whole-app config export/import** with a one-tap export directory, **headless token-gated backup** for external automation, and a black/yellow kxkb-styled settings page.

Installs **side-by-side** with the official app (app id `shiroikuma.universalinstaller`).

**📥 Latest release: [`1.9.11+10`](https://github.com/ShiroiKuma0/shiroikuma-universal-installer/releases/latest)** — [all releases & APK downloads »](https://github.com/ShiroiKuma0/shiroikuma-universal-installer/releases)

</div>

---

## 🎨 A real theming engine

The **白い熊 Installer UI** settings page turns the installer into something you actually style. A global **accent color** (custom color picker plus recent-color hotpicks), a **corner roundness** slider for app-wide shape, and a **typeface** section: choose the system font, monospace, or **import your own `.ttf` / `.otf`**, then dial in font **weight (100–900)** and **size**. A "monospace for technical text" toggle renders package names, versions and sizes in a mono face.

---

## 🪟 Per-surface overrides

The **install pop-up dialog** and the **main page** each carry their own look. Override accent, title text, secondary text, card, background, danger, success and highlight colors — plus border and font — independently per surface. Anything left untouched inherits the global theme, applied through a nested Material 3 theme so every element restyles live.

---

## 🔘 Style the install dialog down to the element

Beyond surface colors, the install dialog exposes **per-button** styling (menu / install / cancel — background, content, border), **per-text-category** styling (app label, version, size… — color, weight, size), a **themable progress line** (color & thickness), and a **custom success badge** (circle color, tick color, ring & tick thickness). Continuous sliders with **live preview** as you drag.

The **install-backend badge** — the “Using Shizuku / Root” pill — is a **black pill with yellow text and border** by default (on brand), and its background, text/icon and border are configurable per surface. The engine-picker dialog it opens carries a matching **yellow border**.

---

## 💾 Export & import the whole app configuration

The first section of the UI page is **Export / Import**: pick an **export directory** once and every export is one tap; the page shows the **latest export in that directory** each time you open it. Everything settable in the app rides along, split into **eight selectable categories** — Installer UI theme (with your imported fonts embedded, their own sub-option), app theme, install behavior, Shizuku & Root options, installer profiles, security & VirusTotal, Sync & Share, Manage & APK extractor — as **one ZIP** (`shiroikuma-universal-installer_<date>_<time>.zip`, a `manifest.json` plus one JSON per category and your fonts alongside). Import restores the categories you choose — older single-JSON exports still load — and offers an in-place **app restart**; the whole flow lives in black/yellow dialogs with round pill buttons.

---

## 🤖 Headless backup for 保存復元 automation

Below the export rows sit an **Automation export** switch (off by default) and an **automation token** you tap to copy. With them on, a sister-app task can back this app up without touching the screen: it broadcasts `shiroikuma.universalinstaller.action.EXPORT_STATE` with the token, an optional target directory and an optional list of categories, and the app writes the same one-ZIP backup and answers with its path, byte size and category count — reporting real counts (`区分 3/9 — Engines`) while it works. `…action.LIST_CATEGORIES` enumerates what can be picked. Nothing is reachable while the switch is off or the token does not match, and the token itself never travels inside a backup.

---

## 🖤💛 kxkb-styled settings page

The 白い熊 Installer UI page follows the kxkb look: flat sections with yellow headings underlined **exactly as wide as the text**, hairline separators, all texts in yellow, and black pills with yellow borders for every chip and button. The same black/yellow treatment runs through the install page — accent storage bar, Local/Download pills, yellow-bordered file cards, and a yellow-edged bottom bar.

---

## ⚫🟡 Black-and-yellow branding

A custom **black/yellow launcher icon**, and a splash with a black background, the black/yellow mark and a yellow title + tagline — so the fork is unmistakable in your launcher and app switcher.

---

## 🔧 Install-flow fix

Drops the false **"data may be wiped"** downgrade warning that stock shows even when it doesn't apply.

---

## Built on Universal Installer

A fork of [pass-with-high-score/universal-installer](https://github.com/pass-with-high-score/universal-installer) (app id `shiroikuma.universalinstaller`, so it coexists with the official build). Upstream is a modern Material 3 package manager that installs **APK / APK+ / APKS / XAPK / APKM** (with split APKs and OBB), downloads packages from URLs, manages installed apps, and silent-installs via **Shizuku or root** — now with **Android TV** support. All credit for the core app goes to the upstream authors; this fork only layers the theming engine and branding on top. The code remains under the **GNU GPL v3**.

## Building

```bash
git clone git@github.com:ShiroiKuma0/shiroikuma-universal-installer.git
cd shiroikuma-universal-installer
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew assembleRelease
```

The fork build task `:app:buildFork` assembles the signed release, copies it to `~/tmp/` as `shiroikuma-universal-installer_<version>.apk`, and bumps the build number. Signing reads a gitignored `key.properties`; without it the build is unsigned.
