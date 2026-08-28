package patches.universal.misc

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val forceIgnoreAudioFocusPatch = bytecodePatch(
    name = "Force Ignore Audio Focus",
    description = "Makes AudioManager.requestAudioFocus always return granted so apps never duck or pause for other audio.",
    default = false,
) {
    val alwaysGrant by booleanOption(
        title = "Always grant",
        default = true,
        key = "ignoreAudioFocus",
        description = "If true, focus is always granted; if false, always denied.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val value = if (alwaysGrant == true) "0x1" else "0x0"
        // requestAudioFocus has multiple overloads, all return I (AUDIOFOCUS_REQUEST_GRANTED=1)
        val patched = foldBooleanReturns(
            mapOf(
                "Landroid/media/AudioManager;" to mapOf(
                    "requestAudioFocus" to value,
                ),
            ),
            returnType = "I",
        )
        if (patched > 0) logger.info("Forced audio focus at $patched call site(s) -> $value")
        else logger.warning("No requestAudioFocus call sites found. No changes applied.")
    }
}
