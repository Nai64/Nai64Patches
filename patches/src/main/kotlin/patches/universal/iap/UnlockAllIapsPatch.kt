package patches.universal.iap

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object UnityIapProductDescriptionConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/unity/purchasing/common/ProductDescription;",
    name = "<init>",
    returnType = "V",
    parameters = listOf(
        "Ljava/lang/String;",
        "Lcom/unity/purchasing/common/ProductMetadata;",
        "Ljava/lang/String;",
        "Ljava/lang/String;",
    ),
)

@Suppress("unused")
val unlockAllIapsPatch = bytecodePatch(
    name = "Unlock All IAPs (Unity IAP)",
    description = "Unlock all in-app purchases in Unity IAP games (Experimental)",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val method = UnityIapProductDescriptionConstructorFingerprint.methodOrNull
        if (method == null || method.implementation == null) {
            logger.warning("Unity IAP ProductDescription not found. No changes applied.")
            return@execute
        }

        if (method.implementation!!.registerCount < 5) {
            logger.warning("Unlock All IAPs: not enough registers")
            return@execute
        }

        // Overwrite the receipt (v3) and transaction id (v4) parameters with a
        // fake receipt, so the C# side treats every product as already owned.
        method.addInstructions(
            0,
            """
            const-string v3, "{\"Store\":\"GooglePlay\",\"TransactionID\":\"fake\"}"
            const-string v4, "fake"
            """.trimIndent(),
        )
        logger.info("Unity IAP products marked as owned")
    }
}