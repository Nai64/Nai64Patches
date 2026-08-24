package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import org.w3c.dom.Element
import java.util.logging.Logger

@Suppress("unused")
val keepScreenOnPatch = resourcePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Adds android:keepScreenOn to every theme so the display stays on while any " +
            "activity of the app is visible",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val resDir = get("res", false)
        if (!resDir.isDirectory) {
            logger.warning("No res directory found. No changes applied.")
            return@execute
        }

        var updatedThemes = 0
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

                            // Only touch theme-like styles; plain styles would ignore the item.
                            val name = style.getAttribute("name")
                            val parent = style.getAttribute("parent")
                            if (!parent.contains("Theme", ignoreCase = true) &&
                                !name.contains("Theme", ignoreCase = true)
                            ) {
                                continue
                            }

                            val items = style.getElementsByTagName("item")
                            var found = false
                            for (j in 0 until items.length) {
                                val item = items.item(j) as? Element ?: continue
                                if (item.getAttribute("name") == "android:keepScreenOn") {
                                    if (item.textContent != "true") {
                                        item.textContent = "true"
                                        changed = true
                                    }
                                    found = true
                                    break
                                }
                            }
                            if (!found) {
                                val item = doc.createElement("item")
                                item.setAttribute("name", "android:keepScreenOn")
                                item.textContent = "true"
                                style.appendChild(item)
                                changed = true
                            }
                        }
                        if (changed) updatedThemes++
                    }
                }.onFailure { logger.warning("Could not parse $relativePath: ${it.message}") }
            }

        if (updatedThemes > 0) {
            logger.info("Enabled keepScreenOn in $updatedThemes style file(s)")
        } else {
            logger.warning("No themes found. No changes applied.")
        }
    }
}
