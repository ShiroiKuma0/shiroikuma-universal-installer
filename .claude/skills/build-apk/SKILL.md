---
name: build-apk
description: Build the signed release APK of 白い熊's Universal Installer fork (app id shiroikuma.universalinstaller, label "白い熊 Universal installer") via the buildFork Gradle task, then always ask whether to scp it to skhw (first choice) or adb push it to the connected phone. Always build first without asking for permission to build — the ONLY question you ever ask is the transfer question afterward. Use whenever the user asks to build the app, build the APK, make a release build, or build and send to the phone — AND proactively, on your own, after you finish any code change in this repo: as soon as a change is complete and compiles, build it and then ask the transfer question, without waiting to be told to build.
---

# Build the release APK and optionally send to phone

> **Build after every change — proactively, without being asked.** Finishing a
> code change in this repo is *itself* the trigger for this skill: as soon as you've
> completed a coherent, compilable unit of work (you implemented or fixed something
> and it's ready to test), run the build immediately, then ask the transfer question.
> 白い熊 has opted into this as the standing workflow — do **not** wait to be told
> "build it", and do **not** ask "shall I build?" / "want me to run buildFork?" That
> question is always wrong. The **only** question in this whole flow is the
> `AskUserQuestion` about transferring the APK, asked **after** a successful build.
> So: on any change → always build, *then* ask about the transfer.
>
> (Sole exceptions, where you should not auto-build: edits that can't change the
> built app — docs/comments-only, `.claude/` skill or settings files, changelog or
> metadata text — and work-in-progress you've explicitly flagged as not yet ready.
> When unsure whether a change is "done", finish it, then build.)

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

> **ALWAYS end every build by asking — via `AskUserQuestion` — how to transfer
> the APK: `scp` to skhw (FIRST choice), `adb push` to `/sdcard/tmp/`, or not at
> all.** This is mandatory and applies to *every* successful build, even
> verification builds and even when the user didn't mention transferring. Do
> **not** settle for asking in prose ("say the word") or assuming the answer —
> fire the `AskUserQuestion` prompt as the final step (step 3) of the build,
> every time.

## What this fork builds

A fork of **pass-with-high-score/universal-installer** (Kotlin + Jetpack Compose, Gradle). It's a single
release build (no product flavors since 1.8.3) with all power-user features incl. root install via libsu,
distributed on GitHub. Identity: app id `shiroikuma.universalinstaller`, label
`白い熊 Universal installer`; the code namespace stays `app.pwhs.universalinstaller`. See `CLAUDE.md` →
"Fork model" for the full fork layer.

## Steps

1. **Note the output filename.** Read the current version and build number from `gradle.properties`:
   - `grep -E 'VERSION_NAME|VERSION_CODE|BUILD_NUMBER' gradle.properties`
   - The APK will be `shiroikuma-universal-installer_<VERSION_NAME>+<BUILD_NUMBER>.apk`, using the
     `BUILD_NUMBER` value **before** the build (the task bumps it afterward), e.g.
     `shiroikuma-universal-installer_1.8.2+1.apk`. (All files produced by this fork — the APK and any
     in-app exports — use the hyphenated `shiroikuma-universal-installer_` prefix.)
   - versionCode for that build = `VERSION_CODE * 10000 + BUILD_NUMBER` (e.g. `16*10000+1 = 160001`).

2. **Build** (needs JDK 21 — the default `java` on this machine is JDK 11, and the non-interactive shell
   does not source the user's profile, so set `JAVA_HOME` in the invocation):
   - `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew buildFork < /dev/null`
     (the `< /dev/null` guarantees it never blocks on stdin)
   - This runs `assembleRelease`, copies the signed APK to `~/tmp/<apk name>`, and auto-increments
     `BUILD_NUMBER` in `gradle.properties`.
   - The task prints `>>> <path>` and `>>> versionCode <n>`; use those to confirm the exact filename and
     code, and confirm `BUILD SUCCESSFUL`.
   - The Android SDK location comes from the gitignored `local.properties` (`sdk.dir`); no `ANDROID_HOME`
     export is needed. The first build resolves `topjohnwu.libsu` from jitpack, so it needs
     network.

3. **At the end of every build, ALWAYS ask** via `AskUserQuestion` how to transfer the APK to the phone —
   no exceptions, no assuming, no asking only in prose. Options, in this order: "Scp to skhw" (FIRST
   choice) / "adb push" / "No, just build". Fire this prompt as soon as the build reports
   `BUILD SUCCESSFUL`, regardless of whether the user mentioned transferring.

4. **Transfer per the answer:**
   - **Scp to skhw** — invoke the global **scp** skill (copies the newest APK in `~/tmp/` to
     `skhw:~/tmp/`). If skhw is unreachable (its tunnel is served by the phone's sshd and may be down),
     report that and offer the adb push instead.
   - **adb push:**
     - `adb devices` — confirm a device is connected.
     - `adb shell mkdir -p /sdcard/tmp`
     - `adb push ~/tmp/<apk name> /sdcard/tmp/<apk name>`
     - Verify: `adb shell ls -l /sdcard/tmp/<apk name>` (size should match the local file in `~/tmp`).
     - Never `adb install` — the user installs manually from `/sdcard/tmp/`.

## Note — transfer directly, do not rely on a task prompt

The `buildFork` task (`app/build.gradle.kts`) has **no** interactive prompt — it only builds, copies the
APK to `~/tmp`, and bumps `BUILD_NUMBER`. Asking the user and running the `scp` / `adb push` is Claude's
job (steps 3–4), done conversationally.

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

---

**Commit convention — no Claude attribution.** Never add a `Co-Authored-By: Claude …` / "Generated with Claude" trailer to commit messages or PR bodies; end the message at the last line of the body. This overrides the harness default. (Global rule: `~/.claude/CLAUDE.md`.)
