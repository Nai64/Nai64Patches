package patches.universal.ui

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Document
import org.w3c.dom.Element

@Suppress("unused")
val transparentSystemBarsPatch = resourcePatch(
    name = "Transparent System Bars",
    description =
        "Forces the status bar and navigation bar to render transparent in every theme so " +
            "app content draws edge-to-edge behind them",
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

                            ensureItem(doc, style, "statusBarColor", "@android:color/transparent")
                            ensureItem(doc, style, "navigationBarColor", "@android:color/transparent")
                            ensureItem(doc, style, "windowDrawsSystemBarBackgrounds", "true")
                            changed = true
                        }
                        if (changed) updatedFiles++
                    }
                }.onFailure { logger.warning("Could not parse $relativePath: ${it.message}") }
            }

        if (updatedFiles > 0) {
            logger.info("Made system bars transparent in $updatedFiles style file(s)")
        } else {
            logger.warning("No themes found. No changes applied.")
        }
    }
}

/** Sets an existing theme item or appends a new one. */
private fun ensureItem(doc: Document, style: Element, name: String, value: String) {
    val items = style.getElementsByTagName("item")
    for (i in 0 until items.length) {
        val item = items.item(i) as? Element ?: continue
        if (item.getAttribute("name") == "android:$name") {
            item.textContent = value
            return
        }
    }
    val item = doc.createElement("item")
    item.setAttribute("name", "android:$name")
    item.textContent = value
    style.appendChild(item)
}
