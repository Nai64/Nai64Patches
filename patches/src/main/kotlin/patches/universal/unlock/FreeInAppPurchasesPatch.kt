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

        // Strategy 1: Unity IAP IStoreListener.ProcessPurchase -> Complete
        run {
            classDefForEach { classDef ->
                val mutableClass = mutableClassDefBy(classDef)
                for (m in mutableClass.methods) {
                    if (m.name != "ProcessPurchase") continue
                    if (!m.returnType.contains("PurchaseProcessingResult")) continue
                    if (m.implementation == null) continue
                    if (m.implementation!!.registerCount < 1) continue
                    try {
                        m.addInstructions(0, """
                            sget-object v0, Lcom/unity/purchasing/PurchaseProcessingResult;->Complete:Lcom/unity/purchasing/PurchaseProcessingResult;
                            return-object v0
                        """.trimIndent())
                        logger.info("Free IAP: patched ${mutableClass.type}->${m.name}")
                        patched++
                    } catch (_: Exception) {}
                }
            }
        }

        // Strategy 2: Google Play Billing launchBillingFlow -> OK
        run {
            classDefForEach { classDef ->
                val mutableClass = mutableClassDefBy(classDef)
                for (m in mutableClass.methods) {
                    if (m.name != "launchBillingFlow") continue
                    if (!m.returnType.contains("BillingResult")) continue
                    if (m.implementation == null) continue
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
                        logger.info("Free IAP: patched ${mutableClass.type}->launchBillingFlow")
                        patched++
                    } catch (_: Exception) {
                        try {
                            m.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
                            patched++
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        // Strategy 3: Make BillingClient appear ready
        run {
            classDefForEach { classDef ->
                if (!classDef.type.contains("BillingClient")) return@classDefForEach
                val mutableClass = mutableClassDefBy(classDef)
                for (m in mutableClass.methods) {
                    if (m.name != "isReady" || m.returnType != "Z") continue
                    if (m.implementation == null) continue
                    try {
                        m.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                        patched++
                    } catch (_: Exception) {}
                }
            }
        }

        // Strategy 4: Spoof PurchasesUpdatedListener to immediately grant
        run {
            classDefForEach { classDef ->
                val mutableClass = mutableClassDefBy(classDef)
                for (m in mutableClass.methods) {
                    if (m.name != "onPurchasesUpdated") continue
                    if (m.implementation == null) continue
                    // onPurchasesUpdated has (BillingResult, List) params, void return
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
                        logger.info("Free IAP: patched ${mutableClass.type}->onPurchasesUpdated")
                        patched++
                    } catch (_: Exception) {}
                }
            }
        }

        // Strategy 5: Legacy AIDL getBuyIntent -> return OK bundle
        run {
            classDefForEach { classDef ->
                val mutableClass = mutableClassDefBy(classDef)
                for (m in mutableClass.methods) {
                    if (m.name != "getBuyIntent") continue
                    if (m.returnType != "Landroid/os/Bundle;") continue
                    if (m.implementation == null) continue
                    if (m.implementation!!.registerCount < 2) continue
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
                }
            }
        }

        // Strategy 6: Generic price spoof — make SkuDetails.getPrice return "0.00"
        run {
            classDefForEach { classDef ->
                val mutableClass = mutableClassDefBy(classDef)
                for (m in mutableClass.methods) {
                    if (m.name != "getPrice" && m.name != "getOriginalPrice") continue
                    if (m.returnType != "Ljava/lang/String;") continue
                    if (m.parameterTypes.isNotEmpty()) continue
                    if (!mutableClass.type.contains("SkuDetails") && !mutableClass.type.contains("ProductDetails")) continue
                    if (m.implementation == null) continue
                    try {
                        m.addInstructions(0, """
                            const-string v0, "0.00"
                            return-object v0
                        """.trimIndent())
                        patched++
                    } catch (_: Exception) {}
                }
            }
        }

        // Strategy 7: Xsolla BillingClient reflector
        run {
            classDefForEach { classDef ->
                if (!classDef.type.contains("Xsolla") && !classDef.type.lowercase().contains("xsolla")) return@classDefForEach
                val mutableClass = mutableClassDefBy(classDef)
                for (m in mutableClass.methods) {
                    if (!m.name.contains("launchBillingFlow")) continue
                    if (m.implementation == null) continue
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
                }
            }
        }

        // Strategy 8: Bypass receipt verification (server and local)
        run {
            classDefForEach { classDef ->
                val mutableClass = mutableClassDefBy(classDef)
                for (m in mutableClass.methods) {
                    val n = m.name
                    if (n != "verifySignature" && n != "verifyPurchase" && n != "isValidSignature" && n != "validateReceipt" && n != "isValid" && !n.contains("verifyReceipt") && !n.contains("verifySignature")) continue
                    if (m.returnType != "Z") continue
                    if (m.implementation == null) continue
                    try {
                        m.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                        patched++
                    } catch (_: Exception) {}
                }
            }
            // Security class helper
            classDefForEach { classDef ->
                if (!classDef.type.contains("Security")) return@classDefForEach
                val mutableClass = mutableClassDefBy(classDef)
                for (m in mutableClass.methods) {
                    if (!m.name.lowercase().contains("verify")) continue
                    if (m.returnType != "Z") continue
                    if (m.implementation == null) continue
                    try {
                        m.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                        patched++
                    } catch (_: Exception) {}
                }
            }
        }

        // Strategy 9: Unity Product hasReceipt / isAvailable and PlayerPrefs
        run {
            classDefForEach { classDef ->
                if (!classDef.type.contains("Product") && !classDef.type.contains("Purchasing") && !classDef.type.contains("PlayerPrefs")) return@classDefForEach
                val mutableClass = mutableClassDefBy(classDef)
                for (m in mutableClass.methods) {
                    if (m.name == "hasReceipt" || m.name == "getHasReceipt" || m.name == "isAvailable" || m.name == "getAvailable") {
                        if (m.returnType != "Z") continue
                        if (m.implementation == null) continue
                        try {
                            m.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                            patched++
                        } catch (_: Exception) {}
                    }
                    if (m.name == "GetInt" || m.name == "getInt") {
                        // PlayerPrefs / SharedPreferences getInt for currency - hard to know key, but make it return large value for any int
                        // Only patch if class is PlayerPrefs
                        if (!classDef.type.contains("PlayerPrefs")) continue
                        if (m.returnType != "I") continue
                        // Do not blanket patch all getInt, only if method has 2 params (key, default)
                        if (m.parameterTypes.size != 2) continue
                        try {
                            m.addInstructions(0, "const v0, 0xF423F\nreturn v0")
                            patched++
                        } catch (_: Exception) {}
                    }
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
