package patches.universal.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.replaceArrayGetterWithEmpty
import patches.universal.misc.replaceGetterWithStaticCall

@Suppress("unused")
val hideEavesdroppingPatch = bytecodePatch(
    name = "Hide Eavesdropping",
    description = "Hides active notifications and microphones so apps cannot snoop on other apps or enumerate audio inputs",
    default = false,
) {
    category("Privacy")
    val hideNotifications by booleanOption(
        title = "Hide notifications",
        default = true,
        key = "hideNotifications",
        description = "Report no active notifications or channels.",
    )
    val hideMicrophones by booleanOption(
        title = "Hide microphones",
        default = true,
        key = "hideMicrophones",
        description = "Report no microphone devices.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        if (hideNotifications == true) {
            patched += replaceArrayGetterWithEmpty(
                "Landroid/app/NotificationManager;",
                setOf("getActiveNotifications"),
                "Landroid/service/notification/StatusBarNotification;",
            )
            patched += replaceGetterWithStaticCall(
                "Landroid/app/NotificationManager;",
                setOf("getNotificationChannels"),
                "Ljava/util/List;",
                "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
            )
        }
        if (hideMicrophones == true) {
            patched += replaceGetterWithStaticCall(
                "Landroid/media/AudioManager;",
                setOf("getMicrophones"),
                "Ljava/util/List;",
                "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
            )
        }
        if (patched > 0) logger.info("Hid eavesdropping vectors at $patched call site(s)")
        else logger.warning("No notification or microphone calls found. No changes applied.")
    }
}
