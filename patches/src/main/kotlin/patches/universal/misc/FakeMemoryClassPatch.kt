package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import java.util.logging.Logger

@Suppress("unused")
val fakeMemoryClassPatch = bytecodePatch(
    name = "Fake Memory Class",
    description = "Reports a chosen memory class through ActivityManager.getMemoryClass() so apps that check available heap see a high value.",
    default = false,
) {
    val memoryClass by intOption(
        title = "Memory class (MB)",
        default = 512,
        key = "memoryClass",
        description = "Memory class in MB (128-1024): 256 (low), 512 (high), 1024 (very high).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Landroid/app/ActivityManager;",
            setOf("getMemoryClass"),
            memoryClass ?: 512,
        )
        if (patched > 0) {
            logger.info("Faked memory class at $patched call site(s)")
        } else {
            logger.warning("No getMemoryClass call sites found. No changes applied.")
        }
    }
}
