package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofSimOperatorPatch = bytecodePatch(
    name = "Spoof SIM Operator",
    description =
        "Reports a chosen MCC+MNC from TelephonyManager.getSimOperator() and " +
            "getNetworkOperator() so apps cannot read the real mobile network operator.",
    default = false,
) {
    val mccMnc by stringOption(
        title = "MCC+MNC",
        default = "310260",
        key = "spoofSimOperatorMccMnc",
        description = "Mobile Country Code + Mobile Network Code to report (e.g. 310260).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val value = (mccMnc ?: "310260").trim()
        val patched = foldNoArgStringGetter(
            definingClass = "Landroid/telephony/TelephonyManager;",
            methodNames = setOf("getSimOperator", "getNetworkOperator"),
            value = value,
        )

        if (patched > 0) {
            logger.info("Spoofed $patched SIM operator read(s)")
        } else {
            logger.warning("No SIM operator reads found. No changes applied.")
        }
    }
}
