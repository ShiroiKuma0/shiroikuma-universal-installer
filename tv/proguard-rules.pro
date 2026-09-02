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

# Line numbers survive R8 so Crashlytics can point at a line rather than a method.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Project rules (R8 enabled 2026-08-02; this module previously shipped unminified) ──

# NanoHTTPD serves the phone→TV upload endpoint. Its request handlers are resolved
# reflectively and its optional SSL/websocket paths reference classes we do not ship.
-keep class org.nanohttpd.** { *; }
-dontwarn org.nanohttpd.**

# Ackpine keeps install/uninstall session state in a Room database and restores sessions
# across process death; the entities and the generated DAO impls are instantiated by name.
-keep class ru.solrudev.ackpine.**.database.** { *; }
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keepclassmembers class * { @androidx.room.* <methods>; }

# Compose keeps its own rules, but the TV focus machinery resolves some modifiers through
# reflection when restoring focus after a configuration change.
-dontwarn androidx.tv.**
