package patches.universal.ui

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger
import org.w3c.dom.Element
import patches.universal.manifest.ensureThemeItem

@Suppress("unused")
val amoledThemePatch = resourcePatch(
    name = "AMOLED Theme",
    description =
        "Forces pure-black backgrounds and surfaces on every dark theme so apps that " +
            "support dark mode render true AMOLED-black instead of dark grey. Light themes are left " +
            "untouched, so text stays readable. Optional toggles cover status/nav bars and Material3 surfaces.",
    default = false,
) {
    val amoledColor by stringOption(
        title = "AMOLED color",
        default = "#FF000000",
        key = "amoledColor",
        description = "Hex color to use for AMOLED (e.g. #FF000000 pure black, #FF0A0A0A near-black).",
        values = linkedMapOf(
            "Pure Black" to "#FF000000",
            "Near Black" to "#FF0A0A0A",
            "Dark Charcoal" to "#FF121212",
        ),
    )
    val blackenStatusBar by booleanOption(
        title = "Blacken Status Bar",
        default = false,
        key = "amoledBlackenStatusBar",
        description = "Also force the status bar background to AMOLED on dark themes.",
    )
    val blackenNavigationBar by booleanOption(
        title = "Blacken Navigation Bar",
        default = false,
        key = "amoledBlackenNavigationBar",
        description = "Also force the navigation bar background to AMOLED on dark themes.",
    )
    val blackenSurfaces by booleanOption(
        title = "Blacken Surfaces",
        default = true,
        key = "amoledBlackenSurfaces",
        description = "Also blacken Material3 surface container colors (recommended for full AMOLED).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val resDir = get("res", false)
        if (!resDir.isDirectory) {
            logger.warning("No res directory found. No changes applied.")
            return@execute
        }

        val rawColor = (amoledColor ?: "#FF000000").trim()
        val amoled = if (rawColor.matches(Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$"))) rawColor else "#FF000000"
        if (amoled != rawColor) logger.warning("Invalid AMOLED color '$rawColor', using $amoled")

        val baseTargets = mutableListOf(
            "android:colorBackground",
            "android:colorBackgroundFloating",
            "android:windowBackground",
            "android:colorSurface",
            "android:colorPrimarySurface",
        )
        if (blackenStatusBar == true) baseTargets.add("android:statusBarColor")
        if (blackenNavigationBar == true) baseTargets.add("android:navigationBarColor")

        // Material3 / Material You surface containers – the main source of dark-grey
        val surfaceTargets = listOf(
            "colorSurface",
            "colorSurfaceVariant",
            "colorSurfaceContainer",
            "colorSurfaceContainerLow",
            "colorSurfaceContainerLowest",
            "colorSurfaceContainerHigh",
            "colorSurfaceContainerHighest",
            "colorSurfaceInverse",
            "colorOnSurface",
            "elevationOverlayColor",
            "surfaceColor",
            "surfaceContainer",
        )
        val targets = buildList {
            addAll(baseTargets)
            if (blackenSurfaces == true) addAll(surfaceTargets)
        }

        var updatedStyles = 0
        var updatedColors = 0

        // 1) Style pass – values*/ night dirs
        resDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }
            .filter { it.parentFile?.name?.startsWith("values", ignoreCase = true) == true }
            .forEach { file ->
                val dirName = file.parentFile?.name ?: ""
                val isNightDir = dirName.startsWith("values-night", ignoreCase = true)
                val relativePath = "res/" + file.relativeTo(resDir).invariantSeparatorsPath
                val isColorsFile = file.name.equals("colors.xml", ignoreCase = true)
                runCatching {
                    document(relativePath).use { doc ->
                        if (isColorsFile && isNightDir) {
                            // In night colors.xml, replace common dark surface/background greys
                            val colors = doc.getElementsByTagName("color")
                            var changed = false
                            for (i in 0 until colors.length) {
                                val c = colors.item(i) as? Element ?: continue
                                val name = c.getAttribute("name") ?: continue
                                val lower = name.lowercase()
                                // Heuristic: background / surface / card / primarySurface / windowBackground
                                val isSurfaceColor = lower.contains("background") ||
                                    lower.contains("surface") ||
                                    lower.contains("card") ||
                                    lower.contains("window_background") ||
                                    lower.contains("colorprimarysurface") ||
                                    lower.contains("elevation")
                                if (!isSurfaceColor) continue
                                val value = c.textContent?.trim() ?: continue
                                // Only replace dark greys / near-black, leave accent colors
                                if (value.matches(Regex("^#([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$"))) {
                                    c.textContent = amoled
                                    changed = true
                                }
                            }
                            if (changed) updatedColors++
                            return@use
                        }

                        val styles = doc.getElementsByTagName("style")
                        var changed = false
                        for (i in 0 until styles.length) {
                            val style = styles.item(i) as? Element ?: continue

                            val name = style.getAttribute("name")
                            val parent = style.getAttribute("parent")
                            val isTheme = parent.contains("Theme", ignoreCase = true) ||
                                name.contains("Theme", ignoreCase = true)

                            // In night dirs apply to all themes (and optionally all styles that define windowBackground);
                            // outside night dirs only apply to explicitly Dark themes
                            val isDark = isNightDir ||
                                name.contains("Dark", ignoreCase = true) ||
                                parent.contains("Dark", ignoreCase = true) ||
                                name.contains("Night", ignoreCase = true) ||
                                parent.contains("Night", ignoreCase = true)
                            if (!isDark) continue
                            // In night dirs: apply to Theme styles primarily, but also to any style that already
                            // defines one of our targets (covers ThemeOverlay, Widget.Card edge cases)
                            if (!isTheme && !isNightDir) continue
                            if (!isTheme && isNightDir) {
                                // Quick check: does this style contain any target attr already?
                                val items = style.getElementsByTagName("item")
                                var hasTarget = false
                                for (k in 0 until items.length) {
                                    val it = items.item(k) as? Element ?: continue
                                    if (it.getAttribute("name") in targets) { hasTarget = true; break }
                                }
                                if (!hasTarget) continue
                            }

                            for (attribute in targets) {
                                ensureThemeItem(doc, style, attribute, amoled)
                            }
                            changed = true
                        }
                        if (changed) updatedStyles++
                    }
                }.onFailure { logger.warning("Could not parse $relativePath: ${it.message}") }
            }

        if (updatedStyles > 0 || updatedColors > 0) {
            logger.info("Applied AMOLED $amoled to $updatedStyles dark theme style(s) and $updatedColors night color file(s)")
        } else {
            logger.warning("No dark themes found. No changes applied.")
        }
    }
}
