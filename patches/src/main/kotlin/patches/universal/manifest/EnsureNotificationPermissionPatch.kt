package patches.universal.manifest

import app.morphe.patcher.patch.resourcePatch
import java.util.logging.Logger
import org.w3c.dom.Element

private const val POST_NOTIFICATIONS = "android.permission.POST_NOTIFICATIONS"

@Suppress("unused")
val ensureNotificationPermissionPatch = resourcePatch(
    name = "Ensure Notification Permission",
    description =
        "Adds POST_NOTIFICATIONS to the manifest so older apps can request notification access " +
            "on Android 13+. This only declares the permission; Android may still require a " +
            "runtime approval. Unlike Spoof Notifications as Enabled, it does not change the " +
            "app's NotificationManager.areNotificationsEnabled check.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var added = false

        document("AndroidManifest.xml").use { manifest ->
            val root = manifest.documentElement ?: return@use
            val permissions = manifest.getElementsByTagName("uses-permission")
            for (i in 0 until permissions.length) {
                val permission = permissions.item(i) as? Element ?: continue
                if (permission.getAttributeNS(NS_ANDROID, "name") == POST_NOTIFICATIONS) return@use
            }

            val permission = manifest.createElement("uses-permission")
            permission.setAttributeNS(NS_ANDROID, "android:name", POST_NOTIFICATIONS)
            root.appendChild(permission)
            added = true
        }

        if (added) logger.info("Added POST_NOTIFICATIONS permission")
        else logger.info("POST_NOTIFICATIONS already declared. No changes applied.")
    }
}
