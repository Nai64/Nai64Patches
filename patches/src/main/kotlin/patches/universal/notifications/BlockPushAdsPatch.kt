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
