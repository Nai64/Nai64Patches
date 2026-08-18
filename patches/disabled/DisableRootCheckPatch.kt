package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableRootCheckPatch = bytecodePatch(
    name = "Disable Root Checks",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        RootBeerIsRootedFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """.trimIndent())
            logger.info("Disabled RootBeer isRooted check")
        }

        RootBeerDetectSuBinaryFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """.trimIndent())
            logger.info("Disabled RootBeer detectSuBinary check")
        }

        RootBeerDetectTestKeysFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x0
                return v0
            """.trimIndent())
            logger.info("Disabled RootBeer detectTestKeys check")
        }

        val applied = listOfNotNull(
            RootBeerIsRootedFingerprint.methodOrNull?.let { "isRooted" },
            RootBeerDetectSuBinaryFingerprint.methodOrNull?.let { "detectSuBinary" },
            RootBeerDetectTestKeysFingerprint.methodOrNull?.let { "detectTestKeys" },
        )
        logger.info("Disable Root Checks patch succeeded — ${applied.size} strategy/strategies applied: ${applied.joinToString(", ")}")
    }
}
