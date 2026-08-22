package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger

@Suppress("unused")
val spoofSimCountryPatch = bytecodePatch(
    name = "Spoof SIM Country",
    description =
        "Reports a chosen ISO country (e.g. us) from TelephonyManager.getSimCountryIso() " +
            "and getNetworkCountryIso() to bypass region locks and geo-restrictions.",
    default = false,
) {
    val country by stringOption(
        title = "Country ISO",
        default = "us",
        key = "spoofSimCountryIso",
        description = "Two-letter ISO 3166-1 country code to report (e.g. us, gb, jp).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val value = (country ?: "us").trim().lowercase()
        val patched = foldNoArgStringGetter(
            definingClass = "Landroid/telephony/TelephonyManager;",
            methodNames = setOf("getSimCountryIso", "getNetworkCountryIso"),
            value = value,
        )

        if (patched > 0) {
            logger.info("Spoofed $patched SIM country read(s)")
        } else {
            logger.warning("No SIM country reads found. No changes applied.")
        }
    }
}
