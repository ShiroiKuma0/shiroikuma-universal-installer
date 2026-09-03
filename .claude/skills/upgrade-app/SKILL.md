---
name: upgrade-app
description: Automates the process of upgrading the app version. Bumps versionCode and versionName in build.gradle.kts for changed modules, updates changelogs, verifies the build, commits, pushes, and creates a GitHub release. Use when the user says "upgrade app", "release new version", or "bump version".
---

# Upgrade App Workflow

This skill automates the end-to-end process of releasing a new version across all application modules (`:app`, `:wearos`, `:tv`).

## Module Architecture & Version Code Bands
The project has three form factors sharing the same package name (`app.pwhs.universalinstaller`) on Google Play Store:
- **Phone (`:app`):** `versionCode` band 1 - 999 (e.g. 36 -> 37)
- **Wear OS (`:wearos`):** `versionCode` band 1000 - 1999 (e.g. 1036 -> 1037)
- **Android TV (`:tv`):** `versionCode` band 2000 - 2999 (e.g. 2027 -> 2028)

## Workflow Steps

### 1. Preparation
- Ensure the git working tree is clean (`git status`).
- Check `gh auth status` to ensure GitHub CLI is authenticated.
- Identify the last git tag:
  ```bash
  LAST_TAG=$(git describe --tags --abbrev=0)
  ```

### 2. Diff Inspection & Selective Versioning
- **MANDATORY:** Check which modules actually have changes since `$LAST_TAG`:
  ```bash
  git diff --name-only $LAST_TAG..HEAD | grep -E '^(app|wearos|tv)/'
  ```
- **Increment rules:**
  - Increment `versionCode` by 1 **ONLY for modules that have changes**.
  - If a module has **no changes** (e.g. `:tv`), do **NOT** bump its `versionCode`. The Fastlane smart deploy logic uses `version_codes_to_retain` to preserve that module's existing build on Google Play Console without rebuilding or re-uploading.
- **versionName:**
  - Suggest a unified `versionName` (e.g., if current is `1.13.0`, suggest `1.13.1`).
  - Set the new `versionName` on all bumped modules.

### 3. Update Files & Changelogs
- Update `build.gradle.kts` for each module that changed.
- Fetch user-facing commits since the last tag:
  ```bash
  git log $LAST_TAG..HEAD --oneline
  ```
- **Filter commits:**
  - Exclude commits that are only about translations or language updates (e.g., `i18n`, `translation`, `locale`, `strings.xml`).
  - **Strip issue references:** Remove all GitHub issue references (e.g., `#70`, `(#72)`, `#121`) from the changelog text. The changelog is for end users, not issue tracking.
- Generate concise changelogs **only for changed modules**:
  - Phone: `fastlane/metadata/android/en-US/changelogs/<new-app-versionCode>.txt` (if `:app` changed)
  - Wear OS: `fastlane_wearos/metadata/android/en-US/changelogs/<new-wear-versionCode>.txt` (if `:wearos` changed)
  - TV: `fastlane_tv/metadata/android/en-US/changelogs/<new-tv-versionCode>.txt` (if `:tv` changed)
- **MANDATORY:** Check the character count of each changelog file (`wc -c <file_path>`). They **MUST NOT exceed 500 characters** (Google Play limit). Shorten if necessary.

### 4. User Confirmation
- **MANDATORY:** Present the detected module changes, version bump plan, and generated changelogs to the user.
- **WAIT** for the user to confirm or edit before proceeding to build and release.

### 5. Build Verification
- Run build verification before creating commits/tags:
  ```bash
  JAVA_HOME="/Applications/Android Studio Preview.app/Contents/jbr/Contents/Home" ./gradlew assembleDebug
  ```
- If it fails, stop and report errors.

### 6. Git & GitHub Operations (MUST BE SEQUENTIAL)
- Execute commit, tag, and release:
  ```bash
  git add app/build.gradle.kts wearos/build.gradle.kts tv/build.gradle.kts fastlane/ fastlane_wearos/ fastlane_tv/ && \
  git commit -m "chore: bump version to <versionName> (<details>)" && \
  git tag v<versionName> && \
  git push origin main && \
  git push origin v<versionName> && \
  gh release create v<versionName> --title "v<versionName>" --notes-file fastlane/metadata/android/en-US/changelogs/<new-app-versionCode>.txt
  ```

## Guardrails
- **Selective Bump:** Never bump a module whose code has not changed since the last tag.
- **Race Condition Prevention:** Never separate `git commit` and `git tag` into different tool calls without ensuring the commit succeeded.
- **Validation:** Always verify the build with `./gradlew assembleDebug` before pushing.
- **Confirmation:** Always wait for user confirmation on the changelog and version bump plan.
- **Changelog Limit:** All changelogs MUST be strictly under 500 characters. Always verify with `wc -c`.
- **No Issue References:** Never include GitHub issue numbers in user-facing changelogs.
- **No Translation Commits:** Never include routine i18n/translation commits in the changelog.
