package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val apkJunkCleanupPatch = resourcePatch(
    name = "Apk Junk Cleanup",
    description = "Removes unused CPU libraries to shrink the APK. Keep only your device's architecture.",
    default = false,
) {
    val keepArch by stringOption(
        title = "Keep architecture",
        default = "arm64-v8a",
        key = "keepArch",
        description = "Native libraries for other ABIs under lib/ will be deleted.",
        values = linkedMapOf(
            "arm64-v8a (most modern devices)" to "arm64-v8a",
            "armeabi-v7a (32-bit ARM)" to "armeabi-v7a",
            "x86 (32-bit Intel)" to "x86",
            "x86_64 (64-bit Intel)" to "x86_64",
        ),
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val selected = keepArch?.trim().takeIf { !it.isNullOrEmpty() } ?: "arm64-v8a"

        val libDir = get("lib", false)
        if (!libDir.isDirectory) {
            logger.warning("No lib/ directory found (no native libs). No changes applied.")
            return@execute
        }

        val archDirs = libDir.listFiles { f -> f.isDirectory } ?: emptyArray()
        if (archDirs.isEmpty()) {
            logger.warning("lib/ is empty. No changes applied.")
            return@execute
        }

        var removed = 0
        var kept = 0
        for (dir in archDirs) {
            if (dir.name == selected) {
                kept++
                continue
            }
            if (dir.deleteRecursively()) {
                logger.info("Removed lib/${dir.name}/")
                removed++
            } else {
                logger.warning("Could not delete lib/${dir.name}/")
            }
        }

        if (kept == 0) {
            logger.warning("Selected architecture $selected not found in lib/. No changes applied.")
        } else if (removed > 0) {
            logger.info("Kept $selected, removed $removed ABI(s)")
        } else {
            logger.info("Only $selected present. No changes applied.")
        }
    }
}
