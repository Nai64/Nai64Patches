package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideMockLocationPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description =
        "Makes Location.isMock and Location.isFromMockProvider always return false so apps " +
            "cannot detect spoofed GPS apps",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/location/Location;" to mapOf(
                    "isMock" to "0x0",
                    "isFromMockProvider" to "0x0",
                ),
            ),
        )

        if (patched > 0) logger.info("Hid $patched mock-location check(s)")
        else logger.warning("No mock-location checks found. No changes applied.")
    }
}
