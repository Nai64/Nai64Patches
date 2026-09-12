package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldObjectGetterToNull
import patches.universal.misc.replaceArrayGetterWithEmpty

@Suppress("unused")
val hideInputDevicesPatch = bytecodePatch(
    name = "Hide Input Devices",
    description = "Hides physical keyboards, gamepads and other input devices so apps cannot fingerprint your hardware",
    default = false,
) {
    category("Privacy")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += replaceArrayGetterWithEmpty(
            "Landroid/hardware/input/InputManager;",
            setOf("getInputDeviceIds"),
            "I",
        )
        patched += foldObjectGetterToNull(
            "Landroid/hardware/input/InputManager;",
            setOf("getInputDevice"),
            "Landroid/view/InputDevice;",
        )
        if (patched > 0) logger.info("Hid input devices at $patched call site(s)")
        else logger.warning("No input device calls found. No changes applied.")
    }
}
