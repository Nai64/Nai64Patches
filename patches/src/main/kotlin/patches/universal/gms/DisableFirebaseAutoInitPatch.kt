package patches.universal.gms

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element
import patches.universal.manifest.NS_ANDROID
import patches.universal.manifest.applicationOrNull

@Suppress("unused")
val disableFirebaseAutoInitPatch = resourcePatch(
    name = "Disable Firebase Auto-Init",
    description = "Stops Firebase Analytics, Crashlytics, Performance, Messaging, In-App Messaging and ad-ID collection from auto-initializing, and removes the measurement provider, receiver and services. Standalone version of the Pairip Bypass Firebase cleanup.",
    default = false,
) {
    category("Google Services")
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val switches = mapOf(
            "firebase_analytics_collection_enabled" to "false",
            "firebase_crashlytics_collection_enabled" to "false",
            "firebase_performance_collection_enabled" to "false",
            "firebase_messaging_auto_init_enabled" to "false",
            "firebase_inapp_messaging_auto_data_collection_enabled" to "false",
            "google_analytics_adid_collection_enabled" to "false",
            "firebase_sessions_enabled" to "false",
        )
        var added = 0
        var updated = 0
        document("AndroidManifest.xml").use { manifest ->
            val application = manifest.documentElement.applicationOrNull() ?: return@use
            for ((name, value) in switches) {
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

        val measurementComponents = setOf(
            "com.google.firebase.provider.FirebaseInitProvider",
            "com.google.android.gms.measurement.AppMeasurementReceiver",
            "com.google.android.gms.measurement.AppMeasurementService",
            "com.google.android.gms.measurement.AppMeasurementJobService",
            "com.google.android.gms.measurement.AppMeasurementContentProvider",
        )
        var removed = 0
        document("AndroidManifest.xml").use { manifest ->
            for (tag in listOf("provider", "receiver", "service")) {
                val nodes = manifest.getElementsByTagName(tag)
                for (index in nodes.length - 1 downTo 0) {
                    val component = nodes.item(index) as? Element ?: continue
                    val name = component.getAttributeNS(NS_ANDROID, "name")
                        .takeIf { it.isNotEmpty() } ?: continue
                    if (name in measurementComponents || name.endsWith(".AppMeasurementReceiver") || name.endsWith(".AppMeasurementService")) {
                        component.parentNode?.removeChild(component)
                        removed++
                    }
                }
            }
        }

        if (added == 0 && updated == 0 && removed == 0) {
            logger.warning("Disable Firebase Auto-Init: no Firebase components found. No changes applied.")
        } else {
            logger.info("Disable Firebase Auto-Init: $added switch(es) added, $updated overridden, $removed component(s) removed")
        }
    }
}
