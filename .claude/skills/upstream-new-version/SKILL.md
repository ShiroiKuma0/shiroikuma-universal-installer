---
name: upstream-new-version
description: Rebase our fork onto a new upstream release of pass-with-high-score/universal-installer. Use when the user says a new upstream version is out, asks to update/sync to upstream, bump to the new Universal Installer release, or rebase custom onto the latest upstream.
---

# Rebase the fork onto a new upstream release

This codifies the "new upstream version" half of the fork workflow. The goal: move `main` to the new
upstream release, replay our `custom` customizations on top of it, and produce a fresh `+1` build.

This is a fork of **pass-with-high-score/universal-installer** (Kotlin + Jetpack Compose, Gradle). There
is **no** patched-Commons step (that is a Fossify-sibling thing — ignore it).

> **Never `git push` or `git commit` unprompted, and never `adb install`.** Same hard rules as everyday
> development (see CLAUDE.md). After the rebase + build you stop and let the user test; you only
> `git push` when they explicitly say **"Push"**.

## Background — how versioning works here

The fork version is driven by `gradle.properties`; `app/build.gradle.kts` computes the applied values:

- `VERSION_NAME` / `VERSION_CODE` **track upstream** (upstream stores these as literals in
  `app/build.gradle.kts`; we mirror them into `gradle.properties`).
- `BUILD_NUMBER` is **our** fork increment. It **resets to `1`** on each new upstream version and bumps by
  `1` on every build (the `build-apk` / `buildFork` step does the bump).
- Fork `versionName` = `"<VERSION_NAME>+<BUILD_NUMBER>"`; `versionCode` = `VERSION_CODE * 10000 + BUILD_NUMBER`.

So when upstream's `versionCode` climbs (e.g. 16 → 17), our codes for the new line (`170001`, `170002`, …)
all exceed the previous line's (`160001`, …), keeping upgrades monotonic.

## Branch / remote model

- `upstream` = `pass-with-high-score/universal-installer` (HTTPS, **fetch-only**; push is disabled).
- `origin` = our fork `ShiroiKuma0/shiroikuma-universal-installer` (SSH, pushable).
- `main` mirrors `upstream/main` — never carries our changes, fast-forward only.
- `custom` carries all our commits and is rebased onto each new `main`. Upstream tags releases `vX.Y.Z`.

## Steps

1. **Fetch upstream:**
   - `git fetch upstream --tags`
   - Identify the new release. Upstream's default branch is `main`; releases are also tagged
     (`git tag --sort=-creatordate | head`). Confirm the new upstream `versionCode` / `versionName`:
     `git show upstream/main:app/build.gradle.kts | grep -E 'versionCode|versionName'`.

2. **Advance `main` to the new upstream release** (it mirrors upstream, no fork work lives there):
   - `git checkout main`
   - `git merge --ff-only upstream/main` (or `git reset --hard v<X.Y.Z>` to track an exact tag).

3. **Rebase `custom` onto the new `main`:**
   - `git checkout custom`
   - `git rebase main`
   - Resolve conflicts so **all** our customizations survive (see the table below). The conflict-prone
     files are **`app/build.gradle.kts`** (upstream bumps the `versionCode`/`versionName` literals that we
     replaced with `forkVersionCode`/`forkVersionName`), `gradle.properties`, and
     `values*/strings.xml`.

4. **Resolve the version conflict the right way.** Upstream changed the `versionCode = N` / `versionName =
   "X.Y.Z"` lines in `app/build.gradle.kts`; **keep OUR side** there (`versionCode = forkVersionCode`,
   `versionName = forkVersionName`, `applicationId = project.property("APP_ID")...`). Then read upstream's
   **new** values out of the conflict and write them into `gradle.properties`:
   - Set `VERSION_NAME` / `VERSION_CODE` to the **new upstream** values.
   - **Reset `BUILD_NUMBER` to `1`.**

5. **Verify our customizations are intact** (after resolving the rebase):

   | What | Expected value | Where |
   | --- | --- | --- |
   | Installed app ID | `shiroikuma.universalinstaller` | `gradle.properties` → `APP_ID` (applied as `applicationId`) |
   | Code namespace (UNCHANGED) | `app.pwhs.universalinstaller` | `app/build.gradle.kts` → `namespace` |
   | App launcher label | `白い熊 Universal installer` | `app_name` in `values/strings.xml` **and** `values-ro/strings.xml` (both `translatable="false"`) |
   | Fork version logic | `forkVersionName` / `forkVersionCode`, `base { archivesName = "shiroikuma-universal-installer_…" }`, `buildFork` task | `app/build.gradle.kts` |
   | Upstream version pins | `VERSION_NAME`/`VERSION_CODE` = new upstream; `BUILD_NUMBER=1` | `gradle.properties` |
   | We build the single release | task `:app:buildFork` → `assembleRelease` | `app/build.gradle.kts` |
   | Signing | reads gitignored `key.properties` → `shiroikuma-universalinstaller.jks` | `app/build.gradle.kts` (unchanged from upstream) |

   **Couplings to be aware of (do NOT "fix" them):** because the **namespace stays
   `app.pwhs.universalinstaller`**, the namespace-based literals upstream uses are still correct under our
   different `applicationId`:
   - `SettingViewModel.defaultInstallerComponent()` builds `ComponentName(context,
     "app.pwhs.universalinstaller.presentation.install.DialogInstallActivity")` — the class FQN is
     namespace-based (correct), and the package comes from the context (= our `applicationId`). Leave it;
     only update the string if upstream **moves/renames** that activity.
   - `ManualTargetedInstaller`'s internal broadcast action `app.pwhs.universalinstaller.INSTALL_STATUS_<id>`
     is a self-contained sender+receiver pair — harmless, left as-is.

   Sanity check: `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew :app:assembleRelease --dry-run`
   to confirm the build script still evaluates after the rebase.

6. **Build the new `+1`** via the **build-apk** skill
   (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./gradlew buildFork < /dev/null`), then **ask** before
   any `adb push`. This is the first build of the new upstream line (`<newVersion>+1`).

7. **Stop.** Let the user test. Commit/push only on their explicit **"Push"** (force-push may be needed for
   `custom` since rebasing rewrites history: `git push --force-with-lease origin custom`; `main` is a
   fast-forward: `git push origin main`).

## Notes

- Keep our changes a **small, legible layer** on top of upstream — prefer rebasing (linear history) over
  merging, so the customization set stays easy to audit and replay.
- If upstream restructures a file we customize, port our change to the new structure rather than forcing
  the old diff.
- `upstream` is configured push-disabled (read-only); only `origin` is pushable.

---

**Commit convention — no Claude attribution.** Never add a `Co-Authored-By: Claude …` / "Generated with Claude" trailer to commit messages or PR bodies; end the message at the last line of the body. This overrides the harness default. (Global rule: `~/.claude/CLAUDE.md`.)
