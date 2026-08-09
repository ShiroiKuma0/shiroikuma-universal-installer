# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Line numbers survive R8 so Crashlytics can point at a line rather than a method. The mapping
# file uploaded alongside the `play` build is what turns the renamed classes back into ours; the
# renamed source file name is of no use to anyone, so it goes.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# libsu (com.topjohnwu.superuser) — full flavor only. Harmless on store (no-op when
# the class is absent). Kept defensively because JitPack-built artifacts occasionally
# drop consumer-rules in ways that break reflective shell plumbing under R8. If a
# release `full` build ever regresses to "NOT_ROOTED" while debug works, look here first.
-keep class com.topjohnwu.superuser.** { *; }
-keep class com.topjohnwu.superuser.internal.** { *; }
-dontwarn com.topjohnwu.superuser.**

# Shizuku.newProcess is package-private; we reach it via reflection to run one-shot `pm`
# commands without building a full UserService/AIDL layer. R8 would otherwise rename the
# method and break ShizukuShellExecutor silently in release builds.
-keepclassmembers class rikka.shizuku.Shizuku {
    private static *** newProcess(...);
}