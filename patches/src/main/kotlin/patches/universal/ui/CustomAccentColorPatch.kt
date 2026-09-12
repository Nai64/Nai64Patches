package patches.universal.ui

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger
import org.w3c.dom.Element
import patches.universal.manifest.ensureThemeItem

@Suppress("unused")
val customAccentColorPatch = resourcePatch(
    name = "Custom Accent Color",
    description = "Replaces the app accent color (buttons, switches, highlights) in every theme",
    default = false,
) {
    category("Interface")
    val accentColor by stringOption(
        title = "Accent color",
        default = "#FF6200EE",
        key = "customAccentColor",
        description = "Hex color for accents (e.g. #FF6200EE purple, #FF03DAC5 teal, #FFFF0000 red).",
        values = linkedMapOf(
            "Material Purple" to "#FF6200EE",
            "Teal" to "#FF03DAC5",
            "Blue" to "#FF2196F3",
            "Red" to "#FFF44336",
            "Green" to "#FF4CAF50",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val raw = (accentColor ?: "#FF6200EE").trim()
        val hexRegex = Regex("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")
        if (!raw.matches(hexRegex)) {
            logger.warning("Invalid accent color '$raw'. No changes applied.")
            return@execute
        }

        val resDir = get("res", false)
        if (!resDir.isDirectory) {
            logger.warning("No res directory found. No changes applied.")
            return@execute
        }

        val appTargets = listOf(
            "colorPrimary",
            "colorAccent",
            "colorSecondary",
            "colorControlActivated",
            "colorControlHighlight",
        )
        val knownAppAttrs = mutableSetOf<String>()
        resDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }
            .filter { it.parentFile?.name?.startsWith("values", ignoreCase = true) == true }
            .forEach { file ->
                runCatching {
                    val doc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
                    doc.documentElement.normalize()
                    val attrs = doc.getElementsByTagName("attr")
                    for (i in 0 until attrs.length) {
                        val a = attrs.item(i) as? Element ?: continue
                        a.getAttribute("name")?.takeIf { it.isNotEmpty() }?.let { knownAppAttrs.add(it) }
                    }
                    val items = doc.getElementsByTagName("item")
                    for (i in 0 until items.length) {
                        val item = items.item(i) as? Element ?: continue
                        val n = item.getAttribute("name") ?: continue
                        if (n.isNotEmpty() && !n.startsWith("android:")) knownAppAttrs.add(n)
                    }
                }
            }

        var updatedFiles = 0
        val skipped = mutableSetOf<String>()
        resDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }
            .filter { it.parentFile?.name?.startsWith("values") == true }
            .forEach { file ->
                val relativePath = "res/" + file.relativeTo(resDir).invariantSeparatorsPath
                runCatching {
                    document(relativePath).use { doc ->
                        val styles = doc.getElementsByTagName("style")
                        var changed = false
                        for (i in 0 until styles.length) {
                            val style = styles.item(i) as? Element ?: continue
                            val name = style.getAttribute("name")
                            val parent = style.getAttribute("parent")
                            if (!parent.contains("Theme", ignoreCase = true) &&
                                !name.contains("Theme", ignoreCase = true)
                            ) continue
                            ensureThemeItem(doc, style, "android:colorAccent", raw)
                            for (attribute in appTargets) {
                                if (attribute !in knownAppAttrs) {
                                    skipped.add(attribute)
                                    continue
                                }
                                ensureThemeItem(doc, style, attribute, raw)
                            }
                            changed = true
                        }
                        if (changed) updatedFiles++
                    }
                }.onFailure { logger.warning("Could not parse $relativePath: ${it.message}") }
            }

        if (skipped.isNotEmpty()) {
            logger.info("Skipped ${skipped.size} unknown attribute(s): ${skipped.sorted().joinToString(", ")}")
        }
        if (updatedFiles > 0) {
            logger.info("Set accent color to $raw in $updatedFiles style file(s)")
        } else {
            logger.warning("No themes found. No changes applied.")
        }
    }
}
