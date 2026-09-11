package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val ignoreTouchExplorationPatch = bytecodePatch(
    name = "Ignore Touch Exploration",
    description =
        "Makes AccessibilityManager.isTouchExplorationEnabled always return false so games " +
            "that disable or alter gameplay under TalkBack behave normally",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Ignore") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/view/accessibility/AccessibilityManager;" to
                    mapOf("isTouchExplorationEnabled" to "0x0"),
            ),
        )

        if (patched > 0) logger.info("Hid $patched touch-exploration check(s)")
        else logger.warning("No touch-exploration checks found. No changes applied.")
    }
}
