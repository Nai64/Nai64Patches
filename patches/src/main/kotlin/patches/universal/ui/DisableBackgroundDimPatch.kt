package patches.universal.ui

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element
import patches.universal.manifest.ensureThemeItem

@Suppress("unused")
val disableBackgroundDimPatch = resourcePatch(
    name = "Disable Background Dim",
    description = "Removes the dim behind dialogs and popups so the background stays fully bright",
    default = false,
) {
    category("Interface")
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
                            val name = style.getAttribute("name")
                            val parent = style.getAttribute("parent")
                            if (!parent.contains("Theme", ignoreCase = true) &&
                                !name.contains("Theme", ignoreCase = true)
                            ) continue
                            ensureThemeItem(doc, style, "android:backgroundDimEnabled", "false")
                            changed = true
                        }
                        if (changed) updatedFiles++
                    }
                }.onFailure { logger.warning("Could not parse $relativePath: ${it.message}") }
            }

        if (updatedFiles > 0) {
            logger.info("Disabled background dim in $updatedFiles style file(s)")
        } else {
            logger.warning("No themes found. No changes applied.")
        }
    }
}
