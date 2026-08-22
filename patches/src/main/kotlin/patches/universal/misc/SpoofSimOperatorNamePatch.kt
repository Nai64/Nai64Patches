package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofSimOperatorNamePatch = bytecodePatch(
    name = "Spoof SIM Operator Name",
    description =
        "Reports a chosen carrier name from TelephonyManager.getSimOperatorName() and " +
            "getNetworkOperatorName() so apps cannot read the real carrier display name.",
    default = false,
) {
    val name by stringOption(
        title = "Operator name",
        default = "T-Mobile",
        key = "spoofSimOperatorName",
        description = "Carrier display name to report (e.g. T-Mobile, Verizon).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val value = (name ?: "T-Mobile").trim()
        val patched = foldNoArgStringGetter(
            definingClass = "Landroid/telephony/TelephonyManager;",
            methodNames = setOf("getSimOperatorName", "getNetworkOperatorName"),
            value = value,
        )

        if (patched > 0) {
            logger.info("Spoofed $patched SIM operator-name read(s)")
        } else {
            logger.warning("No SIM operator-name reads found. No changes applied.")
        }
    }
}
