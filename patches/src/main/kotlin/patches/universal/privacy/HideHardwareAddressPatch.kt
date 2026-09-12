package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.replaceArrayGetterWithEmpty

@Suppress("unused")
val hideHardwareAddressPatch = bytecodePatch(
    name = "Hide Hardware Address",
    description = "Returns an empty hardware address for network interfaces so apps cannot read the device MAC",
    default = false,
) {
    category("Privacy")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = replaceArrayGetterWithEmpty(
            "Ljava/net/NetworkInterface;",
            setOf("getHardwareAddress"),
            "B",
        )
        if (patched > 0) logger.info("Hid hardware address at $patched call site(s)")
        else logger.warning("No hardware address calls found. No changes applied.")
    }
}
