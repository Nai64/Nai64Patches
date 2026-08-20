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

        // Overwrite the receipt (p3) and transaction id (p4) parameters with a
        // fake receipt, so the C# side treats every product as already owned.
        // Writing to the parameter registers works at any register count,
        // unlike local registers v3/v4 which shift when extra locals exist.
        method.addInstructions(
            0,
            """
            const-string p3, "{\"Store\":\"GooglePlay\",\"TransactionID\":\"fake\"}"
            const-string p4, "fake"
            """.trimIndent(),
        )
        logger.info("Unity IAP products marked as owned")
    }
}