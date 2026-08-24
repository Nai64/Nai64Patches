package patches.universal.ui

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger
import org.w3c.dom.Element
import patches.universal.manifest.ensureThemeItem

@Suppress("unused")
val statusBarIconsPatch = resourcePatch(
    name = "Status Bar Icons",
    description =
        "Forces the status bar and navigation bar icon color in every theme. Pick white icons " +
            "for dark backgrounds or dark icons for light backgrounds",
    default = false,
) {
    val iconColor by stringOption(
        title = "Icon color",
        default = "white",
        values = linkedMapOf(
            "White icons (dark backgrounds)" to "white",
            "Dark icons (light backgrounds)" to "dark",
        ),
        key = "statusBarIcons",
        description = "Color of the system bar icons while the app is open.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        // windowLightStatusBar=true renders dark icons; false renders white icons.
        val lightIcons = when (iconColor.orEmpty().trim().lowercase()) {
            "dark" -> "true"
            "white" -> "false"
            else -> {
                logger.warning("Invalid icon color \"$iconColor\". No changes applied.")
                return@execute
            }
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
                            ) {
                                continue
                            }

                            for (attribute in listOf("windowLightStatusBar", "windowLightNavigationBar")) {
                                ensureThemeItem(doc, style, "android:$attribute", lightIcons)
                                changed = true
                            }
                        }
                        if (changed) updatedFiles++
                    }
                }.onFailure { logger.warning("Could not parse $relativePath: ${it.message}") }
            }

        if (updatedFiles > 0) {
            logger.info(
                "Set status bar icons to $iconColor in $updatedFiles style file(s)",
            )
        } else {
            logger.warning("No themes found. No changes applied.")
        }
    }
}


