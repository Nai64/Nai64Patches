package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideMockLocationPatch = bytecodePatch(
    name = "Hide Mock Location",
    description =
        "Makes Location.isMock and Location.isFromMockProvider always return false so apps " +
            "cannot detect spoofed GPS apps",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Hide") } catch (_: NoSuchMethodError) {}
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
