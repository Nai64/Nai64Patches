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
    description = "Makes dark mode pure black for AMOLED screens. Light mode stays normal.",
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
        val hexRegex = Regex("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$")
        val amoled = if (rawColor.matches(hexRegex)) {
            // normalize #RGB -> #FFRRGGBB etc. for consistency, but keep original if valid
            rawColor
        } else "#FF000000"
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

        // Full M3 / Material You dark tokens (v31+). Covers m3_sys_color_dark_*, md_theme_dark_*, etc.
        val surfaceTargets = listOf(
            "colorSurface", "android:colorSurface",
            "colorSurfaceVariant", "android:colorSurfaceVariant",
            "colorSurfaceContainer", "colorSurfaceContainerLow", "colorSurfaceContainerLowest",
            "colorSurfaceContainerHigh", "colorSurfaceContainerHighest",
            "colorSurfaceBright", "colorSurfaceDim",
            "colorPrimaryContainer", "colorSecondaryContainer", "colorTertiaryContainer",
            "colorOnPrimaryContainer", "colorOnSecondaryContainer",
            "background", "surface", "surfaceVariant",
            "outline", "outlineVariant", "scrim", "inverseSurface", "inverseOnSurface", "inversePrimary",
            "elevationOverlayColor", "surfaceColor", "surfaceContainer",
            "m3_sys_color_dark_surface", "m3_sys_color_dark_background", "m3_sys_color_dark_surfaceContainer",
            "md_theme_dark_surface", "md_theme_dark_background", "material_dynamic_dark_surface"
        )
        // Denylist: never blacken text/icon colors that should stay light on black
        val denylist = setOf(
            "colorOnSurface", "android:colorOnSurface",
            "colorOnBackground", "android:colorOnBackground",
            "colorOnSurfaceVariant", "colorOnPrimary", "colorOnSecondary",
            "colorOnSurfaceInverse", "colorSurfaceInverse"
        )
        val filteredSurfaceTargets = surfaceTargets.filter { it !in denylist }
        val targets = buildList {
            addAll(baseTargets)
            if (blackenSurfaces == true) addAll(filteredSurfaceTargets)
        }

        var updatedStyles = 0
        var updatedColors = 0
        var updatedColorStateLists = 0
        var updatedDrawables = 0

        // Helper to check if value is a color we should replace (dark grey, not accent, not transparent)
        fun shouldReplaceColor(value: String): Boolean {
            val v = value.trim()
            if (v.equals("@android:color/transparent", ignoreCase = true) || v.equals("@color/transparent", ignoreCase = true)) return false
            if (v.startsWith("@") || v.startsWith("?")) return false // reference, handled via theme attrs
            return v.matches(Regex("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$"))
        }

        // 1) Style pass – values*/ night dirs + values-night colors.xml
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
                            val colors = doc.getElementsByTagName("color")
                            var changed = false
                            for (i in 0 until colors.length) {
                                val c = colors.item(i) as? Element ?: continue
                                val name = c.getAttribute("name") ?: continue
                                val lower = name.lowercase()
                                val isSurfaceColor = lower.contains("background") ||
                                    lower.contains("surface") ||
                                    lower.contains("card") ||
                                    lower.contains("window_background") ||
                                    lower.contains("colorprimarysurface") ||
                                    lower.contains("elevation") ||
                                    lower.contains("m3_sys") ||
                                    lower.contains("md_theme")
                                if (!isSurfaceColor) continue
                                val value = c.textContent?.trim() ?: continue
                                if (shouldReplaceColor(value)) {
                                    // keep transparent, replace only dark greys (heuristic: any valid hex, but we blacken all surfaces)
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

                            val isDark = isNightDir ||
                                name.contains("Dark", ignoreCase = true) ||
                                parent.contains("Dark", ignoreCase = true) ||
                                name.contains("Night", ignoreCase = true) ||
                                parent.contains("Night", ignoreCase = true) ||
                                name.contains("DayNight", ignoreCase = true) ||
                                parent.contains("DayNight", ignoreCase = true)
                            if (!isDark) continue
                            if (!isTheme && !isNightDir) continue
                            if (!isTheme && isNightDir) {
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
                            // Auto-fix text contrast: ensure onSurface stays light on pure black
                            if (blackenSurfaces == true) {
                                ensureThemeItem(doc, style, "colorOnSurface", "#FFFFFFFF")
                                ensureThemeItem(doc, style, "android:colorOnSurface", "#FFFFFFFF")
                            }
                            changed = true
                        }
                        if (changed) updatedStyles++
                    }
                }.onFailure { logger.warning("Could not parse $relativePath: ${it.message}") }
            }

        // 2) res/color* night ColorStateLists
        resDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }
            .filter { it.parentFile?.path?.contains("color", ignoreCase = true) == true }
            .filter { it.parentFile?.name?.contains("night", ignoreCase = true) == true }
            .forEach { file ->
                val relativePath = "res/" + file.relativeTo(resDir).invariantSeparatorsPath
                runCatching {
                    document(relativePath).use { doc ->
                        val colors = doc.getElementsByTagName("color")
                        var changed = false
                        for (i in 0 until colors.length) {
                            val c = colors.item(i) as? Element ?: continue
                            val value = c.getAttribute("android:color")?.takeIf { it.isNotEmpty() } ?: c.textContent?.trim() ?: continue
                            if (!shouldReplaceColor(value)) continue
                            // only blacken if it's a dark grey, not an accent; heuristic: check parent selector is for surface/background
                            if (c.getAttribute("android:color").isNotEmpty()) {
                                c.setAttribute("android:color", amoled)
                                changed = true
                            } else if (c.textContent?.trim()?.matches(Regex("^#.*")) == true) {
                                c.textContent = amoled
                                changed = true
                            }
                        }
                        if (changed) updatedColorStateLists++
                    }
                }.onFailure { logger.warning("Could not parse $relativePath: ${it.message}") }
            }

        // 3) res/drawable* night solids
        resDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("xml", ignoreCase = true) }
            .filter { it.parentFile?.path?.contains("drawable", ignoreCase = true) == true }
            .filter { it.parentFile?.name?.contains("night", ignoreCase = true) == true }
            .forEach { file ->
                val relativePath = "res/" + file.relativeTo(resDir).invariantSeparatorsPath
                runCatching {
                    document(relativePath).use { doc ->
                        val solids = doc.getElementsByTagName("solid")
                        var changed = false
                        for (i in 0 until solids.length) {
                            val s = solids.item(i) as? Element ?: continue
                            val color = s.getAttribute("android:color")?.takeIf { it.isNotEmpty() } ?: continue
                            if (!shouldReplaceColor(color)) continue
                            s.setAttribute("android:color", amoled)
                            changed = true
                        }
                        if (changed) updatedDrawables++
                    }
                }.onFailure { logger.warning("Could not parse $relativePath: ${it.message}") }
            }

        if (updatedStyles > 0 || updatedColors > 0 || updatedColorStateLists > 0 || updatedDrawables > 0) {
            logger.info("Applied AMOLED $amoled to $updatedStyles dark theme style(s), $updatedColors night color file(s), $updatedColorStateLists ColorStateList(s), $updatedDrawables drawable(s)")
        } else {
            logger.warning("No dark themes found. No changes applied.")
        }
    }
}
