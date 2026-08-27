package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import java.util.logging.Logger

@Suppress("unused")
val fakeAvailableProcessorsPatch = bytecodePatch(
    name = "Fake Available Processors",
    description = "Reports a chosen CPU count through Runtime.availableProcessors() so apps that check core count see a high-end device.",
    default = false,
) {
    val processors by intOption(
        title = "Processor count",
        default = 8,
        key = "availableProcessors",
        description = "Number of processors to report (2-16): 4 (mid), 8 (high), 12 (very high).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgIntGetter(
            "Ljava/lang/Runtime;",
            setOf("availableProcessors"),
            processors ?: 8,
        )
        if (patched > 0) {
            logger.info("Faked available processors at $patched call site(s)")
        } else {
            logger.warning("No availableProcessors call sites found. No changes applied.")
        }
    }
}
