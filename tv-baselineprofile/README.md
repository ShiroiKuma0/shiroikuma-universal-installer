# TV baseline profile

Generates `tv/src/release/generated/baselineProfiles/baseline-prof.txt`, which AGP packages
into the release APK as `assets/dexopt/baseline.prof`. Every smooth TV app examined ships one
(Netflix, Downloader); ours did not until now.

## Generating

Gradle Managed Devices **cannot** be used here — AGP rejects `android-tv` system images with
"TV and Auto devices are presently not supported". So start a TV emulator by hand first:

```bash
# once
$ANDROID_HOME/cmdline-tools/latest/bin/avdmanager create avd \
    -n tv-profile -k "system-images;android-31;android-tv;arm64-v8a" -d tv_1080p

# each time
$ANDROID_HOME/emulator/emulator -avd tv-profile -no-snapshot &
./gradlew :tv:generateReleaseBaselineProfile
```

A phone emulator would also produce a profile, but a worse one: the journey drives D-pad
navigation, and the leanback launcher changes which paths are exercised.

## Verifying it shipped

```bash
unzip -l tv/build/outputs/apk/release/*.apk | grep dexopt
```
Expect `assets/dexopt/baseline.prof` and `baseline.profm`.
