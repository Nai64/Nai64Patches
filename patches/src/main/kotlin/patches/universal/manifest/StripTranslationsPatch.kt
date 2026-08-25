package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

/**
 * Matches resource qualifier directories that carry *language* information only,
 * e.g. values-de, values-pt-rBR, values-b+sr+Latn. Configuration qualifiers such
 * as values-night, values-v23 or values-w360dp are intentionally kept.
 */
private val languageOnlyValuesDir = Regex("^values-(b\\+[a-zA-Z0-9_-]+|[a-z]{2,3}(-r[A-Z]{2})?)$")

@Suppress("unused")
val stripTranslationsPatch = resourcePatch(
    name = "Strip Translations",
    description =
        "Deletes all translated resources so only the default language remains, reducing " +
            "APK size. Android falls back to the default strings automatically.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val resDir = get("res", false)
        if (!resDir.isDirectory) {
            logger.warning("No res directory found. No changes applied.")
            return@execute
        }

        var removedDirs = 0
        var failedDirs = 0
        resDir.listFiles { file -> file.isDirectory }
            ?.filter { languageOnlyValuesDir.matches(it.name) }
            ?.forEach { dir ->
                if (dir.deleteRecursively()) {
                    logger.info("Removed ${dir.name}/")
                    removedDirs++
                } else {
                    logger.warning("Could not delete ${dir.name}/")
                    failedDirs++
                }
            }

        if (removedDirs > 0) {
            logger.info("Stripped $removedDirs translation director(ies)")
        } else if (failedDirs == 0) {
            logger.warning("No translation directories found. No changes applied.")
        }
    }
}
