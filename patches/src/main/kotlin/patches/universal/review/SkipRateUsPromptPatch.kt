package patches.universal.review

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object ReviewManagerRequestReviewFlowFingerprint : Fingerprint(
    name = "requestReviewFlow",
    returnType = "Lcom/google/android/play/core/tasks/Task;",
    custom = { method, classDef ->
        classDef.interfaces.contains("Lcom/google/android/play/core/review/ReviewManager;")
    },
)

internal object ReviewManagerLaunchReviewFlowFingerprint : Fingerprint(
    name = "launchReviewFlow",
    returnType = "Lcom/google/android/play/core/tasks/Task;",
    custom = { method, classDef ->
        classDef.interfaces.contains("Lcom/google/android/play/core/review/ReviewManager;")
    },
)

@Suppress("unused")
val skipRateUsPromptPatch = bytecodePatch(
    name = "Skip Rate-Us Prompt",
    description = "Skip the in-app review (rate us) prompt",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        var patched = 0

        val request = ReviewManagerRequestReviewFlowFingerprint.methodOrNull
        if (request != null && request.implementation != null && request.implementation!!.registerCount >= 1) {
            request.addInstructions(
                0,
                """
                const/4 v0, 0x0
                return-object v0
                """.trimIndent(),
            )
            patched++
        }

        val launch = ReviewManagerLaunchReviewFlowFingerprint.methodOrNull
        if (launch != null && launch.implementation != null && launch.implementation!!.registerCount >= 1) {
            launch.addInstructions(
                0,
                """
                const/4 v0, 0x0
                return-object v0
                """.trimIndent(),
            )
            patched++
        }

        if (patched == 0) {
            logger.warning("Play Core review manager not found. No changes applied.")
        } else {
            logger.info("Skipped rate-us prompt ($patched method(s))")
        }
    }
}