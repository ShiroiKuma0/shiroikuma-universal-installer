---
name: build-apk
description: Build the signed full release APK of 白い熊's Universal Installer fork (app id shiroikuma.universalinstaller, label "白い熊 Universal installer") via the buildFork Gradle task, then always ask whether to push it to the connected phone via adb. Always build first without asking for permission to build — the ONLY question you ever ask is the adb-push question afterward. Use whenever the user asks to build the app, build the APK, make a release build, or build and push to the phone.
---

# Build the full release APK and optionally push to phone

> **Never ask whether to build — just build.** When this skill applies (the user
> asked to build, or you've made changes that are ready to test), run the build
> immediately. Do **not** ask "shall I build?" / "want me to run buildFork?" — that
> question is wrong. The **only** question in this whole flow is the `AskUserQuestion`
> about the `adb push`, asked **after** a successful build. So: always build, *then*
> ask about the push.

> **The push destination is ALWAYS `/sdcard/tmp/`.** Every `adb push` of the APK
> goes to `/sdcard/tmp/<apk name>` — **never** `/sdcard/Download/` or anywhere
> else. Create `/sdcard/tmp` if needed and push there.

> **Never run `adb install` (or `pm install`).** The build step may copy the APK
> to the phone with `adb push` — and only after confirming with the user — but
> **the user installs the APK themselves** from the phone's file manager. Do not
> install it for them under any circumstances.

> **Never `git commit` or `git push` on your own.** Building does not include
> committing. After building (and the optional `adb push`), the user tests the
> build themselves. **Only when the user explicitly says "Push"** do you then
> `git commit` the changes and `git push origin custom`. The user's **"Push"**
> means *commit-and-push-to-the-fork* — it is unrelated to the `adb push` file
> copy in step 4.

> **ALWAYS end every build by asking — via `AskUserQuestion` — whether to
> `adb push` the APK to `/sdcard/tmp/`.** This is mandatory and applies to
> *every* successful build, even verification builds and even when the user
> didn't mention pushing. Do **not** settle for asking in prose ("say the word")
> or assuming the answer — fire the `AskUserQuestion` prompt as the final step
> (step 3) of the build, every time.

## What this fork builds

A fork of **pass-with-high-score/universal-installer** (Kotlin + Jetpack Compose, Gradle). We ship the
**`full`** flavor only (all power-user features incl. root install via libsu — the GitHub-distribution
flavor, **not** the Play `store` flavor). Identity: app id `shiroikuma.universalinstaller`, label
`白い熊 Universal installer`; the code namespace stays `app.pwhs.universalinstaller`. See `CLAUDE.md` →
"Fork model" for the full fork layer.

## Steps

1. **Note the output filename.** Read the current version and build number from `gradle.properties`:
   - `grep -E 'VERSION_NAME|VERSION_CODE|BUILD_NUMBER' gradle.properties`
   - The APK will be `shiroikuma-universalinstaller_<VERSION_NAME>+<BUILD_NUMBER>.apk`, using the
     `BUILD_NUMBER` value **before** the build (the task bumps it afterward), e.g.
     `shiroikuma-universalinstaller_1.8.2+1.apk`.
   - versionCode for that build = `VERSION_CODE * 10000 + BUILD_NUMBER` (e.g. `16*10000+1 = 160001`).

2. **Build** (needs JDK 21 — the default `java` on this machine is JDK 11, and the non-interactive shell
   does not source the user's profile, so set `JAVA_HOME` in the invocation):
   - `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew buildFork < /dev/null`
     (the `< /dev/null` guarantees it never blocks on stdin)
   - This runs `assembleFullRelease`, copies the signed APK to `~/tmp/<apk name>`, and auto-increments
     `BUILD_NUMBER` in `gradle.properties`.
   - The task prints `>>> <path>` and `>>> versionCode <n>`; use those to confirm the exact filename and
     code, and confirm `BUILD SUCCESSFUL`.
   - The Android SDK location comes from the gitignored `local.properties` (`sdk.dir`); no `ANDROID_HOME`
     export is needed. The first `full`-flavor build resolves `topjohnwu.libsu` from jitpack, so it needs
     network.

3. **At the end of every build, ALWAYS ask** via `AskUserQuestion` whether to push the APK to the phone —
   no exceptions, no assuming, no asking only in prose. Options: "Yes, push via adb" / "No, just build".
   Fire this prompt as soon as the build reports `BUILD SUCCESSFUL`, regardless of whether the user
   mentioned pushing.

4. **If yes, push directly yourself:**
   - `adb devices` — confirm a device is connected.
   - `adb shell mkdir -p /sdcard/tmp`
   - `adb push ~/tmp/<apk name> /sdcard/tmp/<apk name>`
   - Verify: `adb shell ls -l /sdcard/tmp/<apk name>` (size should match the local file in `~/tmp`).
   - Never `adb install` — the user installs manually from `/sdcard/tmp/`.

## Note — push directly, do not rely on a task prompt

The `buildFork` task (`app/build.gradle.kts`) has **no** interactive prompt — it only builds, copies the
APK to `~/tmp`, and bumps `BUILD_NUMBER`. Asking the user and running `adb push` is Claude's job
(steps 3–4), done conversationally.

## Signing

Release signing is non-interactive: `app/build.gradle.kts` reads credentials from the gitignored
`key.properties` at the repo root (keys `storeFile` / `storePassword` / `keyAlias` / `keyPassword`). This
fork uses its own keystore `~/.android-keystores/shiroikuma-universalinstaller.jks` (alias
`universalinstaller`). If `key.properties` is absent the `release` build is left **unsigned** and the APK
will not install — recreate it pointing at the keystore. The keystore is stable, so reinstalls upgrade
in place.

## Coexistence with the official app

The fork installs side-by-side with the official Universal Installer because the `applicationId`
(`shiroikuma.universalinstaller`) differs. Do **not** try to install it over the official app — different
signing keys, Android will refuse. The two FileProvider/Shizuku `<provider>` authorities are
`${applicationId}`-based, so they auto-differ; no manual authority edits are needed.
