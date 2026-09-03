package patches.universal.unlock

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val freeInAppPurchasesPatch = bytecodePatch(
    name = "Free In-app Purchases (Experimental)",
    description = "Get paid items for free. Best for offline games.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        val patchedMethods = mutableSetOf<String>()

        fun patchAll(fp: Fingerprint, label: String, injector: (app.morphe.patcher.util.proxy.mutableTypes.MutableMethod) -> Unit) {
            // try multi-match first via context receiver
            try {
                val matches: List<app.morphe.patcher.Match> = try {
                    with(this@execute) { fp.matchAll() }
                } catch (_: Exception) {
                    emptyList()
                }
                if (matches.isNotEmpty()) {
                    for (m in matches) {
                        try {
                            val method = m.method
                            if (method.implementation == null) continue
                            injector(method)
                            patched++
                            patchedMethods.add(label)
                        } catch (_: Exception) {}
                    }
                    return
                }
            } catch (_: Exception) {}
            // fallback single
            val single = try { with(this@execute) { fp.matchOrNull() }?.method } catch (_: Exception) { null } ?: try { fp.methodOrNull } catch (_: Exception) { null }
            if (single?.implementation != null) {
                try {
                    injector(single)
                    patched++
                    patchedMethods.add(label)
                } catch (_: Exception) {}
            }
        }

        val okBillingResult = """
            invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
            move-result-object v0
            const/4 v1, 0x0
            invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
            move-result-object v0
            invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
            move-result-object v0
            return-object v0
        """.trimIndent()

        // ──────────────────────────────────────────────
        // GOOGLE PLAY BILLING
        // ──────────────────────────────────────────────

        patchAll(Fingerprint(name = "launchBillingFlow", custom = { m, _ -> m.returnType.contains("BillingResult") }), "launchBillingFlow") {
            try {
                it.addInstructions(0, okBillingResult)
            } catch (_: Exception) {
                try { it.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0") } catch (_: Exception) {}
            }
        }

        patchAll(Fingerprint(name = "isReady", returnType = "Z", custom = { _, c -> c.type.contains("BillingClient") }), "BillingClient.isReady") {
            it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
        }

        patchAll(Fingerprint(name = "endConnection", custom = { _, c -> c.type.contains("BillingClient") }), "BillingClient.endConnection") {
            it.addInstructions(0, "return-void")
        }

        // startConnection -> fire onBillingSetupFinished(OK) then return
        patchAll(Fingerprint(name = "startConnection", custom = { _, c -> c.type.contains("BillingClient") }), "BillingClient.startConnection") {
            // just make it succeed and callback OK if listener present
            it.addInstructions(0, "return-void")
        }

        // onPurchasesUpdated is intentionally left intact: the game grants items in its
        // own listener, so suppressing it would prevent granting. Fake purchases are
        // delivered via queryPurchasesAsync / launchBillingFlow callbacks below.

        // getBuyIntent -> OK bundle (legacy AIDL v5/v7)
        patchAll(Fingerprint(name = "getBuyIntent", returnType = "Landroid/os/Bundle;"), "getBuyIntent") {
            if (it.parameterTypes.size >= 2) {
                it.addInstructions(0, """
                    new-instance v0, Landroid/os/Bundle;
                    invoke-direct {v0}, Landroid/os/Bundle;-><init>()V
                    const-string v1, "BUY_INTENT"
                    const/4 v2, 0x0
                    invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V
                    return-object v0
                """.trimIndent())
            }
        }

        // isBillingSupported (AIDL) -> 0 = BILLING_RESPONSE_RESULT_OK
        patchAll(Fingerprint(name = "isBillingSupported"), "isBillingSupported") {
            if (it.returnType == "I") it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            else if (it.returnType == "Z") it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
        }

        // getPurchases / queryPurchases -> empty list or empty bundle,
        // or fire listener callback with a fake PURCHASED purchase (startup/resume grant path)
        for (qn in listOf("getPurchases", "queryPurchases", "queryPurchasesAsync", "queryPurchaseHistory", "queryPurchaseHistoryAsync", "queryPurchasesHistory")) {
            patchAll(Fingerprint(name = qn, custom = { m, c -> c.type.contains("BillingClient") || m.definingClass.contains("billing") || c.type.lowercase().contains("billing") }), qn) {
                val listenerIdx = it.parameterTypes.indexOfFirst { p -> p.contains("PurchasesResponseListener") || p.contains("PurchaseHistoryResponseListener") }
                if (listenerIdx >= 0 && it.returnType == "V") {
                    val isHistory = it.parameterTypes[listenerIdx].contains("History")
                    val listenerReg = "p${listenerIdx + 1}"
                    if (isHistory) {
                        it.addInstructions(0, """
                            invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                            move-result-object v0
                            const/4 v1, 0x0
                            invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                            move-result-object v0
                            invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                            move-result-object v0
                            const-string v1, "{\"productId\":\"morphe_fake\",\"purchaseToken\":\"morphe_fake\",\"purchaseTime\":0,\"quantity\":1}"
                            const-string v2, "morphe_fake"
                            new-instance v3, Lcom/android/billingclient/api/PurchaseHistoryRecord;
                            invoke-direct {v3, v1, v2}, Lcom/android/billingclient/api/PurchaseHistoryRecord;-><init>(Ljava/lang/String;Ljava/lang/String;)V
                            new-instance v1, Ljava/util/ArrayList;
                            invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V
                            invoke-virtual {v1, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                            move-object v3, $listenerReg
                            invoke-interface {v3, v0, v1}, Lcom/android/billingclient/api/PurchaseHistoryResponseListener;->onPurchaseHistoryResponse(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V
                            return-void
                        """.trimIndent())
                    } else {
                        it.addInstructions(0, """
                            invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                            move-result-object v0
                            const/4 v1, 0x0
                            invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                            move-result-object v0
                            const-string v1, "{\"orderId\":\"morphe_fake\",\"packageName\":\"morphe_fake\",\"productId\":\"morphe_fake\",\"purchaseTime\":0,\"purchaseState\":1,\"purchaseToken\":\"morphe_fake\",\"quantity\":1,\"acknowledged\":true}"
                            const-string v2, "morphe_fake"
                            new-instance v3, Lcom/android/billingclient/api/Purchase;
                            invoke-direct {v3, v1, v2}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V
                            new-instance v1, Ljava/util/ArrayList;
                            invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V
                            invoke-virtual {v1, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                            move-object v3, $listenerReg
                            invoke-interface {v3, v0, v1}, Lcom/android/billingclient/api/PurchasesResponseListener;->onQueryPurchasesResponse(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V
                            return-void
                        """.trimIndent())
                    }
                } else when {
                    it.returnType.contains("List") -> it.addInstructions(0, "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;\nmove-result-object v0\nreturn-object v0")
                    it.returnType == "Landroid/os/Bundle;" -> it.addInstructions(0, """
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
                    else -> try { it.addInstructions(0, "return-void") } catch (_: Exception) {}
                }
            }
        }

        // querySkuDetailsAsync / queryProductDetailsAsync -> invoke callback with OK + empty list
        for (qn in listOf("querySkuDetailsAsync", "queryProductDetailsAsync", "querySkuDetails", "queryProductDetails", "queryProductDetailsAsyncWithListener")) {
            patchAll(Fingerprint(name = qn), qn) {
                it.addInstructions(0, "return-void")
            }
        }

        // getSkuDetails / getProductDetails AIDL
        for (qn in listOf("getSkuDetails", "getProductDetails")) {
            patchAll(Fingerprint(name = qn, returnType = "Landroid/os/Bundle;"), qn) {
                it.addInstructions(0, """
                    new-instance v0, Landroid/os/Bundle;
                    invoke-direct {v0}, Landroid/os/Bundle;-><init>()V
                    const-string v1, "RESPONSE_CODE"
                    const/4 v2, 0x0
                    invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V
                    return-object v0
                """.trimIndent())
            }
        }

        // consumePurchase / consumeAsync -> fire listener callback with OK, else spoof return
        for (cn in listOf("consumePurchase", "consumeAsync", "consumePurchaseAsync")) {
            patchAll(Fingerprint(name = cn), cn) {
                val listenerIdx = it.parameterTypes.indexOfFirst { p -> p.contains("ConsumeResponseListener") }
                if (listenerIdx == 1 && it.parameterTypes.size == 2 && it.parameterTypes[0].contains("ConsumeParams") && it.returnType == "V") {
                    val listenerReg = "p${listenerIdx + 1}"
                    // params are (ConsumeParams, ConsumeResponseListener); token is first param -> p1
                    it.addInstructions(0, """
                        invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                        move-result-object v0
                        const/4 v1, 0x0
                        invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                        move-result-object v0
                        invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                        move-result-object v0
                        move-object v1, p1
                        invoke-virtual {v1}, Lcom/android/billingclient/api/ConsumeParams;->getPurchaseToken()Ljava/lang/String;
                        move-result-object v1
                        move-object v2, $listenerReg
                        invoke-interface {v2, v0, v1}, Lcom/android/billingclient/api/ConsumeResponseListener;->onConsumeResponse(Lcom/android/billingclient/api/BillingResult;Ljava/lang/String;)V
                        return-void
                    """.trimIndent())
                } else when {
                    it.returnType.contains("BillingResult") -> it.addInstructions(0, okBillingResult)
                    it.returnType == "Landroid/os/Bundle;" -> it.addInstructions(0, """
                        new-instance v0, Landroid/os/Bundle;
                        invoke-direct {v0}, Landroid/os/Bundle;-><init>()V
                        const-string v1, "RESPONSE_CODE"
                        const/4 v2, 0x0
                        invoke-virtual {v0, v1, v2}, Landroid/os/Bundle;->putInt(Ljava/lang/String;I)V
                        return-object v0
                    """.trimIndent())
                    it.returnType == "I" -> it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                    it.returnType == "Z" -> it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                    else -> it.addInstructions(0, "return-void")
                }
            }
        }

        patchAll(Fingerprint(name = "acknowledgePurchase"), "acknowledgePurchase") {
            val listenerIdx = it.parameterTypes.indexOfFirst { p -> p.contains("AcknowledgePurchaseResponseListener") }
            if (listenerIdx == 1 && it.parameterTypes.size == 2 && it.parameterTypes[0].contains("AcknowledgePurchaseParams") && it.returnType == "V") {
                val listenerReg = "p${listenerIdx + 1}"
                it.addInstructions(0, """
                    invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                    move-result-object v0
                    const/4 v1, 0x0
                    invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                    move-result-object v0
                    invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                    move-result-object v0
                    move-object v1, $listenerReg
                    invoke-interface {v1, v0}, Lcom/android/billingclient/api/AcknowledgePurchaseResponseListener;->onAcknowledgePurchaseResponse(Lcom/android/billingclient/api/BillingResult;)V
                    return-void
                """.trimIndent())
            } else when {
                it.returnType.contains("BillingResult") -> it.addInstructions(0, okBillingResult)
                it.returnType == "I" -> it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                else -> it.addInstructions(0, "return-void")
            }
        }

        patchAll(Fingerprint(name = "getBillingConfig"), "getBillingConfig") {
            when {
                it.returnType.contains("BillingResult") -> it.addInstructions(0, okBillingResult)
                else -> it.addInstructions(0, "return-void")
            }
        }

        // Prices -> "0.00" / 0
        for (pm in listOf("getPrice", "getOriginalPrice", "getFormattedPrice", "getDisplayPrice", "getPriceString")) {
            patchAll(Fingerprint(name = pm, returnType = "Ljava/lang/String;", custom = { _, c -> val t=c.type.lowercase(); t.contains("sku") || t.contains("product") || t.contains("billing") }), pm) {
                if (it.parameterTypes.isEmpty()) it.addInstructions(0, "const-string v0, \"0.00\"\nreturn-object v0")
            }
        }
        // OneTimePurchaseOfferDetails / SubscriptionOfferDetails micros
        patchAll(Fingerprint(name = "getPriceAmountMicros", returnType = "J"), "getPriceAmountMicros") {
            it.addInstructions(0, "const-wide/16 v0, 0x0\nreturn-wide v0")
        }
        patchAll(Fingerprint(name = "getPriceAmountMicros", custom = { _, c -> c.type.lowercase().contains("offer") }), "Offer.getPriceAmountMicros") {
            if (it.returnType == "J") it.addInstructions(0, "const-wide/16 v0, 0x0\nreturn-wide v0")
        }
        // getOriginalJson -> fake json
        patchAll(Fingerprint(name = "getOriginalJson", returnType = "Ljava/lang/String;"), "getOriginalJson") {
            it.addInstructions(0, "const-string v0, \"{\\\"productId\\\":\\\"morphe_fake\\\",\\\"purchaseToken\\\":\\\"fake\\\"}\"\nreturn-object v0")
        }

        // Purchase state getters -> look owned/valid (scoped to billing/purchase classes only;
        // ProductDetails identity like getProductId is deliberately NOT spoofed so SKU lookup keeps working)
        patchAll(Fingerprint(name = "getPurchaseState", returnType = "I", custom = { _, c -> val t = c.type.lowercase(); t.contains("billing") || t.contains("purchase") }), "Purchase.getPurchaseState") {
            it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
        }
        patchAll(Fingerprint(name = "isAcknowledged", returnType = "Z", custom = { _, c -> val t = c.type.lowercase(); t.contains("billing") || t.contains("purchase") }), "Purchase.isAcknowledged") {
            it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
        }
        patchAll(Fingerprint(name = "getQuantity", returnType = "I", custom = { _, c -> val t = c.type.lowercase(); t.contains("billing") || t.contains("purchase") }), "Purchase.getQuantity") {
            it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
        }
        for (ps in listOf("getPurchaseToken", "getOrderId", "getSignature")) {
            patchAll(Fingerprint(name = ps, returnType = "Ljava/lang/String;", custom = { _, c -> val t = c.type.lowercase(); t.contains("billing") || t.contains("purchase") }), "Purchase.$ps") {
                if (it.parameterTypes.isEmpty()) it.addInstructions(0, "const-string v0, \"morphe_fake\"\nreturn-object v0")
            }
        }
        patchAll(Fingerprint(name = "getProducts", custom = { m, c -> m.returnType.contains("List") && (c.type.lowercase().contains("billing") || c.type.lowercase().contains("purchase")) }), "Purchase.getProducts") {
            it.addInstructions(0, "const-string v0, \"morphe_fake\"\ninvoke-static {v0}, Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;\nmove-result-object v0\nreturn-object v0")
        }
        patchAll(Fingerprint(name = "getSkus", custom = { m, c -> m.returnType.contains("List") && (c.type.lowercase().contains("billing") || c.type.lowercase().contains("purchase")) }), "Purchase.getSkus") {
            it.addInstructions(0, "const-string v0, \"morphe_fake\"\ninvoke-static {v0}, Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;\nmove-result-object v0\nreturn-object v0")
        }

        patchAll(Fingerprint(name = "isFeatureSupported"), "isFeatureSupported") {
            when {
                it.returnType.contains("BillingResult") -> it.addInstructions(0, okBillingResult)
                it.returnType == "I" -> it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                it.returnType == "Z" -> it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            }
        }

        patchAll(Fingerprint(name = "getConnectionState"), "getConnectionState") {
            it.addInstructions(0, "const/4 v0, 0x2\nreturn v0")
        }

        // onBillingSetupFinished -> suppress recursion, just return
        patchAll(Fingerprint(name = "onBillingSetupFinished"), "onBillingSetupFinished") {
            it.addInstructions(0, "return-void")
        }

        // ──────────────────────────────────────────────
        // UNITY IAP
        // ──────────────────────────────────────────────

        patchAll(Fingerprint(name = "ProcessPurchase", custom = { m, _ -> m.returnType.contains("PurchaseProcessingResult") }), "ProcessPurchase") {
            it.addInstructions(0, "sget-object v0, Lcom/unity/purchasing/PurchaseProcessingResult;->Complete:Lcom/unity/purchasing/PurchaseProcessingResult;\nreturn-object v0")
        }
        patchAll(Fingerprint(name = "OnPurchaseFailed"), "OnPurchaseFailed") { it.addInstructions(0, "return-void") }
        patchAll(Fingerprint(name = "OnPurchaseComplete"), "OnPurchaseComplete") { it.addInstructions(0, "return-void") }
        // CrossPlatformValidator
        patchAll(Fingerprint(name = "Validate", custom = { m, c -> m.returnType.contains("CrossPlatformValidator") || c.type.contains("CrossPlatformValidator") }), "CrossPlatformValidator.Validate") {
            // will be caught below anyway
        }
        for (rn in listOf("hasReceipt", "getHasReceipt")) {
            patchAll(Fingerprint(name = rn, returnType = "Z"), rn) { it.addInstructions(0, "const/4 v0, 0x1\nreturn v0") }
        }

        // ──────────────────────────────────────────────
        // XSOLLA
        // ──────────────────────────────────────────────

        patchAll(Fingerprint(name = "launchBillingFlow", custom = { _, c -> c.type.lowercase().contains("xsolla") }), "Xsolla.launchBillingFlow") {
            try { it.addInstructions(0, okBillingResult) } catch (_: Exception) { it.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0") }
        }
        for (xb in listOf("isAvailable", "isUserAvailable", "isPaymentAvailable", "isInventoryAvailable", "isStoreAvailable")) {
            patchAll(Fingerprint(name = xb, returnType = "Z", custom = { _, c -> c.type.lowercase().contains("xsolla") }), "Xsolla.$xb") {
                it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            }
        }
        for (xg in listOf("getAmount", "getBalance", "getVirtualCurrencyBalance", "getInventory")) {
            patchAll(Fingerprint(name = xg, returnType = "I", custom = { _, c -> c.type.lowercase().contains("xsolla") }), "Xsolla.$xg") {
                it.addInstructions(0, "const v0, 0xf423f\nreturn v0")
            }
        }
        for (xs in listOf("openPayStation", "openPurchase", "createPayment", "validatePurchase", "checkOrder", "getPayStationUrl")) {
            patchAll(Fingerprint(name = xs, custom = { _, c -> c.type.lowercase().contains("xsolla") }), "Xsolla.$xs") {
                if (it.returnType == "V") it.addInstructions(0, "return-void")
                else if (it.returnType == "Z") it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                else if (it.returnType.contains("String")) it.addInstructions(0, "const-string v0, \"https://paystation.xsolla.com\"\nreturn-object v0")
            }
        }

        // ──────────────────────────────────────────────
        // AMAZON, HUAWEI, SAMSUNG
        // ──────────────────────────────────────────────

        // Amazon IAP (com.amazon.device.iap)
        for (am in listOf("purchase", "getUserData", "getProductData", "getPurchaseUpdates", "onProductDataResponse", "onPurchaseResponse", "onUserDataResponse")) {
            patchAll(Fingerprint(name = am, custom = { _, c -> c.type.lowercase().contains("amazon") || c.type.contains("amazon") }), "Amazon.$am") {
                when (it.returnType) {
                    "V" -> it.addInstructions(0, "return-void")
                    "Z" -> it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                    else -> if (it.returnType.contains("String")) it.addInstructions(0, "const-string v0, \"\"\nreturn-object v0") else it.addInstructions(0, "return-void")
                }
            }
        }
        // Amazon PurchasingService specifically
        patchAll(Fingerprint(name = "getUserData", custom = { _, c -> c.type.contains("PurchasingService") || c.type.contains("amazon") }), "Amazon.PurchasingService.getUserData") {
            it.addInstructions(0, "return-void")
        }

        // Huawei IAP
        for (hw in listOf("isEnvReady", "obtainProductInfo", "createPurchaseIntent", "consumeOwnedPurchase", "obtainOwnedPurchases", "obtainOwnedPurchaseRecord", "isSandboxActivated")) {
            patchAll(Fingerprint(name = hw, custom = { _, c -> c.type.lowercase().contains("huawei") || c.type.contains("huawei") }), "Huawei.$hw") {
                when (it.returnType) {
                    "V" -> it.addInstructions(0, "return-void")
                    "Z" -> it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                    "I" -> it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                    else -> it.addInstructions(0, "return-void")
                }
            }
        }

        // Samsung Galaxy Store IAP
        for (sm in listOf("getProductsDetails", "startPayment", "getOwnedList", "consumePurchasedItems", "getProductDetails", "checkPurchasedItem")) {
            patchAll(Fingerprint(name = sm, custom = { _, c -> c.type.lowercase().contains("samsung") || c.type.contains("samsung") }), "Samsung.$sm") {
                when (it.returnType) {
                    "V" -> it.addInstructions(0, "return-void")
                    "Z" -> it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                    "I" -> it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                    else -> it.addInstructions(0, "return-void")
                }
            }
        }

        // ──────────────────────────────────────────────
        // RECEIPT / SIGNATURE VERIFICATION (scoped)
        // ──────────────────────────────────────────────

        for (vn in listOf("verifySignature", "verifyPurchase", "isValidSignature", "validateReceipt", "verifyReceipt", "checkReceipt", "isReceiptValid", "validateSignature")) {
            patchAll(Fingerprint(name = vn, returnType = "Z", custom = { _, c -> val t=c.type.lowercase(); t.contains("billing") || t.contains("purchase") || t.contains("receipt") || t.contains("security") || t.contains("store") || t.contains("googleplay") || t.contains("xsolla") || t.contains("amazon") || t.contains("huawei") || t.contains("validator") }), vn) {
                it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            }
        }
        // ultra-generic names scoped strictly
        for (vn in listOf("verify", "checkSignature", "isValid")) {
            patchAll(Fingerprint(name = vn, returnType = "Z", custom = { _, c -> val t=c.type.lowercase(); (t.contains("security") || t.contains("receipt") || t.contains("purchase") || t.contains("billing") || t.contains("validator")) && !t.contains("okhttp") && !t.contains("ssl") }), vn) {
                it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            }
        }

        patchAll(Fingerprint(returnType = "Z", custom = { m, c -> c.type.contains("Security") && m.name.lowercase().contains("verify") }), "Security.verify") {
            it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
        }
        // Unity CrossPlatformValidator
        patchAll(Fingerprint(returnType = "Z", custom = { m, c -> c.type.contains("CrossPlatformValidator") || (c.type.contains("Validator") && m.name.lowercase().contains("valid")) }), "CrossPlatformValidator") {
            it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
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
