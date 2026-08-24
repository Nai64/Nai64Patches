package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassTestEnvironmentPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "Makes ActivityManager.isUserAMonkey() and isRunningInTestHarness() report false so apps that alter behavior under test/automation frameworks run normally.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/app/ActivityManager;" to mapOf(
                    "isUserAMonkey" to "0x0",
                    "isRunningInTestHarness" to "0x0",
                ),
            ),
        )
        if (patched > 0) logger.info("Forced $patched test-environment check(s) to false")
        else logger.warning("No test-environment checks found. No changes applied.")
    }
}
