package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.replaceArrayGetterWithEmpty
import patches.universal.misc.replaceGetterWithStaticCall

@Suppress("unused")
val hideUsbDevicesPatch = bytecodePatch(
    name = "Hide USB Devices",
    description = "Reports no USB devices or accessories so apps cannot fingerprint connected hardware",
    default = false,
) {
    category("Privacy")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += replaceGetterWithStaticCall(
            "Landroid/hardware/usb/UsbManager;",
            setOf("getDeviceList"),
            "Ljava/util/HashMap;",
            "invoke-static {}, Ljava/util/Collections;->emptyMap()Ljava/util/Map;",
        )
        patched += replaceArrayGetterWithEmpty(
            "Landroid/hardware/usb/UsbManager;",
            setOf("getAccessoryList"),
            "Landroid/hardware/usb/UsbAccessory;",
        )
        if (patched > 0) logger.info("Hid USB devices at $patched call site(s)")
        else logger.warning("No USB device calls found. No changes applied.")
    }
}
