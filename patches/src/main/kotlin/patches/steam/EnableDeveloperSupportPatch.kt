package patches.steam

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldBooleanReturns

@Suppress("unused")
val enableDeveloperSupportPatch = bytecodePatch(
    name = "Enable Developer Support",
    description = "Steam: forces React Native getUseDeveloperSupport to true to unlock dev menu (reload, inspector).",
    default = false,
) {
    compatibleWith("com.valvesoftware.android.steam.community")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldBooleanReturns(
            mapOf(
                "Lcom/valvesoftware/android/steam/community/MainApplication\$reactNativeHost\$1;" to mapOf(
                    "getUseDeveloperSupport" to "0x1",
                ),
            ),
        )
        if (patched > 0) logger.info("Enabled $patched developer-support check(s)")
        else logger.warning("No developer-support checks found. No changes applied.")
    }
}
