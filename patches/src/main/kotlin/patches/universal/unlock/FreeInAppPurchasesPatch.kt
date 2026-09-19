package patches.universal.unlock

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.removeInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.booleanOption
import patches.universal.ads.util.cloneMutable
import java.util.logging.Logger
import patches.universal.ads.util.DiscordPromo
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable

@Suppress("unused")
val freeInAppPurchasesPatch = bytecodePatch(
    name = "Free In-app Purchases",
    description = "Get paid items free: buying grants items without charging. Best for offline games.",
    default = false,
) {
    category("Featured")
    val fakeStartupPurchases by booleanOption(
        title = "Fake owned purchases at startup",
        default = false,
        key = "fakeStartupPurchases",
        description = "Deliver a fake owned purchase on every inventory query. Helps games that only grant at boot, but can stall strict Unity titles. Leave off if a game hangs on loading.",
    )
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        DiscordPromo.logOnce(logger)
        var patched = 0
        val patchedMethods = mutableSetOf<String>()

        // Minimum registers a frame provably holds: param slots (J/D count
        // double) plus this for instance methods. Injected blocks use fixed
        // low regs, and writing past the frame fails verification for the
        // whole class (frozen loading screens), so every injection below is
        // gated on the frame holding it.
        fun minRegs(m: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod): Int {
            return try {
                var slots = 0
                if (!com.android.tools.smali.dexlib2.AccessFlags.STATIC.isSet(m.accessFlags)) slots += 1
                for (p in m.parameterTypes) slots += if (p == "J" || p == "D") 2 else 1
                slots
            } catch (_: Exception) { 0 }
        }
        // Frame expansion for injections needing more regs than the frame
        // holds: clone with extra registers and swap the clone in. The
        // prologue cloneMutable adds is harmless because expanded injections
        // always return before the original body runs.
        // dropTryBlocks: the clone keeps the original try blocks VERBATIM,
        // but prepending shifts every instruction, so stale ranges cover
        // the injected code and ART merges handler states into it
        // (VerifyError on the whole class, seen on Nice Dice 3D). Only
        // safe when the block returns before the original body (which then
        // is dead anyway); fall-through injections must keep them.
        fun expandSwap(m: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod, block: String): Boolean {
            return try {
                val owner = mutableClassDefByOrNull(m.definingClass) ?: return false
                val target = owner.methods.firstOrNull {
                    it.name == m.name && it.parameterTypes == m.parameterTypes && it.returnType == m.returnType
                } ?: return false
                val cloned = m.cloneMutable(additionalRegisters = 4)
                owner.methods.remove(target)
                cloned.addInstructions(0, block)
                owner.methods.add(cloned)
                logger.info("FreeIAP expanded frame: ${m.definingClass}->${m.name}")
                true
            } catch (_: Exception) { false }
        }
        fun patchAll(fp: Fingerprint, label: String, needRegs: Int = 1, injector: (app.morphe.patcher.util.proxy.mutableTypes.MutableMethod) -> Unit) {
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
                            if (minRegs(method) < needRegs) {
                                logger.info("FreeIAP skipped tiny frame: ${method.definingClass}->${method.name} regs=${minRegs(method)} need=$needRegs label=$label")
                                continue
                            }
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
                    if (minRegs(single) < needRegs) return
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

        // Resolve iget chain loading the PurchasesUpdatedListener into v0
        // on a BillingClient impl. Billing 5-7 holds it directly; billing
        // 8+ buries it in a holder (e.g. zze:zzn -> zzn.zzb). Field names
        // are obfuscated per version, so resolve by TYPE at patch time.
        fun listenerIget(defClass: String): String? {
            return try {
                val cls = mutableClassDefByOrNull(defClass) ?: return null
                val direct = cls.fields.firstOrNull {
                    it.type == "Lcom/android/billingclient/api/PurchasesUpdatedListener;"
                }
                if (direct != null) {
                    return "iget-object v0, v0, $defClass->${direct.name}:${direct.type}"
                }
                for (f in cls.fields) {
                    val holder = f.type
                    if (!holder.startsWith("Lcom/android/billingclient/api/")) continue
                    if (holder.contains("Listener;")) continue
                    val holderCls = try { mutableClassDefByOrNull(holder) } catch (_: Exception) { null } ?: continue
                    val inner = holderCls.fields.firstOrNull {
                        it.type == "Lcom/android/billingclient/api/PurchasesUpdatedListener;"
                    } ?: continue
                    return "iget-object v0, v0, $defClass->${f.name}:${f.type}\n" +
                        "iget-object v0, v0, $holder->${inner.name}:${inner.type}"
                }
                null
            } catch (_: Exception) { null }
        }
        // Buy-time grant block for launchBillingFlow (v0..v4, instance
        // methods only): fire onPurchasesUpdated(OK, [fake PURCHASED]) on
        // the client's own listener, then return OK. Null listener falls
        // through to OK-only. This mirrors native MOD-menu behavior: the
        // grant happens when the user buys, while init/query/catalog paths
        // stay stock so strict titles keep booting.
        // NOTE (morphe inline-smali quirk, verified by assembling test
        // fragments): p-regs resolving above v15 FAIL to assemble in
        // methods with big frames, and the failed line is silently
        // dropped. Every injection below therefore copies params with
        // move-*/from16 (which assembles in any frame) and otherwise
        // touches only v-regs. NEVER use a narrow opcode with a p-reg.
        //
        // VERIFIER RULE (learned from on-device VerifyError killing the
        // whole BillingClientImpl on Nice Dice 3D): ART rejects a method
        // when one register holds an int at one branch join and a
        // reference at another, or when fall-through code retypes regs
        // the original body still expects. So: v0..v3 hold OBJECTS ONLY,
        // v4 holds INTS ONLY, every path returns directly (no shared
        // :done join merging mismatched types), and fall-through
        // injections go through cloneMutable (see startConnection below).
        // Uniqueness comes from UUID (object ops only) instead of
        // currentTimeMillis (wide pair would need a 6th register).
        //
        // The fake purchase carries the REAL requested product id read
        // from the BillingFlowParams at runtime (Billing 5+ details list,
        // legacy SkuDetails, ancient direct getSku, in that order). Games
        // grant items by matching purchase.productId against their catalog,
        // so a hardcoded id matches nothing and the tap silently does
        // nothing (e.g. Subway Surfers City). Which API exists is resolved
        // at PATCH time against the app's BillingFlowParams class, so no
        // missing-method crash is possible at runtime. flowReg is the p-reg
        // holding BillingFlowParams (null when unknown, e.g. the native
        // launchBillingFlowCpp bridge) and falls back to "morphe_fake".
        fun billingApiHas(mName: String): Boolean {
            return try {
                mutableClassDefByOrNull("Lcom/android/billingclient/api/BillingFlowParams;")?.methods?.any { it.name == mName } == true
            } catch (_: Exception) { false }
        }
        fun flowParamsReg(m: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod, isStatic: Boolean): String? {
            val idx = m.parameterTypes.indexOfFirst { it.contains("BillingFlowParams") }
            if (idx < 0) return null
            return "p${idx + (if (isStatic) 0 else 1)}"
        }
        val bfpClass = "Lcom/android/billingclient/api/BillingFlowParams;"
        val pdpClass = "Lcom/android/billingclient/api/BillingFlowParams\$ProductDetailsParams;"
        val pdClass = "Lcom/android/billingclient/api/ProductDetails;"
        // No-arg method on defClass with the given return type (obfuscation-proof lookup).
        fun noArgMethods(defClass: String, returns: Set<String>): List<Pair<String, String>> {
            return try {
                mutableClassDefByOrNull(defClass)?.methods
                    ?.filter { it.parameterTypes.isEmpty() && it.returnType in returns }
                    ?.map { it.name to it.returnType } ?: emptyList()
            } catch (_: Exception) { emptyList() }
        }
        // No-arg method on ProductDetailsParams returning ProductDetails
        // (named getProductDetails when kept, zza() when obfuscated).
        fun pdpDetailsGetter(): String? {
            return try {
                val ms = mutableClassDefByOrNull(pdpClass)?.methods ?: return null
                (ms.firstOrNull { it.parameterTypes.isEmpty() && it.name == "getProductDetails" }
                    ?: ms.firstOrNull { it.parameterTypes.isEmpty() && it.returnType == pdClass })?.name
            } catch (_: Exception) { null }
        }
        // Exact name only: never guess among the many String getters
        // (getTitle/getDescription would grant the wrong identity).
        fun productIdGetter(): String? {
            return try {
                if (mutableClassDefByOrNull(pdClass)?.methods?.any { it.parameterTypes.isEmpty() && it.name == "getProductId" } == true) "getProductId" else null
            } catch (_: Exception) { null }
        }
        // Details-list getters on BillingFlowParams, modern shape. Named API
        // first; on obfuscated Billing (R8 renames the getter to zza()/zzf())
        // discover by signature instead. Gated on the ProductDetailsParams
        // class existing so this only runs on the Billing 5+ shape, List
        // getter preferred (pairs with the Builder setter taking List).
        fun detailsListGetters(): List<Pair<String, String>> {
            if (billingApiHas("getProductDetailsParamsList")) return listOf("getProductDetailsParamsList" to "Ljava/util/List;")
            val hasShape = try { mutableClassDefByOrNull(pdpClass) != null } catch (_: Exception) { false }
            if (!hasShape) return emptyList()
            val found = noArgMethods(bfpClass, setOf("Ljava/util/List;", "Ljava/util/ArrayList;"))
            if (found.isEmpty()) return emptyList()
            val pdpGetter = pdpDetailsGetter()
            val idGetter = productIdGetter()
            if (pdpGetter == null || idGetter == null) return emptyList()
            return ((found.filter { it.second == "Ljava/util/List;" } + found.filter { it.second == "Ljava/util/ArrayList;" })
                .take(3))
        }
        // intReturn covers native bridges returning a response CODE int
        // (e.g. BillingClientImpl.launchBillingFlowCpp -> I): same grant,
        // then 0 (OK) instead of a BillingResult object.
        fun buyGrantBlock(igetTail: String, flowReg: String?, intReturn: Boolean = false): String {
            // Tails are plain strings (no nested triple-quotes: those would
            // terminate the outer smali block early and break compilation).
            // Both paths return directly so no cross-path :done join can
            // merge mismatched register types (ART VerifyError).
            val grantTail = if (intReturn) "const/4 v0, 0x0\nreturn v0" else "return-object v1"
            val nocbTail = if (intReturn) "const/4 v0, 0x0\nreturn v0" else
                "invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult\$Builder;\n" +
                "move-result-object v1\n" +
                "const/4 v4, 0x0\n" +
                "invoke-virtual {v1, v4}, Lcom/android/billingclient/api/BillingResult\$Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult\$Builder;\n" +
                "move-result-object v1\n" +
                "invoke-virtual {v1}, Lcom/android/billingclient/api/BillingResult\$Builder;->build()Lcom/android/billingclient/api/BillingResult;\n" +
                "move-result-object v1\n" +
                "return-object v1"
            if (flowReg != null) {
                val src = when {
                    billingApiHas("getProductDetailsParamsList") -> "named:getProductDetailsParamsList"
                    else -> {
                        val c = detailsListGetters()
                        if (c.isNotEmpty()) "signature:" + c.joinToString(",") { it.first } + "+" + pdpDetailsGetter() + "+" + productIdGetter()
                        else "static-fallback(no-api)"
                    }
                }
                logger.info("FreeIAP product-id source: $src")
            }
            // Product-id prelude -> v3, using v1 as scratch (v0/v2 kept).
            val pid = if (flowReg == null) {
                "const-string v3, \"morphe_fake\""
            } else {
                val sb = StringBuilder()
                sb.appendLine("move-object/from16 v3, $flowReg")
                if (billingApiHas("getProductDetailsParamsList")) {
                    sb.appendLine("invoke-virtual {v3}, Lcom/android/billingclient/api/BillingFlowParams;->getProductDetailsParamsList()Ljava/util/List;")
                    sb.appendLine("move-result-object v3")
                    sb.appendLine("if-eqz v3, :morphe_iap_pid_fake")
                    sb.appendLine("invoke-interface {v3}, Ljava/util/List;->isEmpty()Z")
                    sb.appendLine("move-result v4")
                    sb.appendLine("if-eqz v4, :morphe_iap_pid_hasitem")
                    sb.appendLine("goto :morphe_iap_pid_fake")
                    sb.appendLine(":morphe_iap_pid_hasitem")
                    sb.appendLine("const/4 v4, 0x0")
                    sb.appendLine("invoke-interface {v3, v4}, Ljava/util/List;->get(I)Ljava/lang/Object;")
                    sb.appendLine("move-result-object v3")
                    sb.appendLine("check-cast v3, Lcom/android/billingclient/api/ProductDetailsParams;")
                    sb.appendLine("invoke-virtual {v3}, Lcom/android/billingclient/api/ProductDetailsParams;->getProductDetails()Lcom/android/billingclient/api/ProductDetails;")
                    sb.appendLine("move-result-object v3")
                    sb.appendLine("if-eqz v3, :morphe_iap_pid_fake")
                    sb.appendLine("invoke-virtual {v3}, Lcom/android/billingclient/api/ProductDetails;->getProductId()Ljava/lang/String;")
                    sb.appendLine("move-result-object v3")
                    sb.appendLine("if-nez v3, :morphe_iap_pid_done")
                } else {
                    // Obfuscated Billing: accessors discovered by signature.
                    // instance-of guards each candidate so a wrong-shaped
                    // list can never crash with ClassCastException.
                    val pdpGet = pdpDetailsGetter()
                    val idGet = productIdGetter()
                    val cands = if (pdpGet != null && idGet != null) detailsListGetters() else emptyList()
                    cands.forEachIndexed { i, g ->
                        sb.appendLine("move-object/from16 v3, $flowReg")
                        sb.appendLine("invoke-virtual {v3}, $bfpClass->${g.first}()${g.second}")
                        sb.appendLine("move-result-object v3")
                        sb.appendLine("if-eqz v3, :morphe_iap_pid_next$i")
                        sb.appendLine("invoke-interface {v3}, Ljava/util/List;->isEmpty()Z")
                        sb.appendLine("move-result v4")
                        sb.appendLine("if-eqz v4, :morphe_iap_pid_has$i")
                        sb.appendLine("goto :morphe_iap_pid_next$i")
                        sb.appendLine(":morphe_iap_pid_has$i")
                        sb.appendLine("const/4 v4, 0x0")
                        sb.appendLine("invoke-interface {v3, v4}, Ljava/util/List;->get(I)Ljava/lang/Object;")
                        sb.appendLine("move-result-object v3")
                        sb.appendLine("instance-of v4, v3, $pdpClass")
                        sb.appendLine("if-eqz v4, :morphe_iap_pid_next$i")
                        sb.appendLine("check-cast v3, $pdpClass")
                        sb.appendLine("invoke-virtual {v3}, $pdpClass->$pdpGet()$pdClass")
                        sb.appendLine("move-result-object v3")
                        sb.appendLine("if-eqz v3, :morphe_iap_pid_next$i")
                        sb.appendLine("invoke-virtual {v3}, $pdClass->$idGet()Ljava/lang/String;")
                        sb.appendLine("move-result-object v3")
                        sb.appendLine("if-nez v3, :morphe_iap_pid_done")
                        sb.appendLine(":morphe_iap_pid_next$i")
                    }
                }
                if (billingApiHas("getSkuDetails")) {
                    sb.appendLine("move-object/from16 v3, $flowReg")
                    sb.appendLine("invoke-virtual {v3}, Lcom/android/billingclient/api/BillingFlowParams;->getSkuDetails()Lcom/android/billingclient/api/SkuDetails;")
                    sb.appendLine("move-result-object v3")
                    sb.appendLine("if-eqz v3, :morphe_iap_pid_fake")
                    sb.appendLine("invoke-virtual {v3}, Lcom/android/billingclient/api/SkuDetails;->getSku()Ljava/lang/String;")
                    sb.appendLine("move-result-object v3")
                    sb.appendLine("if-nez v3, :morphe_iap_pid_done")
                } else if (billingApiHas("getSku")) {
                    sb.appendLine("move-object/from16 v3, $flowReg")
                    sb.appendLine("invoke-virtual {v3}, Lcom/android/billingclient/api/BillingFlowParams;->getSku()Ljava/lang/String;")
                    sb.appendLine("move-result-object v3")
                    sb.appendLine("if-nez v3, :morphe_iap_pid_done")
                }
                sb.appendLine(":morphe_iap_pid_fake")
                sb.appendLine("const-string v3, \"morphe_fake\"")
                sb.appendLine(":morphe_iap_pid_done")
                sb.toString()
            }
            return """
                move-object/from16 v0, p0
                $igetTail
                if-eqz v0, :morphe_iap_nocb
                $pid
                invoke-static {}, Ljava/util/UUID;->randomUUID()Ljava/util/UUID;
                move-result-object v2
                invoke-virtual {v2}, Ljava/util/UUID;->toString()Ljava/lang/String;
                move-result-object v2
                new-instance v1, Ljava/lang/StringBuilder;
                invoke-direct {v1}, Ljava/lang/StringBuilder;-><init>()V
                const-string v0, "{\"orderId\":\"morphe-"
                invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                move-result-object v1
                invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                move-result-object v1
                const-string v0, "\",\"packageName\":\"morphe_fake\",\"productId\":\""
                invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                move-result-object v1
                invoke-virtual {v1, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                move-result-object v1
                const-string v0, "\",\"purchaseTime\":0,\"purchaseState\":1,\"purchaseToken\":\"morphe-"
                invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                move-result-object v1
                invoke-virtual {v1, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                move-result-object v1
                const-string v0, "\",\"quantity\":1,\"acknowledged\":true}"
                invoke-virtual {v1, v0}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                move-result-object v1
                invoke-virtual {v1}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
                move-result-object v1
                move-object/from16 v0, p0
                $igetTail
                const-string v2, "morphe_fake"
                new-instance v3, Lcom/android/billingclient/api/Purchase;
                invoke-direct {v3, v1, v2}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V
                new-instance v1, Ljava/util/ArrayList;
                invoke-direct {v1}, Ljava/util/ArrayList;-><init>()V
                invoke-virtual {v1, v3}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z
                move-object v3, v1
                invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                move-result-object v1
                const/4 v4, 0x0
                invoke-virtual {v1, v4}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                move-result-object v1
                invoke-virtual {v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                move-result-object v1
                invoke-interface {v0, v1, v3}, Lcom/android/billingclient/api/PurchasesUpdatedListener;->onPurchasesUpdated(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V
                $grantTail
                :morphe_iap_nocb
                $nocbTail
            """.trimIndent()
        }

        patchAll(Fingerprint(name = "launchBillingFlow", custom = { m, _ -> m.returnType.contains("BillingResult") }), "launchBillingFlow", 2) {
            val isStatic = try {
                com.android.tools.smali.dexlib2.AccessFlags.STATIC.isSet(it.accessFlags)
            } catch (_: Exception) { true }
            val field = if (!isStatic) listenerIget(it.definingClass) else null
            if (field != null) {
                val block = buyGrantBlock(field, flowParamsReg(it, isStatic))
                var granted = false
                if (minRegs(it) >= 5) {
                    try { it.addInstructions(0, block); granted = true } catch (_: Exception) {}
                }
                if (!granted) {
                    try { granted = expandSwap(it, block) } catch (_: Exception) {}
                }
                if (granted) {
                    logger.info("FreeIAP buy-time grant: ${it.definingClass}->${it.name}")
                } else try {
                    it.addInstructions(0, okBillingResult)
                } catch (_: Exception) {}
            } else try {
                it.addInstructions(0, okBillingResult)
            } catch (_: Exception) {
                try { it.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0") } catch (_: Exception) {}
            }
        }

        // Unity IL2CPP native bridge (BillingClientImpl.launchBillingFlowCpp):
        // exact-name fingerprint above misses it, so cover by return type.
        patchAll(Fingerprint(name = "launchBillingFlowCpp"), "launchBillingFlowCpp", 2) {
            val isStatic = try {
                com.android.tools.smali.dexlib2.AccessFlags.STATIC.isSet(it.accessFlags)
            } catch (_: Exception) { true }
            val field = if (!isStatic) listenerIget(it.definingClass) else null
            if (field != null && it.returnType.contains("BillingResult")) {
                // Native bridge params are opaque: keep the static fallback id.
                val block = buyGrantBlock(field, null)
                var granted = false
                if (minRegs(it) >= 5) {
                    try { it.addInstructions(0, block); granted = true } catch (_: Exception) {}
                }
                if (!granted) {
                    try { granted = expandSwap(it, block) } catch (_: Exception) {}
                }
                if (granted) {
                    logger.info("FreeIAP buy-time grant: ${it.definingClass}->${it.name}")
                    return@patchAll
                }
                try {
                    it.addInstructions(0, okBillingResult)
                    return@patchAll
                } catch (_: Exception) {}
            }
            when {
                it.returnType.contains("BillingResult") -> try {
                    it.addInstructions(0, okBillingResult)
                } catch (_: Exception) {
                    try { it.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0") } catch (_: Exception) {}
                }
                // Int-returning native bridge (e.g. launchBillingFlowCpp ->
                // I): OK-only for now. A grant block here goes through the
                // clone path (tiny frame) and ART rejects the result
                // (VerifyError on the whole class, seen on Nice Dice 3D),
                // while the Java launchBillingFlow grant above covers the
                // JNI-bridged Unity/Defold purchase path.
                it.returnType == "I" -> it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                it.returnType == "V" -> it.addInstructions(0, "return-void")
            }
        }

        patchAll(Fingerprint(name = "isReady", returnType = "Z", custom = { _, c -> c.type.contains("BillingClient") }), "BillingClient.isReady") {
            it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
        }

        patchAll(Fingerprint(name = "endConnection", custom = { _, c -> c.type.contains("BillingClient") }), "BillingClient.endConnection") {
            it.addInstructions(0, "return-void")
        }

        // startConnection(BillingClientStateListener) -> fire
        // onBillingSetupFinished(OK) on the listener, then FALL THROUGH to
        // the real body (no return): the real connection still runs, so the
        // untouched product catalog below keeps working on devices with
        // Play, while no-Play devices boot on the early OK instead of
        // waiting for setup forever. Any other overload (e.g. the native
        // (J) bridge used by Unity IL2CPP games) is left completely
        // untouched: voiding it strands native setup with no callback and
        // freezes the app on its loading screen.
        // VERIFIER RULE: fall-through injections MUST go through
        // cloneMutable (expandSwap): direct prepending retypes v0/v1 for
        // the original body below and ART rejects the whole class
        // (seen on Nice Dice 3D). If the frame cannot grow, skip rather
        // than inject unsafely.
        patchAll(Fingerprint(name = "startConnection", custom = { _, c -> c.type.contains("BillingClient")         }), "BillingClient.startConnection", 2) {
            if (it.parameterTypes == listOf("Lcom/android/billingclient/api/BillingClientStateListener;") && it.returnType == "V") {
                val block = """
                    invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                    move-result-object v0
                    const/4 v1, 0x0
                    invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                    move-result-object v0
                    invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                    move-result-object v0
                    move-object/from16 v1, p1
                    invoke-interface {v1, v0}, Lcom/android/billingclient/api/BillingClientStateListener;->onBillingSetupFinished(Lcom/android/billingclient/api/BillingResult;)V
                """.trimIndent()
                if (!expandSwap(it, block)) {
                    logger.warning("FreeIAP startConnection left stock (frame too small to clone safely)")
                }
            }
            // else: leave the overload alone (see comment above)
        }

        // onPurchasesUpdated is fired by the buy-time grant in
        // launchBillingFlow above and intentionally left intact elsewhere:
        // the game grants items in its own listener.

        // getBuyIntent -> OK bundle (legacy AIDL v5/v7)
        patchAll(Fingerprint(name = "getBuyIntent", returnType = "Landroid/os/Bundle;"), "getBuyIntent", 3) {
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

        // getPurchases / queryPurchases -> empty list or empty bundle.
        // Listener callbacks get OK + EMPTY list by default (MOD-menu
        // behavior: inventory restores nothing, the grant happens at buy
        // time). The fakeStartupPurchases option restores the old fake
        // PURCHASED delivery for games that only grant at boot.
        for (qn in listOf("getPurchases", "queryPurchases", "queryPurchasesAsync", "queryPurchaseHistory", "queryPurchaseHistoryAsync", "queryPurchasesHistory")) {
            patchAll(Fingerprint(name = qn, custom = { m, c -> c.type.contains("BillingClient") || m.definingClass.contains("billing") || c.type.lowercase().contains("billing") }), qn, 3) {
                val listenerIdx = it.parameterTypes.indexOfFirst { p -> p.contains("PurchasesResponseListener") || p.contains("PurchaseHistoryResponseListener") }
                if (listenerIdx >= 0 && it.returnType == "V") {
                    val isHistory = it.parameterTypes[listenerIdx].contains("History")
                    val listenerReg = "p${listenerIdx + 1}"
                    val iface = if (isHistory) "Lcom/android/billingclient/api/PurchaseHistoryResponseListener;" else "Lcom/android/billingclient/api/PurchasesResponseListener;"
                    val cb = if (isHistory) "onPurchaseHistoryResponse" else "onQueryPurchasesResponse"
                    if (fakeStartupPurchases != true) {
                        it.addInstructions(0, """
                            invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                            move-result-object v0
                            const/4 v1, 0x0
                            invoke-virtual {v0, v1}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                            move-result-object v0
                            invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                            move-result-object v0
                            invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
                            move-result-object v1
                            move-object/from16 v2, $listenerReg
                            invoke-interface {v2, v0, v1}, $iface->$cb(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V
                            return-void
                        """.trimIndent())
                    } else {
                    // v0..v3: expanded into a grown frame via expandSwap so
                    // tiny delegate frames (e.g. 3-reg BillingClientImpl
                    // methods) verify instead of killing their class.
                    val block = if (isHistory) {
                        """
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
                            move-object/from16 v3, $listenerReg
                            invoke-interface {v3, v0, v1}, Lcom/android/billingclient/api/PurchaseHistoryResponseListener;->onPurchaseHistoryResponse(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V
                            return-void
                        """.trimIndent()
                    } else {
                        """
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
                            move-object/from16 v3, $listenerReg
                            invoke-interface {v3, v0, v1}, Lcom/android/billingclient/api/PurchasesResponseListener;->onQueryPurchasesResponse(Lcom/android/billingclient/api/BillingResult;Ljava/util/List;)V
                            return-void
                        """.trimIndent()
                    }
                    if (!expandSwap(it, block) && minRegs(it) >= 4) {
                        it.addInstructions(0, block)
                    }
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

        // querySkuDetailsAsync / queryProductDetailsAsync and their sync
        // List variants are intentionally LEFT STOCK: faking an empty
        // catalog stalls Unity shop init on loading screens (native MOD
        // menus never touch the catalog either). The grant happens at buy
        // time via launchBillingFlow above.

        // ── Synthetic product catalog (Billing 5+ only) ──
        // Some setups never resolve the catalog at all: no working Play
        // backend (e.g. MicroG-only devices) means queryProductDetailsAsync
        // goes nowhere, Unity IAP initializes with zero products, and every
        // tap dies in C# with "unknown product" before our buy-time grant
        // can fire (seen on Nice Dice 3D). Answer the query ourselves with
        // one ProductDetails per requested id, built from the ids the game
        // asked for. All names resolve at patch time (exact names first,
        // signature fallback for obfuscated Billing); the labeled loop
        // lives in an injected catch-free helper (labels + try blocks in
        // the target method don't mix), the call site stays straight-line.
        // VERIFIER RULE (same as the grant block): v0..v3/v5/v6 hold
        // OBJECTS ONLY, v4 holds INTS ONLY, and the helper returns the
        // list (empty when the request is null) instead of branching out.
        try {
            val qppClass = "Lcom/android/billingclient/api/QueryProductDetailsParams;"
            val qpClass = "Lcom/android/billingclient/api/QueryProductDetailsParams\$Product;"
            val catPdClass = "Lcom/android/billingclient/api/ProductDetails;"
            val catListener = "Lcom/android/billingclient/api/ProductDetailsResponseListener;"
            // Candidate request-list accessors on QueryProductDetailsParams.
            // Exact name first; otherwise every no-arg internal-holder
            // getter (obfuscated Billing hides the list behind e.g. zza()
            // returning a zzbt). The helper tries each at runtime behind
            // instance-of guards, so a wrong pick degrades to the next
            // candidate instead of crashing. No patch-time interface proof
            // needed: internal holders may not even load as mutable types.
            val listGetters = try {
                val ms = mutableClassDefByOrNull(qppClass)?.methods ?: emptyList()
                val exact = ms.firstOrNull { it.parameterTypes.isEmpty() && it.name == "getProductList" }
                if (exact != null) {
                    listOf(exact.name to exact.returnType)
                } else {
                    val found = (ms.filter { it.parameterTypes.isEmpty() && it.returnType == "Ljava/util/List;" } +
                        ms.filter { it.parameterTypes.isEmpty() && it.returnType == "Ljava/util/ArrayList;" } +
                        ms.filter {
                            it.parameterTypes.isEmpty() &&
                                it.returnType.startsWith("Lcom/google/android/gms/internal/play_billing/")
                        }).map { it.name to it.returnType }.take(4)
                    found
                }
            } catch (_: Exception) { emptyList() }
            // Product id/type accessors. Exact names first; on obfuscated
            // Billing neither exists, so fall back to dynamic resolution:
            // call the String getters and decide at runtime which value
            // is the type (equals "inapp"/"subs") and which is the id.
            // Never guesses blindly: a wrong identity would grant nothing.
            val qpMethods = try {
                mutableClassDefByOrNull(qpClass)?.methods ?: emptyList()
            } catch (_: Exception) { emptyList() }
            val qpIdExact = qpMethods.firstOrNull { it.parameterTypes.isEmpty() && it.name == "getProductId" }?.name
            val qpTypeExact = qpMethods.firstOrNull { it.parameterTypes.isEmpty() && it.name == "getProductType" }?.name
            val qpStrGetters = qpMethods
                .filter { it.parameterTypes.isEmpty() && it.returnType == "Ljava/lang/String;" }
                .map { it.name }
                .filter { it != "toString" && it != "hashCode" }
                .take(2)
            val hasPdCtor = try {
                mutableClassDefByOrNull(catPdClass)?.methods
                    ?.any { it.name == "<init>" && it.parameterTypes == listOf("Ljava/lang/String;") } == true
            } catch (_: Exception) { false }
            val hasIdSource = qpIdExact != null || qpStrGetters.isNotEmpty()
            if (listGetters.isNotEmpty() && hasIdSource && hasPdCtor) {
                // Emits code leaving product id in v2 and product type in
                // v3 (v2/v3/v4/v6 discipline per the verifier rule above).
                val idTypeLines = if (qpIdExact != null) {
                    val t = if (qpTypeExact != null) {
                        "invoke-virtual {v2}, $qpClass->$qpTypeExact()Ljava/lang/String;\nmove-result-object v3"
                    } else {
                        "const-string v3, \"inapp\""
                    }
                    "$t\ninvoke-virtual {v2}, $qpClass->$qpIdExact()Ljava/lang/String;\nmove-result-object v2"
                } else if (qpStrGetters.size >= 2) {
                    val a = qpStrGetters[0]
                    val b = qpStrGetters[1]
                    """
                    invoke-virtual {v2}, $qpClass->$a()Ljava/lang/String;
                    move-result-object v3
                    if-eqz v3, :morphe_cat_loop
                    invoke-virtual {v2}, $qpClass->$b()Ljava/lang/String;
                    move-result-object v2
                    const-string v6, "inapp"
                    invoke-virtual {v3, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v4
                    if-nez v4, :morphe_cat_havetype
                    const-string v6, "subs"
                    invoke-virtual {v3, v6}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z
                    move-result v4
                    if-nez v4, :morphe_cat_havetype
                    move-object v6, v2
                    move-object v2, v3
                    move-object v3, v6
                    :morphe_cat_havetype
                    """.trimIndent()
                } else {
                    "invoke-virtual {v2}, $qpClass->${qpStrGetters[0]}()Ljava/lang/String;\n" +
                        "move-result-object v2\nconst-string v3, \"inapp\""
                }
                patchAll(Fingerprint(name = "queryProductDetailsAsync", custom = { m, c ->
                    c.type.contains("BillingClient") && m.returnType == "V" &&
                        m.parameterTypes.any { it.contains("QueryProductDetailsParams") } &&
                        m.parameterTypes.any { it.contains("ProductDetailsResponseListener") }
                }), "queryProductDetailsAsync", 3) {
                    val isStatic = try {
                        com.android.tools.smali.dexlib2.AccessFlags.STATIC.isSet(it.accessFlags)
                    } catch (_: Exception) { true }
                    val pOff = if (isStatic) 0 else 1
                    val paramsIdx = it.parameterTypes.indexOfFirst { p -> p.contains("QueryProductDetailsParams") }
                    val listenerIdx = it.parameterTypes.indexOfFirst { p -> p.contains("ProductDetailsResponseListener") }
                    if (paramsIdx < 0 || listenerIdx < 0) return@patchAll
                    val paramsReg = "p${paramsIdx + pOff}"
                    val listenerReg = "p${listenerIdx + pOff}"
                    // Install the loop helper once per owner class.
                    try {
                        val owner = mutableClassDefByOrNull(it.definingClass)
                        if (owner != null && owner.methods.none { m -> m.name == "morpheFakeProductList" }) {
                            val candsSmali = StringBuilder()
                            listGetters.forEachIndexed { i, g ->
                                candsSmali.appendLine("move-object/from16 v1, p0")
                                candsSmali.appendLine("invoke-virtual {v1}, $qppClass->${g.first}()${g.second}")
                                candsSmali.appendLine("move-result-object v1")
                                candsSmali.appendLine("instance-of v4, v1, Ljava/util/List;")
                                candsSmali.appendLine("if-eqz v4, :morphe_cat_next$i")
                                candsSmali.appendLine("check-cast v1, Ljava/util/List;")
                                candsSmali.appendLine("invoke-interface {v1}, Ljava/util/List;->isEmpty()Z")
                                candsSmali.appendLine("move-result v4")
                                candsSmali.appendLine("if-eqz v4, :morphe_cat_fill")
                                candsSmali.appendLine(":morphe_cat_next$i")
                            }
                            // Clone a donor static method instead of building an
                            // ImmutableMethod from scratch: cloned methods
                            // are proven dex-writable in this toolchain.
                            val donor = owner.methods.firstOrNull { m ->
                                try {
                                    com.android.tools.smali.dexlib2.AccessFlags.STATIC.isSet(m.accessFlags) &&
                                        m.implementation != null
                                } catch (_: Exception) { false }
                            } ?: run {
                                logger.warning("FreeIAP synthetic catalog helper skipped: no static donor")
                                return@patchAll
                            }
                            val helper = donor.cloneMutable(
                                name = "morpheFakeProductList",
                                parameters = listOf(
                                    com.android.tools.smali.dexlib2.immutable.ImmutableMethodParameter(
                                        qppClass, null, null,
                                    ),
                                ),
                                returnType = "Ljava/util/List;",
                                additionalRegisters = 8,
                            )
                            helper.implementation?.let { impl ->
                                try { impl.removeInstructions(impl.instructions.size) } catch (_: Exception) {}
                            }
                            owner.methods.add(helper)
                            val dbgText = """
                                new-instance v0, Ljava/util/ArrayList;
                                invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
                                if-eqz p0, :morphe_cat_done
                                $candsSmali goto :morphe_cat_done
                                :morphe_cat_fill
                                invoke-interface {v1}, Ljava/util/List;->iterator()Ljava/util/Iterator;
                                move-result-object v1
                                const/4 v4, 0x0
                                :morphe_cat_loop
                                invoke-interface {v1}, Ljava/util/Iterator;->hasNext()Z
                                move-result v4
                                if-eqz v4, :morphe_cat_done
                                invoke-interface {v1}, Ljava/util/Iterator;->next()Ljava/lang/Object;
                                move-result-object v2
                                instance-of v4, v2, $qpClass
                                if-eqz v4, :morphe_cat_loop
                                check-cast v2, $qpClass
                                $idTypeLines
                                if-eqz v2, :morphe_cat_loop
                                new-instance v5, Ljava/lang/StringBuilder;
                                invoke-direct {v5}, Ljava/lang/StringBuilder;-><init>()V
                                const-string v6, "{\"productId\":\""
                                invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v5
                                invoke-virtual {v5, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v5
                                const-string v6, "\",\"type\":\""
                                invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v5
                                invoke-virtual {v5, v3}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v5
                                const-string v6, "\",\"title\":\""
                                invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v5
                                invoke-virtual {v5, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v5
                                const-string v6, "\",\"name\":\""
                                invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v5
                                invoke-virtual {v5, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v5
                                const-string v6, "\",\"description\":\""
                                invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v5
                                invoke-virtual {v5, v2}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v5
                                const-string v6, "\"}"
                                invoke-virtual {v5, v6}, Ljava/lang/StringBuilder;->append(Ljava/lang/String;)Ljava/lang/StringBuilder;
                                move-result-object v5
                                invoke-virtual {v5}, Ljava/lang/StringBuilder;->toString()Ljava/lang/String;
                                move-result-object v6
                                new-instance v2, $catPdClass
                                invoke-direct {v2, v6}, $catPdClass-><init>(Ljava/lang/String;)V
                                invoke-interface {v0, v2}, Ljava/util/List;->add(Ljava/lang/Object;)Z
                                goto :morphe_cat_loop
                                :morphe_cat_done
                                return-object v0
                                nop
                            """.trimIndent()
                            helper.addInstructionsWithLabels(0, dbgText)
                            logger.info("FreeIAP synthetic catalog helper installed in ${it.definingClass}")
                        }
                    } catch (e: Exception) {
                        logger.warning("FreeIAP synthetic catalog helper skipped: ${e.message}")
                        return@patchAll
                    }
                    val helperRef = "${it.definingClass}->morpheFakeProductList($qppClass)Ljava/util/List;"
                    logger.info("FreeIAP synthetic catalog sources: " + listGetters.joinToString(",") { it.first })
                    // NOTE: the callback goes through java.lang.reflect
                    // (getClass/getMethod/invoke) instead of a direct
                    // interface invoke: morphe's inline lexer rejects the
                    // ProductDetailsResponseListener method ref outright.
                    // Straight-line, no labels: clone-safe. Needs v0..v6.
                    val block = """
                        move-object/from16 v0, $paramsReg
                        invoke-static {v0}, $helperRef
                        move-result-object v1
                        invoke-static {}, Lcom/android/billingclient/api/BillingResult;->newBuilder()Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                        move-result-object v0
                        const/4 v4, 0x0
                        invoke-virtual {v0, v4}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->setResponseCode(I)Lcom/android/billingclient/api/BillingResult${'$'}Builder;
                        move-result-object v0
                        invoke-virtual {v0}, Lcom/android/billingclient/api/BillingResult${'$'}Builder;->build()Lcom/android/billingclient/api/BillingResult;
                        move-result-object v0
                        move-object/from16 v2, $listenerReg
                        invoke-virtual {v2}, Ljava/lang/Object;->getClass()Ljava/lang/Class;
                        move-result-object v3
                        const-string v5, "onProductDetailsResponse"
                        const/4 v4, 0x2
                        new-array v6, v4, [Ljava/lang/Class;
                        const-class v5, Lcom/android/billingclient/api/BillingResult;
                        const/4 v4, 0x0
                        aput-object v6, v4, v5
                        const-class v5, Ljava/util/List;
                        const/4 v4, 0x1
                        aput-object v6, v4, v5
                        const-string v5, "onProductDetailsResponse"
                        invoke-virtual {v3, v5, v6}, Ljava/lang/Class;->getMethod(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;
                        move-result-object v3
                        const/4 v4, 0x2
                        new-array v5, v4, [Ljava/lang/Object;
                        const/4 v4, 0x0
                        aput-object v5, v4, v0
                        const/4 v4, 0x1
                        aput-object v5, v4, v1
                        invoke-virtual {v3, v2, v5}, Ljava/lang/reflect/Method;->invoke(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;
                        move-result-object v3
                        return-void
                    """.trimIndent()
                    var done = false
                    if (minRegs(it) >= 7) {
                        try { it.addInstructions(0, block); done = true } catch (_: Exception) {}
                    }
                    if (!done) {
                        try { done = expandSwap(it, block) } catch (_: Exception) {}
                    }
                    if (done) {
                        logger.info("FreeIAP synthetic catalog: ${it.definingClass}->${it.name}")
                    } else {
                        logger.warning("FreeIAP synthetic catalog NOT applied to ${it.definingClass}->${it.name}")
                    }
                }
            } else {
                logger.info("FreeIAP synthetic catalog skipped (query API shape not found)")
            }
        } catch (e: Exception) {
            logger.warning("FreeIAP synthetic catalog skipped: ${e.message}")
        }

        // getSkuDetails / getProductDetails AIDL
        for (qn in listOf("getSkuDetails", "getProductDetails")) {
            patchAll(Fingerprint(name = qn, returnType = "Landroid/os/Bundle;"), qn, 3) {
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
            patchAll(Fingerprint(name = cn), cn, 3) {
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
                        move-object/from16 v1, p1
                        invoke-virtual {v1}, Lcom/android/billingclient/api/ConsumeParams;->getPurchaseToken()Ljava/lang/String;
                        move-result-object v1
                        move-object/from16 v2, $listenerReg
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

        patchAll(Fingerprint(name = "acknowledgePurchase"), "acknowledgePurchase", 2) {
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
                    move-object/from16 v1, $listenerReg
                    invoke-interface {v1, v0}, Lcom/android/billingclient/api/AcknowledgePurchaseResponseListener;->onAcknowledgePurchaseResponse(Lcom/android/billingclient/api/BillingResult;)V
                    return-void
                """.trimIndent())
            } else when {
                it.returnType.contains("BillingResult") -> it.addInstructions(0, okBillingResult)
                it.returnType == "I" -> it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                else -> it.addInstructions(0, "return-void")
            }
        }

        patchAll(Fingerprint(name = "getBillingConfig"), "getBillingConfig", 2) {
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
        patchAll(Fingerprint(name = "getPriceAmountMicros", returnType = "J"), "getPriceAmountMicros", 2) {
            it.addInstructions(0, "const-wide/16 v0, 0x0\nreturn-wide v0")
        }
        patchAll(Fingerprint(name = "getPriceAmountMicros", custom = { _, c -> c.type.lowercase().contains("offer") }), "Offer.getPriceAmountMicros", 2) {
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

        patchAll(Fingerprint(name = "isFeatureSupported"), "isFeatureSupported", 2) {
            when {
                it.returnType.contains("BillingResult") -> it.addInstructions(0, okBillingResult)
                it.returnType == "I" -> it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                it.returnType == "Z" -> it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            }
        }

        patchAll(Fingerprint(name = "getConnectionState"), "getConnectionState") {
            it.addInstructions(0, "const/4 v0, 0x2\nreturn v0")
        }

        // NOTE: onBillingSetupFinished is intentionally left intact: the game
        // learns billing is ready through its own listener, and startConnection
        // above already fires it with OK. Suppressing it breaks init.

        // ──────────────────────────────────────────────
        // UNITY IAP
        // ──────────────────────────────────────────────

        patchAll(Fingerprint(name = "ProcessPurchase", custom = { m, _ -> m.returnType.contains("PurchaseProcessingResult") }), "ProcessPurchase") {
            it.addInstructions(0, "sget-object v0, Lcom/unity/purchasing/PurchaseProcessingResult;->Complete:Lcom/unity/purchasing/PurchaseProcessingResult;\nreturn-object v0")
        }
        patchAll(Fingerprint(name = "OnPurchaseFailed"), "OnPurchaseFailed") { it.addInstructions(0, "return-void") }
        patchAll(Fingerprint(name = "OnSetupFailed"), "OnSetupFailed") { it.addInstructions(0, "return-void") }
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
        // REVENUECAT (server receipt validation cannot be faked;
        // these make the app run its bought-path locally instead)
        // ──────────────────────────────────────────────

        val rcPurchases = "Lcom/revenuecat/purchases/Purchases;"
        val rcPurchaseCb = "Lcom/revenuecat/purchases/interfaces/PurchaseCallback;"
        val rcInfo = "Lcom/revenuecat/purchases/EntitlementInfo;"
        val rcInfos = "Lcom/revenuecat/purchases/EntitlementInfos;"
        val rcTx = "Lcom/revenuecat/purchases/models/StoreTransaction;"
        val rcCust = "Lcom/revenuecat/purchases/CustomerInfo;"
        val fakeId = "morphe_fake"

        // 1b) Same fake via sun.misc.Unsafe allocation (no constructors, no
        // range invokes): allocate + populate fields resolved at patch time.
        fun rcField(className: String, name: String, type: String? = null): String? {
            val cls = try { mutableClassDefByOrNull(className) } catch (_: Exception) { return null } ?: return null
            val f = cls.fields.firstOrNull { it.name == name && (type == null || it.type == type) } ?: return null
            return "$className->${f.name}:${f.type}"
        }
        fun rcFirstFieldOfType(className: String, type: String): String? {
            val cls = try { mutableClassDefByOrNull(className) } catch (_: Exception) { return null } ?: return null
            val f = cls.fields.firstOrNull { it.type == type } ?: return null
            return "$className->${f.name}:${f.type}"
        }
        val rcInfoIdF = rcField(rcInfo, "identifier", "Ljava/lang/String;")
            ?: rcFirstFieldOfType(rcInfo, "Ljava/lang/String;")
        val rcInfoActiveF = rcField(rcInfo, "isActive", "Z")
            ?: rcFirstFieldOfType(rcInfo, "Z")
        val rcInfosCtor1 = try {
            mutableClassDefByOrNull(rcInfos)?.methods
                ?.firstOrNull { it.name == "<init>" && it.parameterTypes == listOf("Ljava/util/Map;") }
        } catch (_: Exception) { null }
        val rcTxOrderF = rcField(rcTx, "orderId", "Ljava/lang/String;")
        val rcTxTokenF = rcField(rcTx, "purchaseToken", "Ljava/lang/String;")
        val rcCustInfosF = rcFirstFieldOfType(rcCust, rcInfos)
        if (rcInfoIdF != null && rcInfoActiveF != null && rcInfosCtor1 != null && rcCustInfosF != null) {
            // fixed regs v0-v9 (4-bit-safe throughout, no range, no clone-window math beyond +12)
            val uCb = 0
            val uId = 1
            val uInfo = 2
            val uMap = 3
            val uInfos = 4
            val uTx = 5
            val uCust = 6
            val uUnsafe = 7
            val uField = 8
            val uTmp = 9
            for (pn in listOf("purchase", "purchasePackage", "purchaseProduct")) {
                patchAll(Fingerprint(name = pn, definingClass = rcPurchases, returnType = "V",
                    custom = { m, _ -> m.parameterTypes.lastOrNull() == rcPurchaseCb }), "RC.$pn-unsafe") { method ->
                    try {
                        val cbIdx = method.parameterTypes.size
                        val owner = try {
                            Fingerprint(name = pn, definingClass = rcPurchases, returnType = "V",
                                custom = { m, _ -> m.parameterTypes.lastOrNull() == rcPurchaseCb }).classDefOrNull
                        } catch (_: Exception) { null } ?: return@patchAll
                        val cloned = method.cloneMutable(additionalRegisters = 12)
                        val target = owner.methods.firstOrNull {
                            it.name == method.name && it.parameterTypes == method.parameterTypes && it.returnType == method.returnType
                        } ?: return@patchAll
                        val sb = StringBuilder()
                        fun emit(s: String) {
                            sb.append(s).append('\n')
                        }
                        emit("move-object/from16 v$uCb, p$cbIdx")
                        emit("const-string v10, \"MorpheRC\"")
                        emit("const-string v11, \"RC $pn buy tapped\"")
                        emit("invoke-static {v10, v11}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I")
                        emit("const-string v$uId, \"$fakeId\"")
                        // Unsafe handle
                        emit("const-string v$uTmp, \"theUnsafe\"")
                        emit("const-class v$uUnsafe, Lsun/misc/Unsafe;")
                        emit("invoke-virtual {v$uUnsafe, v$uTmp}, Ljava/lang/Class;->getDeclaredField(Ljava/lang/String;)Ljava/lang/reflect/Field;")
                        emit("move-result-object v$uField")
                        emit("const/4 v$uTmp, 0x1")
                        emit("invoke-virtual {v$uField, v$uTmp}, Ljava/lang/reflect/Field;->setAccessible(Z)V")
                        emit("const/4 v$uTmp, 0x0")
                        emit("invoke-virtual {v$uField, v$uTmp}, Ljava/lang/reflect/Field;->get(Ljava/lang/Object;)Ljava/lang/Object;")
                        emit("move-result-object v$uUnsafe")
                        emit("check-cast v$uUnsafe, Lsun/misc/Unsafe;")
                        // EntitlementInfo + active flag + id
                        emit("const-class v$uTmp, $rcInfo")
                        emit("invoke-virtual {v$uUnsafe, v$uTmp}, Lsun/misc/Unsafe;->allocateInstance(Ljava/lang/Class;)Ljava/lang/Object;")
                        emit("move-result-object v$uInfo")
                        emit("check-cast v$uInfo, $rcInfo")
                        emit("const/4 v$uTmp, 0x1")
                        emit("iput-boolean v$uTmp, v$uInfo, $rcInfoActiveF")
                        emit("iput-object v$uId, v$uInfo, $rcInfoIdF")
                        // EntitlementInfos via real 1-arg ctor over singleton map
                        emit("invoke-static {v$uId, v$uInfo}, Ljava/util/Collections;->singletonMap(Ljava/lang/Object;Ljava/lang/Object;)Ljava/util/Map;")
                        emit("move-result-object v$uMap")
                        emit("new-instance v$uInfos, $rcInfos")
                        emit("invoke-direct {v$uInfos, v$uMap}, $rcInfos-><init>(Ljava/util/Map;)V")
                        // StoreTransaction allocated, best-effort id fields
                        emit("const-class v$uTmp, $rcTx")
                        emit("invoke-virtual {v$uUnsafe, v$uTmp}, Lsun/misc/Unsafe;->allocateInstance(Ljava/lang/Class;)Ljava/lang/Object;")
                        emit("move-result-object v$uTx")
                        emit("check-cast v$uTx, $rcTx")
                        if (rcTxOrderF != null) emit("iput-object v$uId, v$uTx, $rcTxOrderF")
                        if (rcTxTokenF != null) emit("iput-object v$uId, v$uTx, $rcTxTokenF")
                        // CustomerInfo allocated + infos field
                        emit("const-class v$uTmp, $rcCust")
                        emit("invoke-virtual {v$uUnsafe, v$uTmp}, Lsun/misc/Unsafe;->allocateInstance(Ljava/lang/Class;)Ljava/lang/Object;")
                        emit("move-result-object v$uCust")
                        emit("check-cast v$uCust, $rcCust")
                        emit("iput-object v$uInfos, v$uCust, $rcCustInfosF")
                        emit("invoke-interface {v$uCb, v$uTx, v$uCust}, $rcPurchaseCb->onCompleted($rcTx$rcCust)V")
                        emit("return-void")
                        try {
                            owner.methods.remove(target)
                        } catch (_: Exception) {}
                        cloned.addInstructions(0, sb.toString().trimIndent())
                        owner.methods.add(cloned)
                        patched++
                        patchedMethods.add("RC.$pn-unsafe")
                        logger.info("Free In-app Purchases: faked RevenueCat $pn success callback (unsafe)")
                    } catch (e: Exception) {
                        logger.warning("Free In-app Purchases: RC.$pn unsafe fake skipped: ${e.message}")
                    }
                }
            }
        } else {
            logger.warning("Free In-app Purchases: RevenueCat unsafe fake skipped (fields not found)")
        }

        // 2) RevenueCat BillingWrapper.onPurchasesUpdated -> append a fake
        // PURCHASED Google purchase to a list copy, rebind the param, fall through.
        patchAll(Fingerprint(name = "onPurchasesUpdated",
            definingClass = "Lcom/revenuecat/purchases/google/BillingWrapper;",
            returnType = "V",
            custom = { m, _ -> m.parameterTypes.size == 2 && m.parameterTypes[1] == "Ljava/util/List;" }),
            "RC.onPurchasesUpdated") { method ->
            try {
                val origCount = method.implementation!!.registerCount
                // High regs only: low regs are Undefined at entry (reading them
                // fails verification), and 35c needs regs <= 15. Temps must also
                // stay BELOW the param slots at the top of the frame.
                if (origCount > 13) {
                    logger.warning("Free In-app Purchases: RC.onPurchasesUpdated fake skipped (frame too large)")
                    return@patchAll
                }
                val vH = origCount
                val owner = try {
                    Fingerprint(name = "onPurchasesUpdated",
                        definingClass = "Lcom/revenuecat/purchases/google/BillingWrapper;",
                        returnType = "V",
                        custom = { m, _ -> m.parameterTypes.size == 2 && m.parameterTypes[1] == "Ljava/util/List;" }).classDefOrNull
                } catch (_: Exception) { null } ?: return@patchAll
                // +8: temps (3) must end up strictly below the param slots.
                val cloned = method.cloneMutable(additionalRegisters = 8)
                val target = owner.methods.firstOrNull {
                    it.name == method.name && it.parameterTypes == method.parameterTypes && it.returnType == method.returnType
                } ?: return@patchAll
                val sb = StringBuilder()
                fun emit(s: String) {
                    sb.append(s).append('\n')
                }
                emit("const-string v$vH, \"MorpheRC\"")
                emit("const-string v${vH + 1}, \"RC purchasesUpdated\"")
                emit("invoke-static {v$vH, v${vH + 1}}, Landroid/util/Log;->d(Ljava/lang/String;Ljava/lang/String;)I")
                emit("const-string v$vH, \"{\\\"orderId\\\":\\\"morphe_fake\\\",\\\"packageName\\\":\\\"morphe_fake\\\",\\\"productId\\\":\\\"morphe_fake\\\",\\\"purchaseTime\\\":0,\\\"purchaseState\\\":1,\\\"purchaseToken\\\":\\\"morphe_fake\\\",\\\"quantity\\\":1,\\\"acknowledged\\\":true}\"")
                emit("const-string v${vH + 1}, \"morphe_fake\"")
                emit("new-instance v${vH + 2}, Lcom/android/billingclient/api/Purchase;")
                emit("invoke-direct {v${vH + 2}, v$vH, v${vH + 1}}, Lcom/android/billingclient/api/Purchase;-><init>(Ljava/lang/String;Ljava/lang/String;)V")
                emit("move-object/from16 v$vH, p2")
                emit("new-instance v${vH + 1}, Ljava/util/ArrayList;")
                emit("invoke-direct {v${vH + 1}, v$vH}, Ljava/util/ArrayList;-><init>(Ljava/util/Collection;)V")
                emit("invoke-virtual {v${vH + 1}, v${vH + 2}}, Ljava/util/ArrayList;->add(Ljava/lang/Object;)Z")
                emit("move-object/from16 p2, v${vH + 1}")
                try {
                    owner.methods.remove(target)
                } catch (_: Exception) {}
                cloned.addInstructions(0, sb.toString().trimIndent())
                owner.methods.add(cloned)
                patched++
                patchedMethods.add("RC.onPurchasesUpdated")
                logger.info("Free In-app Purchases: faked purchase into RevenueCat BillingWrapper")
            } catch (e: Exception) {
                logger.warning("Free In-app Purchases: RC.onPurchasesUpdated fake skipped: ${e.message}")
            }
        }

        // 3) App-side RevenueCat error callbacks with PurchasesError -> suppress,
        // so failed server validation cannot pop error UI over the unlock.
        patchAll(Fingerprint(name = "onError", returnType = "V",
            custom = { m, c -> !c.type.contains("revenuecat") && m.parameterTypes.any { it.contains("PurchasesError") } }),
            "RC.onError") {
            it.addInstructions(0, "return-void")
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
