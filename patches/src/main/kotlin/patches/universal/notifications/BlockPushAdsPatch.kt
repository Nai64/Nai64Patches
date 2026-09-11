package patches.universal.notifications

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object FirebaseMessagingServiceOnMessageReceivedFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/messaging/FirebaseMessagingService;",
    name = "Block Push Ads",
    returnType = "V",
    parameters = listOf("Lcom/google/firebase/messaging/RemoteMessage;"),
)

@Suppress("unused")
val blockPushAdsPatch = bytecodePatch(
    name = "Block Push Ads",
    description = "Block push notification ads (Firebase Cloud Messaging). Also blocks legitimate notifications.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Ads") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val method = FirebaseMessagingServiceOnMessageReceivedFingerprint.methodOrNull
        if (method == null || method.implementation == null) {
            logger.warning("FirebaseMessagingService not found. No changes applied.")
            return@execute
        }

        method.addInstruction(0, "return-void")
        logger.info("Push notifications blocked")
    }
}
