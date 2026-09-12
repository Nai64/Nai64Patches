package patches.universal.ui

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger
import org.w3c.dom.Element
import patches.universal.manifest.ensureThemeItem

@Suppress("unused")
val customNavigationBarColorPatch = resourcePatch(
    name = "Custom Navigation Bar Color",
    description = "Sets a custom background color for the navigation bar in every theme",
    default = false,
) {
    category("Interface")
    val barColor by stringOption(
        title = "Navigation bar color",
        default = "#FF000000",
        key = "customNavigationBarColor",
        description = "Hex color for the navigation bar background (e.g. #FF000000 black, #FFFFFFFF white).",
        values = linkedMapOf(
            "Black" to "#FF000000",
            "Dark Gray" to "#FF212121",
            "White" to "#FFFFFFFF",
            "Transparent" to "#00000000",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val raw = (barColor ?: "#FF000000").trim()
        val hexRegex = Regex("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")
        if (!raw.matches(hexRegex)) {
            logger.warning("Invalid navigation bar color '$raw'. No changes applied.")
            return@execute
        }

        val resDir = get("res", false)
        if (!resDir.isDirectory) {
            logger.warning("No res directory found. No changes applied.")
            return@execute
        }

        var updatedFiles = 0
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
                            ensureThemeItem(doc, style, "android:navigationBarColor", raw)
                            changed = true
                        }
                        if (changed) updatedFiles++
                    }
                }.onFailure { logger.warning("Could not parse $relativePath: ${it.message}") }
            }

        if (updatedFiles > 0) {
            logger.info("Set navigation bar color to $raw in $updatedFiles style file(s)")
        } else {
            logger.warning("No themes found. No changes applied.")
        }
    }
}
