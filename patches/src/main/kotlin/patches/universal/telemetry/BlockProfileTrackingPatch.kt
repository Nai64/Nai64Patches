package patches.universal.telemetry

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private val profileStaticEntries = setOf(
    "identify", "setUserId", "setUserProperties", "setUserAttributes",
)

private val profileInstanceEntries = setOf(
    "identify", "set", "setUserId", "setUserProperties", "onUserLogin",
    "profilePush", "setProfile", "increment",
)

@Suppress("unused")
val blockProfileTrackingPatch = bytecodePatch(
    name = "Block Profile Tracking",
    description = "Stops user profile stitching in analytics SDKs whose events are blocked but identity calls are not",
    default = false,
) {
    category("Telemetry")
    val blockMixpanelPeople by booleanOption(
        title = "Block Mixpanel People",
        default = true,
        key = "blockMixpanelPeople",
        description = "Mixpanel user profile updates.",
    )
    val blockCleverTapProfiles by booleanOption(
        title = "Block CleverTap profiles",
        default = true,
        key = "blockCleverTapProfiles",
        description = "CleverTap login and profile pushes.",
    )
    val blockAmplitudeIdentity by booleanOption(
        title = "Block Amplitude identity",
        default = true,
        key = "blockAmplitudeIdentity",
        description = "Amplitude user ID and property setters.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val prefixes = buildMap {
            if (blockMixpanelPeople == true) put("Lcom/mixpanel/android/mpmetrics/MixpanelAPI\$People;", "Mixpanel People")
            if (blockCleverTapProfiles == true) put("Lcom/clevertap/android/sdk/CleverTapAPI;", "CleverTap Profiles")
            if (blockAmplitudeIdentity == true) put("Lcom/amplitude/api/Amplitude;", "Amplitude Identity")
        }
        if (prefixes.isEmpty()) {
            logger.warning("All vendors disabled. No changes applied.")
            return@execute
        }
        val hit = blockSdkMethods(prefixes, profileStaticEntries, profileInstanceEntries)
        if (hit.isEmpty()) logger.warning("No profile tracking calls found. No changes applied.")
        else hit.forEach { (sdk, count) -> logger.info("Blocked $sdk ($count method(s))") }
    }
}
