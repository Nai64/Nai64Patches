package patches.universal.unlock

import app.morphe.patcher.patch.rawResourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

private data class ByteRule(
    val from: ByteArray,
    val mask: BooleanArray,
    val to: ByteArray,
    val line: Int,
)

private fun hexByte(token: String, line: Int): Byte {
    require(token.length == 2 && token.all {
        it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F'
    }) {
        "Invalid hex byte '$token' on rule line $line"
    }
    return token.toInt(16).toByte()
}

private fun parseRules(text: String): List<ByteRule> {
    val rules = mutableListOf<ByteRule>()
    text.lineSequence().forEachIndexed { index, raw ->
        val line = index + 1
        val body = raw.trim()
        if (body.isEmpty() || body.startsWith("#")) return@forEachIndexed
        val parts = body.split("=>", limit = 2)
        require(parts.size == 2) { "Rule on line $line must use FROM => TO" }
        val fromTokens = parts[0].trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        val toTokens = parts[1].trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        require(fromTokens.isNotEmpty() && fromTokens.size == toTokens.size) {
            "Rule on line $line has mismatched byte counts"
        }
        val from = ByteArray(fromTokens.size)
        val mask = BooleanArray(fromTokens.size)
        val to = ByteArray(fromTokens.size)
        for (i in fromTokens.indices) {
            if (fromTokens[i] == "??") {
                require(toTokens[i] != "??") { "Wildcard in replacement on line $line" }
                mask[i] = true
            } else {
                from[i] = hexByte(fromTokens[i], line)
            }
            to[i] = hexByte(toTokens[i], line)
        }
        rules.add(ByteRule(from, mask, to, line))
    }
    return rules
}

private fun matchOffsets(bytes: ByteArray, rule: ByteRule): List<Int> {
    if (bytes.size < rule.from.size) return emptyList()
    val hits = mutableListOf<Int>()
    for (start in 0..bytes.size - rule.from.size) {
        var matches = true
        for (i in rule.from.indices) {
            if (!rule.mask[i] && bytes[start + i] != rule.from[i]) {
                matches = false
                break
            }
        }
        if (matches) hits.add(start)
    }
    return hits
}

@Suppress("unused")
val freeInAppPurchasesPatch = rawResourcePatch(
    name = "Free In-app Purchases",
    default = false,
) {
    val rules by stringOption(
        key = "iapNativeRules",
        title = "Native byte rules",
        default = "",
        description = "One rule per line as FROM => TO using hex bytes. Use ?? in FROM for bytes that vary. Lines starting with # are ignored.",
        required = true,
    )
    val libraryFilter by stringOption(
        key = "iapNativeLibraries",
        title = "Native libraries",
        default = "",
        description = "Comma separated library paths to patch. Leave empty to scan every .so file under lib/.",
        required = false,
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val parsed = parseRules(rules ?: "")
        if (parsed.isEmpty()) {
            logger.warning("Free In-app Purchases: no native byte rules configured")
            return@execute
        }

        val requested = (libraryFilter ?: "").split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
        val candidates = listApkEntries("lib/")
            .filter { it.endsWith(".so") && (requested.isEmpty() || it in requested) }
            .sorted()
        require(candidates.isNotEmpty()) { "No native libraries found to patch" }

        var applied = 0
        var skipped = 0
        for (path in candidates) {
            val file = get(path, true)
            val original = file.readBytes()
            val patchedBytes = original.copyOf()
            for (rule in parsed) {
                val hits = matchOffsets(original, rule)
                if (hits.isEmpty()) {
                    skipped++
                    logger.info("Free In-app Purchases: rule ${rule.line} not present in $path")
                } else if (hits.size == 1) {
                    rule.to.copyInto(patchedBytes, hits[0])
                    applied++
                    logger.info("Free In-app Purchases: rule ${rule.line} applied at 0x${hits[0].toString(16)} in $path")
                } else {
                    skipped++
                    logger.warning("Free In-app Purchases: rule ${rule.line} skipped in $path with ${hits.size} matches")
                }
            }
            if (!patchedBytes.contentEquals(original)) file.writeBytes(patchedBytes)
        }

        if (applied == 0) {
            logger.warning("Free In-app Purchases: no native sites patched. Add rules for this game.")
        } else {
            logger.info("Free In-app Purchases: patched $applied native site(s), skipped $skipped rule check(s)")
        }
    }
}
