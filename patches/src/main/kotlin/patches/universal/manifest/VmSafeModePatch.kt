package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger

@Suppress("unused")
val vmSafeModePatch = resourcePatch(
    name = "VM Safe Mode",
    description =
        "Sets android:vmSafeMode so the app runs with AOT/JIT optimizations disabled, which " +
            "can stabilize apps that crash from compiler optimizations on some devices",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var applied = false
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull()
            if (application != null) {
                application.setAttributeNS(NS_ANDROID, "android:vmSafeMode", "true")
                applied = true
            }
        }

        if (applied) {
            logger.info("Enabled android:vmSafeMode")
        } else {
            logger.warning("No <application> element found. No changes applied.")
        }
    }
}
