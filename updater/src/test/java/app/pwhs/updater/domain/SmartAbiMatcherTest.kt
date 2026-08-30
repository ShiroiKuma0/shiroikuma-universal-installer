package app.pwhs.updater.domain

import app.pwhs.updater.domain.matcher.SmartAbiMatcher
import app.pwhs.updater.domain.model.AssetArtifact
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class SmartAbiMatcherTest {

    @Test
    fun selectBestAsset_arm64Preferred_selectsArm64() {
        val assets = listOf(
            AssetArtifact(name = "app-armeabi-v7a.apk", downloadUrl = "http://example.com/v7a.apk"),
            AssetArtifact(name = "app-arm64-v8a.apk", downloadUrl = "http://example.com/arm64.apk"),
            AssetArtifact(name = "app-x86_64.apk", downloadUrl = "http://example.com/x86_64.apk"),
        )
        val selected = SmartAbiMatcher.selectBestAsset(
            assets = assets,
            deviceAbis = listOf("arm64-v8a", "armeabi-v7a"),
        )
        assertNotNull(selected)
        assertEquals("app-arm64-v8a.apk", selected?.name)
    }

    @Test
    fun selectBestAsset_aarch64Alias_selectsCorrectly() {
        val assets = listOf(
            AssetArtifact(name = "app-armv7.apk", downloadUrl = "http://example.com/v7.apk"),
            AssetArtifact(name = "app-aarch64.apk", downloadUrl = "http://example.com/aarch64.apk"),
        )
        val selected = SmartAbiMatcher.selectBestAsset(
            assets = assets,
            deviceAbis = listOf("arm64-v8a"),
        )
        assertNotNull(selected)
        assertEquals("app-aarch64.apk", selected?.name)
    }

    @Test
    fun selectBestAsset_fallbackToUniversal() {
        val assets = listOf(
            AssetArtifact(name = "source-code.zip", downloadUrl = "http://example.com/src.zip"),
            AssetArtifact(name = "app-universal.apk", downloadUrl = "http://example.com/universal.apk"),
        )
        val selected = SmartAbiMatcher.selectBestAsset(
            assets = assets,
            deviceAbis = listOf("arm64-v8a"),
        )
        assertNotNull(selected)
        assertEquals("app-universal.apk", selected?.name)
    }

    @Test
    fun selectBestAsset_emptyOrNoApk_returnsNull() {
        val assets = listOf(
            AssetArtifact(name = "checksum.txt", downloadUrl = "http://example.com/sum.txt"),
        )
        val selected = SmartAbiMatcher.selectBestAsset(
            assets = assets,
            deviceAbis = listOf("arm64-v8a"),
        )
        assertNull(selected)
    }
}
