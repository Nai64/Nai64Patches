package patches.universal.ui

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Document
import org.w3c.dom.Element

@Suppress("unused")
val amoledThemePatch = resourcePatch(
    name = "AMOLED Theme",
    description =
        "Forces pure-black (#FF000000) backgrounds and surfaces on every dark theme so apps that " +
            "support dark mode render true AMOLED-black instead of dark grey. Light themes are left " +
            "untouched, so text stays readable. Optional toggles can also blacken the system bars",
    default = false,
) {
    val blackenStatusBar by booleanOption(
        title = "Blacken Status Bar",
        default = false,
        key = "amoledBlackenStatusBar",
        description = "Also force the status bar background to pure black on dark themes.",
    )
    val blackenNavigationBar by booleanOption(
        title = "Blacken Navigation Bar",
        default = false,
        key = "amoledBlackenNavigationBar",
        description = "Also force the navigation bar background to pure black on dark themes.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val resDir = get("res", false)
        if (!resDir.isDirectory) {
            logger.warning("No res directory found. No changes applied.")
            return@execute
        }

        // Framework color attributes always exist, so overriding them never breaks compilation
        // on apps that do not use Material/AppCompat theming.
        val amoled = "#FF000000"
        val targets = buildList {
            add("android:colorBackground")
            add("android:colorBackgroundFloating")
            add("android:windowBackground")
            add("android:colorSurface")
            add("android:colorPrimarySurface")
            if (blackenStatusBar == true) add("android:statusBarColor")
            if (blackenNavigationBar == true) add("android:navigationBarColor")
        }

        var updatedStyles = 0
        resDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }
            .filter { it.parentFile?.name?.startsWith("values", ignoreCase = true) == true }
            .forEach { file ->
                val dirName = file.parentFile?.name ?: ""
                val isNightDir = dirName.startsWith("values-night", ignoreCase = true)
                val relativePath = "res/" + file.relativeTo(resDir).invariantSeparatorsPath
                runCatching {
                    document(relativePath).use { doc ->
                        val styles = doc.getElementsByTagName("style")
                        var changed = false
                        for (i in 0 until styles.length) {
                            val style = styles.item(i) as? Element ?: continue

                            // Only touch theme-like styles.
                            val name = style.getAttribute("name")
                            val parent = style.getAttribute("parent")
                            val isTheme = parent.contains("Theme", ignoreCase = true) ||
                                name.contains("Theme", ignoreCase = true)
                            if (!isTheme) continue

                            // Only dark themes: night resource dirs or an explicit Dark theme.
                            val isDark = isNightDir ||
                                name.contains("Dark", ignoreCase = true) ||
                                parent.contains("Dark", ignoreCase = true)
                            if (!isDark) continue

                            for (attribute in targets) {
                                ensureItem(doc, style, attribute, amoled)
                            }
                            changed = true
                        }
                        if (changed) updatedStyles++
                    }
                }.onFailure { logger.warning("Could not parse $relativePath: ${it.message}") }
            }

        if (updatedStyles > 0) {
            logger.info("Applied AMOLED black to $updatedStyles dark theme style(s)")
        } else {
            logger.warning("No dark themes found. No changes applied.")
        }
    }
}

/** Sets an existing theme item or appends a new one. */
private fun ensureItem(doc: Document, style: Element, attrName: String, value: String) {
    val items = style.getElementsByTagName("item")
    for (i in 0 until items.length) {
        val item = items.item(i) as? Element ?: continue
        if (item.getAttribute("name") == attrName) {
            item.textContent = value
            return
        }
    }
    val item = doc.createElement("item")
    item.setAttribute("name", attrName)
    item.textContent = value
    style.appendChild(item)
}
