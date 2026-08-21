package patches.universal.telemetry

import app.morphe.patcher.patch.resourcePatch
import patches.universal.manifest.NS_ANDROID
import patches.universal.manifest.applicationOrNull
import java.util.logging.Logger
import org.w3c.dom.Element

/** Facebook SDK auto-init switches, keyed by meta-data name. */
private val facebookAutoInitSwitches = mapOf(
    "com.facebook.sdk.AutoInitEnabled" to "false",
    "com.facebook.sdk.AutoLogAppEventsEnabled" to "false",
    "com.facebook.sdk.AdvertiserIDCollectionEnabled" to "false",
)

@Suppress("unused")
val disableFacebookAutoInitPatch = resourcePatch(
    name = "Disable Facebook SDK Auto-Init",
    description =
        "Adds Facebook SDK manifest switches that stop auto-initialization, automatic app " +
            "event logging and advertiser ID collection at startup",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var added = 0
        var updated = 0
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
                ?: return@use

            for ((name, value) in facebookAutoInitSwitches) {
                val metadata = application.getElementsByTagName("meta-data")
                var target: Element? = null
                for (i in 0 until metadata.length) {
                    val meta = metadata.item(i) as? Element ?: continue
                    if (meta.getAttributeNS(NS_ANDROID, "name") == name) {
                        target = meta
                        break
                    }
                }

                if (target != null) {
                    if (target.getAttributeNS(NS_ANDROID, "value") != value) {
                        target.setAttributeNS(NS_ANDROID, "android:value", value)
                        updated++
                    }
                } else {
                    val meta = manifest.createElement("meta-data")
                    meta.setAttributeNS(NS_ANDROID, "android:name", name)
                    meta.setAttributeNS(NS_ANDROID, "android:value", value)
                    application.appendChild(meta)
                    added++
                }
            }
        }

        if (added > 0 || updated > 0) {
            logger.info("Facebook SDK auto-init: $added switch(es) added, $updated overridden")
        } else {
            logger.warning("No changes applied.")
        }
    }
}
