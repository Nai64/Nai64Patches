package patches.universal.unlock

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val freeInAppPurchasesPatch = bytecodePatch(
    name = "Free In-app Purchases (Experimental)",
    description = "Spoofs in-app purchases to appear successful. Covers Google Play Billing, Unity IAP, Xsolla, and common receipt verification. Server-side verification may still block in online games.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        val patchedMethods = mutableSetOf<String>()

        // ──────────────────────────────────────────────
        // GOOGLE PLAY BILLING
        // ──────────────────────────────────────────────

        // launchBillingFlow -> return OK BillingResult
        val launchBillingFp = object : Fingerprint(
            name = "launchBillingFlow",
            custom = { method, _ -> method.returnType.contains("BillingResult") },
        ) {}
        val launchBillingMethod = launchBillingFp.methodOrNull
        if (launchBillingMethod?.implementation != null) {
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
                patchedMethods.add("launchBillingFlow")
                patched++
            } catch (_: Exception) {}
        }

        // isReady -> true (BillingClient)
        val isReadyFp = object : Fingerprint(
            name = "isReady",
            returnType = "Z",
            custom = { _, classDef -> classDef.type.contains("BillingClient") },
        ) {}
        val isReadyMethod = isReadyFp.methodOrNull
        if (isReadyMethod?.implementation != null) {
            try {
                isReadyMethod.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                patchedMethods.add("BillingClient.isReady")
                patched++
            } catch (_: Exception) {}
        }

        // endConnection -> no-op (prevent billing client teardown)
        val endConnectionFp = object : Fingerprint(
            name = "endConnection",
            custom = { _, classDef -> classDef.type.contains("BillingClient") },
        ) {}
        val endConnectionMethod = endConnectionFp.methodOrNull
        if (endConnectionMethod?.implementation != null) {
            try {
                endConnectionMethod.addInstructions(0, "return-void")
                patchedMethods.add("BillingClient.endConnection")
                patched++
            } catch (_: Exception) {}
        }

        // onPurchasesUpdated -> fire with OK result + empty list
        val onPurchasesUpdatedFp = object : Fingerprint(name = "onPurchasesUpdated") {}
        val onPurchasesUpdatedMethod = onPurchasesUpdatedFp.methodOrNull
        if (onPurchasesUpdatedMethod?.implementation != null) {
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
                patchedMethods.add("onPurchasesUpdated")
                patched++
            } catch (_: Exception) {}
        }

        // getBuyIntent -> OK bundle (legacy AIDL billing v5/v7)
        val getBuyIntentFp = object : Fingerprint(
            name = "getBuyIntent",
            returnType = "Landroid/os/Bundle;",
        ) {}
        val getBuyIntentMethod = getBuyIntentFp.methodOrNull
        if (getBuyIntentMethod?.implementation != null && getBuyIntentMethod.parameterTypes.size >= 2) {
            try {
                getBuyIntentMethod.addInstructions(0, """
                    new-instance v0, Landroid/os/Bundle;
                    invoke-direct {v0}, Landroid/os/Bundle;-><init>()V
                    const-string v1, "BUY_INTENT"
                    const/4 v2, 0x0
                    invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V
                    return-object v0
                """.trimIndent())
                patchedMethods.add("getBuyIntent")
                patched++
            } catch (_: Exception) {}
        }

        // getPurchases / queryPurchases -> return list with fake purchase
        for (queryName in listOf("getPurchases", "queryPurchases", "queryPurchasesAsync")) {
            val queryFp = object : Fingerprint(
                name = queryName,
                custom = { method, classDef ->
                    classDef.type.contains("BillingClient") && method.parameterTypes.size <= 2
                },
            ) {}
            val queryMethod = queryFp.methodOrNull
            if (queryMethod?.implementation != null) {
                try {
                    when {
                        queryMethod.returnType.contains("List") -> {
                            queryMethod.addInstructions(0, """
                                invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                                move-result-object v0
                                return-object v0
                            """.trimIndent())
                        }
                        queryMethod.returnType == "Landroid/os/Bundle;" -> {
                            queryMethod.addInstructions(0, """
                                new-instance v0, Landroid/os/Bundle;
                                invoke-direct {v0}, Landroid/os/Bundle;-><init>()V
                                const-string v1, "RESPONSE_CODE"
                                const/4 v2, 0x0
                                invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V
                                const-string v1, "INAPP_PURCHASE_DATA_LIST"
                                new-instance v2, Ljava/util/ArrayList;
                                invoke-direct {v2}, Ljava/util/ArrayList;-><init>()V
                                invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putStringArrayList(Ljava/lang/String;Ljava/util/ArrayList;)V
                                return-object v0
                            """.trimIndent())
                        }
                    }
                    patchedMethods.add(queryName)
                    patched++
                } catch (_: Exception) {}
            }
        }

        // querySkuDetailsAsync / queryProductDetailsAsync -> return empty (no products to show)
        for (queryName in listOf("querySkuDetailsAsync", "queryProductDetailsAsync", "querySkuDetails")) {
            val queryFp = object : Fingerprint(name = queryName) {}
            val queryMethod = queryFp.methodOrNull
            if (queryMethod?.implementation != null) {
                try {
                    queryMethod.addInstructions(0, "return-void")
                    patchedMethods.add(queryName)
                    patched++
                } catch (_: Exception) {}
            }
        }

        // consumePurchase / consumeAsync -> always succeed
        for (consumeName in listOf("consumePurchase", "consumeAsync", "consumePurchaseAsync")) {
            val consumeFp = object : Fingerprint(name = consumeName) {}
            val consumeMethod = consumeFp.methodOrNull
            if (consumeMethod?.implementation != null) {
                try {
                    when {
                        consumeMethod.returnType.contains("BillingResult") -> {
                            consumeMethod.addInstructions(0, """
                                invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v0
                                const/4 v1, 0x0
                                invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                                move-result-object v0
                                invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                                move-result-object v0
                                return-object v0
                            """.trimIndent())
                        }
                        consumeMethod.returnType == "Landroid/os/Bundle;" -> {
                            consumeMethod.addInstructions(0, """
                                new-instance v0, Landroid/os/Bundle;
                                invoke-direct {v0}, Landroid/os/Bundle;-><init>()V
                                const-string v1, "RESPONSE_CODE"
                                const/4 v2, 0x0
                                invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V
                                return-object v0
                            """.trimIndent())
                        }
                    }
                    patchedMethods.add(consumeName)
                    patched++
                } catch (_: Exception) {}
            }
        }

        // ackPurchase -> always succeed
        val ackPurchaseFp = object : Fingerprint(name = "acknowledgePurchase") {}
        val ackPurchaseMethod = ackPurchaseFp.methodOrNull
        if (ackPurchaseMethod?.implementation != null) {
            try {
                ackPurchaseMethod.addInstructions(0, """
                    invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                    move-result-object v0
                    const/4 v1, 0x0
                    invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                    move-result-object v0
                    invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                    move-result-object v0
                    return-object v0
                """.trimIndent())
                patchedMethods.add("acknowledgePurchase")
                patched++
            } catch (_: Exception) {}
        }

        // getPrice / getOriginalPrice / getOriginalPriceAmountMicros -> "0.00" / 0
        for (priceMethod in listOf("getPrice", "getOriginalPrice")) {
            val priceFp = object : Fingerprint(
                name = priceMethod,
                returnType = "Ljava/lang/String;",
                custom = { _, classDef ->
                    val t = classDef.type.lowercase()
                    t.contains("skudetails") || t.contains("productdetails") || t.contains("sku") || t.contains("product")
                },
            ) {}
            val method = priceFp.methodOrNull
            if (method?.implementation != null && method.parameterTypes.isEmpty()) {
                try {
                    method.addInstructions(0, "const-string v0, \"0.00\"\nreturn-object v0")
                    patchedMethods.add(priceMethod)
                    patched++
                } catch (_: Exception) {}
            }
        }

        val getPriceMicrosFp = object : Fingerprint(
            name = "getPriceAmountMicros",
            returnType = "J",
        ) {}
        val getPriceMicrosMethod = getPriceMicrosFp.methodOrNull
        if (getPriceMicrosMethod?.implementation != null) {
            try {
                getPriceMicrosMethod.addInstructions(0, "const-wide/16 v0, 0x0\nreturn-wide v0")
                patchedMethods.add("getPriceAmountMicros")
                patched++
            } catch (_: Exception) {}
        }

        // ──────────────────────────────────────────────
        // UNITY IAP
        // ──────────────────────────────────────────────

        // ProcessPurchase -> Complete
        val processPurchaseFp = object : Fingerprint(name = "ProcessPurchase") {}
        val processPurchaseMethod = processPurchaseFp.methodOrNull
        if (processPurchaseMethod?.implementation != null && processPurchaseMethod.returnType.contains("PurchaseProcessingResult")) {
            try {
                processPurchaseMethod.addInstructions(0, """
                    sget-object v0, Lcom/unity/purchasing/PurchaseProcessingResult;->Complete:Lcom/unity/purchasing/PurchaseProcessingResult;
                    return-object v0
                """.trimIndent())
                patchedMethods.add("ProcessPurchase")
                patched++
            } catch (_: Exception) {}
        }

        // OnPurchaseFailed -> no-op (prevent failure callback)
        val onPurchaseFailedFp = object : Fingerprint(name = "OnPurchaseFailed") {}
        val onPurchaseFailedMethod = onPurchaseFailedFp.methodOrNull
        if (onPurchaseFailedMethod?.implementation != null) {
            try {
                onPurchaseFailedMethod.addInstructions(0, "return-void")
                patchedMethods.add("OnPurchaseFailed")
                patched++
            } catch (_: Exception) {}
        }

        // IStoreListener.OnPurchaseComplete -> no-op (prevent re-purchase flow)
        val onPurchaseCompleteFp = object : Fingerprint(name = "OnPurchaseComplete") {}
        val onPurchaseCompleteMethod = onPurchaseCompleteFp.methodOrNull
        if (onPurchaseCompleteMethod?.implementation != null) {
            try {
                onPurchaseCompleteMethod.addInstructions(0, "return-void")
                patchedMethods.add("OnPurchaseComplete")
                patched++
            } catch (_: Exception) {}
        }

        // hasReceipt / getHasReceipt -> true
        for (receiptName in listOf("hasReceipt", "getHasReceipt")) {
            val fp = object : Fingerprint(
                name = receiptName,
                returnType = "Z",
            ) {}
            val method = fp.methodOrNull
            if (method?.implementation != null) {
                try {
                    method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                    patchedMethods.add(receiptName)
                    patched++
                } catch (_: Exception) {}
            }
        }

        // ──────────────────────────────────────────────
        // XSOLLA
        // ──────────────────────────────────────────────

        // Xsolla launchBillingFlow -> OK
        val xsollaLaunchFp = object : Fingerprint(
            name = "launchBillingFlow",
            custom = { _, classDef -> classDef.type.lowercase().contains("xsolla") },
        ) {}
        val xsollaLaunchMethod = xsollaLaunchFp.methodOrNull
        if (xsollaLaunchMethod?.implementation != null) {
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
                patchedMethods.add("Xsolla.launchBillingFlow")
                patched++
            } catch (_: Exception) {}
        }

        // Xsolla isAvailable / isUserAvailable -> true
        for (xsollaBool in listOf("isAvailable", "isUserAvailable", "isPaymentAvailable")) {
            val fp = object : Fingerprint(
                name = xsollaBool,
                returnType = "Z",
                custom = { _, classDef -> classDef.type.lowercase().contains("xsolla") },
            ) {}
            val method = fp.methodOrNull
            if (method?.implementation != null) {
                try {
                    method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                    patchedMethods.add("Xsolla.$xsollaBool")
                    patched++
                } catch (_: Exception) {}
            }
        }

        // Xsolla getAmount / getBalance -> 999999
        for (xsollaGetter in listOf("getAmount", "getBalance")) {
            val fp = object : Fingerprint(
                name = xsollaGetter,
                returnType = "I",
                custom = { _, classDef -> classDef.type.lowercase().contains("xsolla") },
            ) {}
            val method = fp.methodOrNull
            if (method?.implementation != null) {
                try {
                    method.addInstructions(0, "const v0, 0xf423f\nreturn v0")
                    patchedMethods.add("Xsolla.$xsollaGetter")
                    patched++
                } catch (_: Exception) {}
            }
        }

        // Xsolla payStationOpen -> no-op (prevent payment UI)
        val xsollaPayStationFp = object : Fingerprint(
            name = "openPayStation",
            custom = { _, classDef -> classDef.type.lowercase().contains("xsolla") },
        ) {}
        val xsollaPayStationMethod = xsollaPayStationFp.methodOrNull
        if (xsollaPayStationMethod?.implementation != null) {
            try {
                xsollaPayStationMethod.addInstructions(0, "return-void")
                patchedMethods.add("Xsolla.openPayStation")
                patched++
            } catch (_: Exception) {}
        }

        // ──────────────────────────────────────────────
        // RECEIPT / SIGNATURE VERIFICATION
        // ──────────────────────────────────────────────

        // verifySignature / verifyPurchase / isValidSignature / validateReceipt -> true
        for (verifyName in listOf(
            "verifySignature", "verifyPurchase", "isValidSignature", "validateReceipt",
            "verifyReceipt", "checkReceipt", "isReceiptValid", "validateSignature",
            "verify", "checkSignature", "isValid",
        )) {
            val fp = object : Fingerprint(
                name = verifyName,
                returnType = "Z",
            ) {}
            val method = fp.methodOrNull
            if (method?.implementation != null) {
                try {
                    method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                    patchedMethods.add(verifyName)
                    patched++
                } catch (_: Exception) {}
            }
        }

        // Security.verify methods
        val securityVerifyFp = object : Fingerprint(
            returnType = "Z",
            custom = { method, classDef ->
                classDef.type.contains("Security") && method.name.lowercase().contains("verify")
            },
        ) {}
        val securityVerifyMethod = securityVerifyFp.methodOrNull
        if (securityVerifyMethod?.implementation != null) {
            try {
                securityVerifyMethod.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                patchedMethods.add("Security.verify")
                patched++
            } catch (_: Exception) {}
        }

        // isFeatureSupported -> OK / true
        val isFeatureSupportedFp = object : Fingerprint(name = "isFeatureSupported") {}
        val isFeatureSupportedMethod = isFeatureSupportedFp.methodOrNull
        if (isFeatureSupportedMethod?.implementation != null) {
            try {
                when {
                    isFeatureSupportedMethod.returnType.contains("BillingResult") -> {
                        isFeatureSupportedMethod.addInstructions(0, """
                            invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                            move-result-object v0
                            const/4 v1, 0x0
                            invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                            move-result-object v0
                            invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                            move-result-object v0
                            return-object v0
                        """.trimIndent())
                    }
                    isFeatureSupportedMethod.returnType == "I" -> isFeatureSupportedMethod.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                    isFeatureSupportedMethod.returnType == "Z" -> isFeatureSupportedMethod.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                }
                patchedMethods.add("isFeatureSupported")
                patched++
            } catch (_: Exception) {}
        }

        // getConnectionState -> 2 (CONNECTED)
        val getConnectionStateFp = object : Fingerprint(name = "getConnectionState") {}
        val getConnectionStateMethod = getConnectionStateFp.methodOrNull
        if (getConnectionStateMethod?.implementation != null) {
            try {
                getConnectionStateMethod.addInstructions(0, "const/4 v0, 0x2\nreturn v0")
                patchedMethods.add("getConnectionState")
                patched++
            } catch (_: Exception) {}
        }

        // onBillingSetupFinished -> fire with OK BillingResult
        val onBillingSetupFinishedFp = object : Fingerprint(name = "onBillingSetupFinished") {}
        val onBillingSetupFinishedMethod = onBillingSetupFinishedFp.methodOrNull
        if (onBillingSetupFinishedMethod?.implementation != null) {
            try {
                onBillingSetupFinishedMethod.addInstructions(0, """
                    invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                    move-result-object v0
                    const/4 v1, 0x0
                    invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                    move-result-object v0
                    invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                    move-result-object v0
                    invoke-interface {p0, v0}, Lcom/android/billingclient/api/BillingClientStateListener;->onBillingSetupFinished(Lcom/android/billingclient/api/BillingResult;)V
                    return-void
                """.trimIndent())
                patchedMethods.add("onBillingSetupFinished")
                patched++
            } catch (_: Exception) {}
        }

        // getSkuDetails (old AIDL) -> empty Bundle
        val getSkuDetailsFp = object : Fingerprint(
            name = "getSkuDetails",
            returnType = "Landroid/os/Bundle;",
        ) {}
        val getSkuDetailsMethod = getSkuDetailsFp.methodOrNull
        if (getSkuDetailsMethod?.implementation != null) {
            try {
                getSkuDetailsMethod.addInstructions(0, """
                    new-instance v0, Landroid/os/Bundle;
                    invoke-direct {v0}, Landroid/os/Bundle;-><init>()V
                    const-string v1, "RESPONSE_CODE"
                    const/4 v2, 0x0
                    invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V
                    return-object v0
                """.trimIndent())
                patchedMethods.add("getSkuDetails")
                patched++
            } catch (_: Exception) {}
        }

        // ──────────────────────────────────────────────
        // REPORT
        // ──────────────────────────────────────────────

        if (patched > 0) {
            logger.info("Free In-app Purchases: patched $patched check(s)")
            logger.info("Patched methods: ${patchedMethods.sorted().joinToString(", ")}")
        } else {
            logger.warning("No billing/purchase checks found. No changes applied.")
        }
    }
}
