package app.pwhs.universalinstaller.util

import app.pwhs.universalinstaller.domain.scanner.DexTrackerScanner
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.FileInputStream

class DexTrackerScannerTest {

    @Test
    fun testParseRulesJson() {
        val jsonFile = File("src/main/assets/exodus_trackers.json")
        assertTrue("exodus_trackers.json should exist", jsonFile.exists())

        val rules = DexTrackerScanner.parseRulesJson(FileInputStream(jsonFile))
        assertFalse("Rules list should not be empty", rules.isEmpty())
        assertTrue("Should have hundreds of rules", rules.size > 300)

        val firebaseAnalytics = rules.find { it.name.contains("Firebase Analytics", ignoreCase = true) }
        assertTrue("Should contain Firebase Analytics rule", firebaseAnalytics != null)
    }

    @Test
    fun testScanRealApkIfAvailable() {
        val apkFile = File("build/outputs/apk/debug/app-debug.apk")
        if (!apkFile.exists()) return

        val jsonFile = File("src/main/assets/exodus_trackers.json")
        val rules = DexTrackerScanner.loadRules(FileInputStream(jsonFile))

        val start = System.currentTimeMillis()
        val detected = DexTrackerScanner.scanApk(apkFile, rules)
        val duration = System.currentTimeMillis() - start

        println("Scanned ${apkFile.name} in ${duration}ms, detected ${detected.size} trackers: ${detected.map { it.name }}")
        assertTrue("Scan should finish in reasonable time", duration < 5000)
    }
}
