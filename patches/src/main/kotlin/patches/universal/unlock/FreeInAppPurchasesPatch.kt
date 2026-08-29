package patches.universal.unlock

import app.morphe.patcher.Fingerprint
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

        // Strategy 1: Unity ProcessPurchase -> Complete
        val processPurchaseFp = object : Fingerprint(name = "ProcessPurchase") {}
        val processPurchaseMethod = processPurchaseFp.methodOrNull
        if (processPurchaseMethod != null && processPurchaseMethod.returnType.contains("PurchaseProcessingResult") && processPurchaseMethod.implementation != null) {
            try {
                processPurchaseMethod.addInstructions(0, """
                    sget-object v0, Lcom/unity/purchasing/PurchaseProcessingResult;->Complete:Lcom/unity/purchasing/PurchaseProcessingResult;
                    return-object v0
                """.trimIndent())
                patched++
            } catch (_: Exception) {}
        }

        // Strategy 2: Billing launchBillingFlow -> OK
        val launchBillingFp = object : Fingerprint(
            name = "launchBillingFlow",
            custom = { method, _ -> method.returnType.contains("BillingResult") },
        ) {}
        val launchBillingMethod = launchBillingFp.methodOrNull
        if (launchBillingMethod != null && launchBillingMethod.implementation != null) {
            try {
                try {
                    launchBillingMethod.addInstructions(0, """
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
                    launchBillingMethod.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
                }
                patched++
            } catch (_: Exception) {}
        }

        // Strategy 3: isReady -> true (in BillingClient)
        val isReadyFp = object : Fingerprint(
            name = "isReady",
            returnType = "Z",
            custom = { _, classDef -> classDef.type.contains("BillingClient") },
        ) {}
        val isReadyMethod = isReadyFp.methodOrNull
        if (isReadyMethod != null && isReadyMethod.implementation != null) {
            try {
                isReadyMethod.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                patched++
            } catch (_: Exception) {}
        }

        // Strategy 4: onPurchasesUpdated -> grant
        val onPurchasesUpdatedFp = object : Fingerprint(name = "onPurchasesUpdated") {}
        val onPurchasesUpdatedMethod = onPurchasesUpdatedFp.methodOrNull
        if (onPurchasesUpdatedMethod != null && onPurchasesUpdatedMethod.implementation != null) {
            try {
                onPurchasesUpdatedMethod.addInstructions(0, """
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
        }

        // Strategy 5: getBuyIntent -> OK bundle (AIDL v5/v7 billing)
        val getBuyIntentFp = object : Fingerprint(
            name = "getBuyIntent",
            returnType = "Landroid/os/Bundle;",
        ) {}
        val getBuyIntentMethod = getBuyIntentFp.methodOrNull
        if (getBuyIntentMethod != null && getBuyIntentMethod.implementation != null && getBuyIntentMethod.parameterTypes.size >= 2) {
            try {
                getBuyIntentMethod.addInstructions(0, """
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

        // Strategy 6: price -> "0.00" (SkuDetails / ProductDetails)
        val getPriceFp = object : Fingerprint(
            name = "getPrice",
            returnType = "Ljava/lang/String;",
            custom = { _, classDef ->
                val t = classDef.type.lowercase()
                t.contains("skudetails") || t.contains("productdetails")
            },
        ) {}
        val getPriceMethod = getPriceFp.methodOrNull
        if (getPriceMethod != null && getPriceMethod.parameterTypes.isEmpty() && getPriceMethod.implementation != null) {
            try {
                getPriceMethod.addInstructions(0, "const-string v0, \"0.00\"\nreturn-object v0")
                patched++
            } catch (_: Exception) {}
        }

        val getOriginalPriceFp = object : Fingerprint(
            name = "getOriginalPrice",
            returnType = "Ljava/lang/String;",
            custom = { _, classDef ->
                val t = classDef.type.lowercase()
                t.contains("skudetails") || t.contains("productdetails")
            },
        ) {}
        val getOriginalPriceMethod = getOriginalPriceFp.methodOrNull
        if (getOriginalPriceMethod != null && getOriginalPriceMethod.parameterTypes.isEmpty() && getOriginalPriceMethod.implementation != null) {
            try {
                getOriginalPriceMethod.addInstructions(0, "const-string v0, \"0.00\"\nreturn-object v0")
                patched++
            } catch (_: Exception) {}
        }

        // Strategy 7: Xsolla launchBillingFlow -> OK
        val xsollaLaunchFp = object : Fingerprint(
            name = "launchBillingFlow",
            custom = { _, classDef -> classDef.type.lowercase().contains("xsolla") },
        ) {}
        val xsollaLaunchMethod = xsollaLaunchFp.methodOrNull
        if (xsollaLaunchMethod != null && xsollaLaunchMethod.implementation != null) {
            try {
                xsollaLaunchMethod.addInstructions(0, """
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

        // Strategy 8: verifySignature / verifyPurchase / validateReceipt -> true
        for (verifyName in listOf("verifySignature", "verifyPurchase", "isValidSignature", "validateReceipt")) {
            val fp = object : Fingerprint(
                name = verifyName,
                returnType = "Z",
            ) {}
            val method = fp.methodOrNull
            if (method != null && method.implementation != null) {
                try {
                    method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                    patched++
                } catch (_: Exception) {}
            }
        }
        // Also check Security.verify methods
        val securityVerifyFp = object : Fingerprint(
            returnType = "Z",
            custom = { method, classDef ->
                classDef.type.contains("Security") && method.name.lowercase().contains("verify")
            },
        ) {}
        val securityVerifyMethod = securityVerifyFp.methodOrNull
        if (securityVerifyMethod != null && securityVerifyMethod.implementation != null) {
            try {
                securityVerifyMethod.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                patched++
            } catch (_: Exception) {}
        }

        // Strategy 9: hasReceipt / isAvailable -> true
        for (receiptName in listOf("hasReceipt", "getHasReceipt", "isAvailable")) {
            val fp = object : Fingerprint(
                name = receiptName,
                returnType = "Z",
                custom = { _, classDef ->
                    val t = classDef.type.lowercase()
                    t.contains("product") || t.contains("purchasing")
                },
            ) {}
            val method = fp.methodOrNull
            if (method != null && method.implementation != null) {
                try {
                    method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                    patched++
                } catch (_: Exception) {}
            }
        }

        // Strategy 10: SharedPreferences check for receipt -> true
        // (Some games store receipt status in SharedPreferences)
        val hasReceiptSharedPrefsFp = object : Fingerprint(
            strings = listOf("hasReceipt", "has_receipt", "purchased"),
            custom = { method, _ ->
                method.returnType == "Z" && method.parameterTypes.isEmpty()
            },
        ) {}
        val hasReceiptPrefsMethod = hasReceiptSharedPrefsFp.methodOrNull
        if (hasReceiptPrefsMethod != null && hasReceiptPrefsMethod.implementation != null) {
            try {
                hasReceiptPrefsMethod.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                patched++
            } catch (_: Exception) {}
        }

        if (patched > 0) {
            logger.info("Free In-app Purchases: patched $patched purchase check(s)")
        } else {
            logger.warning("No Unity IAP / Billing purchase checks found. No changes applied.")
        }
    }
}
