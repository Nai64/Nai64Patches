package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofNetworkOperatorNamePatch = bytecodePatch(
    name = "Spoof Network Operator Name",
    description = "Reports a chosen operator name through TelephonyManager.getNetworkOperatorName() so apps that check the carrier name see a generic value.",
    default = false,
) {
    val operatorName by stringOption(
        title = "Operator name",
        default = "Android",
        key = "networkOperatorName",
        description = "Operator name to report (e.g. Android, Verzion, T-Mobile).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldNoArgStringGetter(
            "Landroid/telephony/TelephonyManager;",
            setOf("getNetworkOperatorName"),
            operatorName ?: "Android",
        )
        if (patched > 0) {
            logger.info("Spoofed network operator name at $patched call site(s)")
        } else {
            logger.warning("No getNetworkOperatorName call sites found. No changes applied.")
        }
    }
}
