<div align="center">

<img src="app/src/main/ic_launcher-playstore.png" width="120" alt="白い熊 Universal installer icon" />

# 白い熊 Universal installer

**A package installer you can theme down to the last button, border and progress line.**

A fork of [pass-with-high-score/universal-installer](https://github.com/pass-with-high-score/universal-installer) with **major additions**: a full font/color/shape theming engine that is **black-and-yellow out of the box**, per-surface and per-element styling of the install dialog, custom imported fonts, a categorized **whole-app config export/import** with a one-tap export directory, **headless backup and restore for external automation** behind a verified-caller data door, and Shizuku that actually talks to **白い熊 雫**.

Installs **side-by-side** with the official app (app id `shiroikuma.universalinstaller`).

**📥 Latest release: [`1.14.0+001`](https://github.com/ShiroiKuma0/shiroikuma-universal-installer/releases/latest)** — [all releases & APK downloads »](https://github.com/ShiroiKuma0/shiroikuma-universal-installer/releases)

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

## 🤖 Headless backup, restore, and a door that checks who is knocking

Below the export rows sit an **Automation export** switch (**on** by default), a
**「Use authorization token?」** switch (**off** by default), and — only while that second switch is on
— the **automation token** you tap to copy. A sister-app task can back this app up without touching
the screen: it broadcasts `shiroikuma.universalinstaller.action.EXPORT_STATE` with an optional target
directory and an optional list of categories, and the app writes the same one-ZIP backup and answers
with its path, byte size and category count — reporting real counts (`区分 3/9 — Engines`) and which
category is being written as it works. `…action.LIST_CATEGORIES` enumerates what can be picked, and
`…action.CANCEL_EXPORT` stops a run in flight, deleting the partial rather than leaving half a
backup behind. Every archive is written to a `.part` name and renamed only once it is complete, so a
killed export can never masquerade as the latest good one.

The token is opt-in because the point is a **clean phone**: a pasted secret cannot survive a wipe,
and restoring an app together with its data happens on a device where nothing has been configured
yet. A token sent when none is required is ignored rather than refused, so a caller configured last
year keeps working.

Restoring needs to know **who is asking**, which a broadcast cannot answer — so it lives behind a
`ContentProvider` at `shiroikuma.universalinstaller.automation` instead. It answers `describe`,
`export`, `import` and `cancel`; it identifies its caller by **exact package name, uid, and a pinned
signing certificate**; and the payload moves through a **file descriptor the caller opens**, never a
path, so the backup stays inside the caller's own encryption and checksums. `import` exists only
here and has no broadcast action at all. The token never travels inside a backup.

---

## 🖤💛 kxkb-styled settings page

The 白い熊 Installer UI page follows the kxkb look: flat sections with yellow headings underlined **exactly as wide as the text**, hairline separators, all texts in yellow, and black pills with yellow borders for every chip and button. The same black/yellow treatment runs through the install page — accent storage bar, Local/Download pills, yellow-bordered file cards, and a yellow-edged bottom bar.

---

## ⚫🟡 Black and yellow, out of the box

The 白い熊 look is **compiled in as the default**, not something you configure: a fresh install already opens black with yellow text, items and borders — its own **白い熊 Yellow** color preset, dark and AMOLED, **Material You off** so wallpaper colors can never repaint the app. Every UI preference simply *resolves* to it when nothing is stored, so the settings page shows real values instead of blanks and "reset" returns here rather than to stock Material. Anything you set still wins. Plus a custom **black/yellow launcher icon** and a black splash with a yellow title + tagline, so the fork is unmistakable in your launcher and app switcher.

---

## 🔑 Shizuku that works with 白い熊 雫

Silent installs authorize against **白い熊 雫** (`shiroikuma.shizuku`) as well as stock Shizuku, with no Compat Hub stub in between. The app requests both `API_V23` permission names, so the server grants whichever one it actually defines — and it accepts **both binder envelopes**, the modern `rikka.shizuku.BinderContainer` a current server sends first and the legacy one the last published client library (13.1.5, from 2023) knows. Without that, the hand-off threw `ClassNotFoundException`, the binder was silently dropped, and Shizuku read as "not running" forever with the service up and the permission granted. The picker also **names the Shizuku you actually have** — "白い熊 雫 is installed but its service isn't running" — and offers a button to open it, instead of a generic hint about an app it cannot identify.

---

## 🔧 The install dialog tells you what it is about to do

Re-installing the version you already have no longer looks like an upgrade. Stock drew the *"installed → new"* arrow between two identical version strings and offered an **Update** button; here a same-`versionCode` file reads **`1.9.12 (32) — already installed`**, carries a **Same version** chip and a **Reinstall** button, all in green — deliberately not one of the theme's accent roles, since in the black-and-yellow scheme every one of those is yellow. A downgrade still gets the red ⚠ line and a **Downgrade** button. Three states, three colours, no guessing.

---

## 🔧 Install-flow fixes

Long version names — the `6.3.0-alpha.2026-07-30.g5c0ed6a3+002` kind our forks produce — used to tear the install dialog apart: the old version ate the full width and the new one wrapped into a one-character-per-line ribbon spilling past the card. The two are now measured first and, when they don't fit side by side, stacked with a downward arrow, each wrapping properly; the Details card wraps the full version too instead of ellipsising it mid-string. And the false **"data may be wiped"** downgrade warning stock shows even when it doesn't apply is gone.

---

## 🕵️ No telemetry, ever

Upstream 1.9.12 added Firebase Analytics and Crashlytics, kept to a `play` build flavor that needs a `google-services.json` the repo does not ship. **This fork never has one**, so every `play` variant is switched off at configuration time: the Firebase Gradle plugins are never applied and not a line of analytics or crash-reporting code is linked into the APK. Our build task is pinned to the `opensource` flavor explicitly rather than to the generic `assembleRelease`, so that stays true even if a Firebase config ever landed in a checkout.

---

## Built on Universal Installer

A fork of [pass-with-high-score/universal-installer](https://github.com/pass-with-high-score/universal-installer) (app id `shiroikuma.universalinstaller`, so it coexists with the official build), currently tracking upstream **1.14.0**. Upstream is a modern Material 3 package manager that installs **APK / APK+ / APKS / XAPK / APKM** (with split APKs and OBB), downloads and streams packages from URLs, manages installed apps, and silent-installs via **Shizuku, Dhizuku, microG, a custom shell authorizer or root** — with an Obtainium-style **app updater** (GitHub / GitLab / Codeberg / F-Droid / direct APK), **Android TV** and **Wear OS** companions, an encrypted whole-app backup, an embedded-tracker scanner, a package blacklist, VirusTotal scanning and installs confirmed straight from a notification. All credit for the core app goes to the upstream authors; this fork only layers the theming engine and branding on top. The code remains under the **GNU GPL v3**.

## Building

```bash
git clone git@github.com:ShiroiKuma0/shiroikuma-universal-installer.git
cd shiroikuma-universal-installer
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew assembleOpensourceRelease
```

The fork build task `:app:buildFork` assembles the signed release, copies it to `~/tmp/` as `shiroikuma-universal-installer_<version>_arm64-v8a.apk`, and bumps the build number. The build is restricted to **arm64-v8a**, so the suffix describes the file rather than decorating it. Signing reads a gitignored `key.properties`; without it the build is unsigned.
