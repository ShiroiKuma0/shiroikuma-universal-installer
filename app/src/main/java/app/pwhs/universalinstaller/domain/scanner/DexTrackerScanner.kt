package app.pwhs.universalinstaller.domain.scanner

import android.net.Uri
import app.pwhs.universalinstaller.domain.model.TrackerInfo
import org.json.JSONArray
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object DexTrackerScanner {

    internal data class TrackerRule(
        val id: Int,
        val name: String,
        val patterns: List<String>,
        val categories: List<String>,
        val website: String?,
    )

    internal class TrackerTrie {
        class Node {
            val children = HashMap<Char, Node>()
            val rules = mutableListOf<TrackerRule>()
        }

        val root = Node()

        fun insert(pattern: String, rule: TrackerRule) {
            var curr = root
            for (ch in pattern) {
                curr = curr.children.getOrPut(ch) { Node() }
            }
            curr.rules.add(rule)
        }

        inline fun search(className: String, onMatch: (TrackerRule) -> Unit) {
            var curr = root
            val len = className.length
            for (i in 0 until len) {
                val ch = className[i]
                curr = curr.children[ch] ?: return
                if (curr.rules.isNotEmpty()) {
                    val nextIdx = i + 1
                    if (nextIdx == len || className[nextIdx] == '.' || className[nextIdx] == '$') {
                        for (r in curr.rules) {
                            onMatch(r)
                        }
                    }
                }
            }
        }
    }

    @Volatile
    private var cachedRules: List<TrackerRule>? = null
    @Volatile
    private var cachedTrie: TrackerTrie? = null

    internal fun getOrLoadRules(context: android.content.Context): Pair<List<TrackerRule>, TrackerTrie> {
        val rules = cachedRules
        val trie = cachedTrie
        if (rules != null && trie != null) return rules to trie
        return synchronized(this) {
            val r = cachedRules ?: try {
                context.assets.open("exodus_trackers.json").use { parseRulesJson(it) }
            } catch (e: Exception) {
                emptyList()
            }.also { cachedRules = it }

            val t = cachedTrie ?: buildTrie(r).also { cachedTrie = it }
            r to t
        }
    }

    internal fun buildTrie(rules: List<TrackerRule>): TrackerTrie {
        val trie = TrackerTrie()
        for (rule in rules) {
            for (pattern in rule.patterns) {
                trie.insert(pattern, rule)
            }
        }
        return trie
    }

    internal fun loadRules(jsonStream: InputStream): List<TrackerRule> {
        cachedRules?.let { return it }
        val rules = parseRulesJson(jsonStream)
        cachedRules = rules
        cachedTrie = buildTrie(rules)
        return rules
    }

    internal fun parseRulesJson(stream: InputStream): List<TrackerRule> {
        val jsonText = stream.bufferedReader().use { it.readText() }
        val array = JSONArray(jsonText)
        val list = ArrayList<TrackerRule>(array.length())
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val id = obj.getInt("id")
            val name = obj.getString("name")
            val sig = obj.optString("signature", "")
            val website = if (obj.has("website") && !obj.isNull("website")) obj.getString("website") else null
            val categories = ArrayList<String>()
            val catArr = obj.optJSONArray("categories")
            if (catArr != null) {
                for (c in 0 until catArr.length()) {
                    categories.add(catArr.getString(c))
                }
            }
            if (sig.isNotBlank()) {
                val patterns = sig.split('|')
                    .map { it.trim().trimEnd('.') }
                    .filter { it.isNotBlank() }
                if (patterns.isNotEmpty()) {
                    list.add(
                        TrackerRule(
                            id = id,
                            name = name,
                            patterns = patterns,
                            categories = categories,
                            website = website,
                        )
                    )
                }
            }
        }
        return list
    }

    /**
     * Scans an APK file for trackers using the application context to load Exodus Privacy rules.
     */
    fun scanApk(apkFile: File, context: android.content.Context): List<TrackerInfo> {
        val (_, trie) = getOrLoadRules(context)
        return scanApk(apkFile, trie)
    }

    /**
     * Scans an APK from a Uri (file or content) using application context rules.
     */
    fun scanApk(context: android.content.Context, uri: Uri): List<TrackerInfo> {
        val (_, trie) = getOrLoadRules(context)
        return try {
            if (uri.scheme == "file") {
                val path = uri.path
                if (path != null) {
                    val file = File(path)
                    if (file.exists() && file.length() > 0) {
                        return scanApk(file, trie)
                    }
                }
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedInputStream(stream, 64 * 1024).use { bis ->
                    scanApkStream(bis, trie)
                }
            } ?: emptyList()
        } catch (t: Throwable) {
            Timber.w(t, "DexTrackerScanner: failed to scan uri %s", uri)
            emptyList()
        }
    }

    /**
     * Scans an APK file for trackers matching the loaded Exodus Privacy rules.
     */
    internal fun scanApk(apkFile: File, rules: List<TrackerRule>): List<TrackerInfo> {
        val trie = cachedTrie ?: buildTrie(rules)
        return scanApk(apkFile, trie)
    }

    internal fun scanApk(apkFile: File, trie: TrackerTrie): List<TrackerInfo> {
        if (!apkFile.exists() || apkFile.length() <= 0) return emptyList()
        val detectedTrackers = LinkedHashMap<Int, TrackerInfo>()

        try {
            ZipFile(apkFile).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.name.startsWith("classes") && entry.name.endsWith(".dex")) {
                        zip.getInputStream(entry).use { dexStream ->
                            val dexBytes = dexStream.readBytes()
                            scanDexBytes(dexBytes, trie, detectedTrackers)
                        }
                    }
                }
            }
        } catch (t: Throwable) {
            Timber.w(t, "DexTrackerScanner: failed to scan %s", apkFile.path)
        }

        return detectedTrackers.values.toList()
    }

    /**
     * Scans an APK from a streaming InputStream using the TrackerTrie.
     */
    internal fun scanApkStream(stream: InputStream, trie: TrackerTrie): List<TrackerInfo> {
        val detectedTrackers = LinkedHashMap<Int, TrackerInfo>()
        try {
            val zis = ZipInputStream(stream)
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name.startsWith("classes") && entry.name.endsWith(".dex")) {
                    val dexBytes = zis.readBytes()
                    scanDexBytes(dexBytes, trie, detectedTrackers)
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        } catch (t: Throwable) {
            Timber.w(t, "DexTrackerScanner: failed to scan APK stream")
        }
        return detectedTrackers.values.toList()
    }

    internal fun scanApkStream(stream: InputStream, rules: List<TrackerRule>): List<TrackerInfo> {
        val trie = cachedTrie ?: buildTrie(rules)
        return scanApkStream(stream, trie)
    }

    private fun scanDexBytes(
        dexBytes: ByteArray,
        trie: TrackerTrie,
        detectedTrackers: MutableMap<Int, TrackerInfo>,
    ) {
        if (dexBytes.size < 112) return
        val buffer = ByteBuffer.wrap(dexBytes).order(ByteOrder.LITTLE_ENDIAN)

        // Check DEX magic: "dex\n"
        val magic0 = buffer.get(0)
        val magic1 = buffer.get(1)
        val magic2 = buffer.get(2)
        val magic3 = buffer.get(3)
        if (magic0 != 'd'.code.toByte() || magic1 != 'e'.code.toByte() ||
            magic2 != 'x'.code.toByte() || magic3 != '\n'.code.toByte()
        ) {
            return
        }

        val stringIdsSize = buffer.getInt(0x38)
        val stringIdsOff = buffer.getInt(0x3C)
        val typeIdsSize = buffer.getInt(0x40)
        val typeIdsOff = buffer.getInt(0x44)
        val classDefsSize = buffer.getInt(0x60)
        val classDefsOff = buffer.getInt(0x64)

        if (classDefsSize <= 0 || classDefsOff <= 0 || classDefsOff >= dexBytes.size) return

        // Extract class descriptors
        for (i in 0 until classDefsSize) {
            val classDefOffset = classDefsOff + (i * 32)
            if (classDefOffset + 4 > dexBytes.size) break
            val classIdx = buffer.getInt(classDefOffset)

            if (classIdx in 0 until typeIdsSize) {
                val typeOffset = typeIdsOff + (classIdx * 4)
                if (typeOffset + 4 > dexBytes.size) continue
                val descriptorIdx = buffer.getInt(typeOffset)

                if (descriptorIdx in 0 until stringIdsSize) {
                    val stringIdOffset = stringIdsOff + (descriptorIdx * 4)
                    if (stringIdOffset + 4 > dexBytes.size) continue
                    val stringDataOff = buffer.getInt(stringIdOffset)

                    val className = readDexClassString(dexBytes, stringDataOff) ?: continue
                    
                    // Match against rules using prefix Trie in O(L)
                    trie.search(className) { rule ->
                        if (!detectedTrackers.containsKey(rule.id)) {
                            detectedTrackers[rule.id] = TrackerInfo(
                                id = rule.id,
                                name = rule.name,
                                categories = rule.categories,
                                website = rule.website,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun readDexClassString(dexBytes: ByteArray, offset: Int): String? {
        if (offset < 0 || offset >= dexBytes.size) return null
        var cursor = offset

        // Read ULEB128 utf16 size
        var value = 0
        var shift = 0
        while (cursor < dexBytes.size) {
            val b = dexBytes[cursor++].toInt()
            value = value or ((b and 0x7F) shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
            if (shift >= 35) return null
        }

        // Read MUTF-8 string until null terminator
        val start = cursor
        while (cursor < dexBytes.size && dexBytes[cursor] != 0.toByte()) {
            cursor++
        }
        val length = cursor - start
        if (length <= 2) return null

        // Check if starts with 'L' and ends with ';'
        if (dexBytes[start] == 'L'.code.toByte() && dexBytes[cursor - 1] == ';'.code.toByte()) {
            val chars = CharArray(length - 2)
            for (i in 0 until (length - 2)) {
                val b = dexBytes[start + 1 + i].toInt().toChar()
                chars[i] = if (b == '/') '.' else b
            }
            return String(chars)
        }
        return null
    }
}
