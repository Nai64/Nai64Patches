package patches.universal.ui

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

@Suppress("unused")
val immersiveFullscreenPatch = resourcePatch(
    name = "Immersive Fullscreen",
    description =
        "Adds android:windowFullscreen to every theme so activities render edge-to-edge " +
            "without the status bar",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

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
                                if (item.getAttribute("name") == "android:windowFullscreen") {
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
                                item.setAttribute("name", "android:windowFullscreen")
                                item.textContent = "true"
                                style.appendChild(item)
                                changed = true
                            }
                        }
                        if (changed) updatedFiles++
                    }
                }.onFailure { logger.warning("Could not parse $relativePath: ${it.message}") }
            }

        if (updatedFiles > 0) {
            logger.info("Enabled windowFullscreen in $updatedFiles style file(s)")
        } else {
            logger.warning("No themes found. No changes applied.")
        }
    }
}
