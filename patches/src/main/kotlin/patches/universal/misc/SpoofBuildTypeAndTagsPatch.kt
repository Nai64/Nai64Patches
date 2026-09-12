package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val spoofBuildTypeAndTagsPatch = bytecodePatch(
    name = "Spoof Build Type and Tags",
    description = "Reports a user build with release-keys tags so apps stop treating the device as rooted or debuggable.",
    default = false,
) {
    category("Spoof")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStaticStringField(
            "Landroid/os/Build;",
            mapOf("TYPE" to "user", "TAGS" to "release-keys"),
        )
        if (patched > 0) logger.info("Spoofed $patched build type/tag read(s)")
        else logger.warning("No build type/tag reads found. No changes applied.")
    }
}
