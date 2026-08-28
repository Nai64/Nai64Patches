package patches.universal.unlock

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val freeInAppPurchasesPatch = bytecodePatch(
    name = "Free In-app Purchases (Experimental)",
    description = "Makes Unity in-app purchases appear successful without paying. Supports Unity IAP and Google Play Billing. Use for offline games only — online verification may still block.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0

        // Single pass over all classes to reduce heap and time (was 6 separate scans)
        classDefForEach { classDef ->
            val mutableClass = try { mutableClassDefBy(classDef) } catch (_: Exception) { return@classDefForEach }
            for (m in mutableClass.methods) {
                val impl = m.implementation ?: continue
                val name = m.name
                val ret = m.returnType
                val type = mutableClass.type

                // Strategy 1: Unity ProcessPurchase -> Complete
                if (name == "ProcessPurchase" && ret.contains("PurchaseProcessingResult")) {
                    if (impl.registerCount < 1) continue
                    try {
                        m.addInstructions(0, """
                            sget-object v0, Lcom/unity/purchasing/PurchaseProcessingResult;->Complete:Lcom/unity/purchasing/PurchaseProcessingResult;
                            return-object v0
                        """.trimIndent())
                        patched++
                    } catch (_: Exception) {}
                    continue
                }

                // Strategy 2: Billing launchBillingFlow -> OK (lightweight const null fallback to save heap)
                if (name == "launchBillingFlow" && ret.contains("BillingResult")) {
                    try {
                        // Lightweight: return null BillingResult with OK code via const null + builder would be heavy; use simple const null return for speed
                        // Many games only check responseCode == 0, but returning null may NPE. Try builder first, fallback to null.
                        try {
                            m.addInstructions(0, """
                                invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v0
                                const/4 v1, 0x0
                                invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v0
                                invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                                move-result-object v0
                                return-object v0
                            """.trimIndent())
                        } catch (_: Exception) {
                            m.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
                        }
                        patched++
                    } catch (_: Exception) {}
                    continue
                }

                // Strategy 3: isReady -> true
                if (name == "isReady" && ret == "Z" && type.contains("BillingClient")) {
                    try {
                        m.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                        patched++
                    } catch (_: Exception) {}
                    continue
                }

                // Strategy 4: onPurchasesUpdated -> grant
                if (name == "onPurchasesUpdated") {
                    try {
                        m.addInstructions(0, """
                            invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                            move-result-object v0
                            const/4 v1, 0x0
                            invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                            move-result-object v0
                            invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                            move-result-object v1
                            invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                            move-result-object v2
                            invoke-interface {p0, v1, v2}, Lcom/android/billingclient/api/PurchasesUpdatedListener;->onPurchasesUpdated(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V
                            return-void
                        """.trimIndent())
                        patched++
                    } catch (_: Exception) {}
                    continue
                }

                // Strategy 5: AIDL getBuyIntent -> OK bundle
                if (name == "getBuyIntent" && ret == "Landroid/os/Bundle;") {
                    if (impl.registerCount < 2) continue
                    try {
                        m.addInstructions(0, """
                            new-instance v0, Landroid/os/Bundle;
                            invoke-direct {v0}, Landroid/os/Bundle;-><init>()V
                            const-string v1, "BUY_INTENT"
                            const/4 v2, 0x0
                            invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V
                            return-object v0
                        """.trimIndent())
                        patched++
                    } catch (_: Exception) {}
                    continue
                }

                // Strategy 6: price -> 0.00
                if ((name == "getPrice" || name == "getOriginalPrice") && ret == "Ljava/lang/String;" && m.parameterTypes.isEmpty() && (type.contains("SkuDetails") || type.contains("ProductDetails"))) {
                    try {
                        m.addInstructions(0, """
                            const-string v0, "0.00"
                            return-object v0
                        """.trimIndent())
                        patched++
                    } catch (_: Exception) {}
                    continue
                }

                // Strategy 7: Xsolla reflector
                if (type.contains("Xsolla") && name.contains("launchBillingFlow")) {
                    try {
                        m.addInstructions(0, """
                            invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                            move-result-object v0
                            const/4 v1, 0x0
                            invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                            move-result-object v0
                            invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                            move-result-object v0
                            return-object v0
                        """.trimIndent())
                        patched++
                    } catch (_: Exception) {}
                    continue
                }

                // Strategy 8: verifySignature etc. -> true
                if ((name == "verifySignature" || name == "verifyPurchase" || name == "isValidSignature" || name == "validateReceipt" || name.lowercase().contains("verifysignature") || name.lowercase().contains("verifypurchase")) && ret == "Z") {
                    try {
                        m.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                        patched++
                    } catch (_: Exception) {}
                    continue
                }
                if (type.contains("Security") && name.lowercase().contains("verify") && ret == "Z") {
                    try {
                        m.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                        patched++
                    } catch (_: Exception) {}
                    continue
                }

                // Strategy 9: hasReceipt / isAvailable -> true, GetCurrentGem etc. handled by UnlimitedCurrencies
                if ((name == "hasReceipt" || name == "getHasReceipt" || name == "isAvailable") && ret == "Z" && (type.contains("Product") || type.contains("Purchasing"))) {
                    try {
                        m.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                        patched++
                    } catch (_: Exception) {}
                    continue
                }
            }
        }

        if (patched > 0) {
            logger.info("Free In-app Purchases: patched $patched purchase check(s)")
        } else {
            logger.warning("No Unity IAP / Billing purchase checks found. No changes applied.")
        }
    }
}
