package patches.universal.unlock

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.logging.Logger

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks premium features, subscriptions, and ownership checks. Makes isPremium/isOwned/isVip/hasReceipt return true, and forces SharedPreferences/DataStore purchase keys via getBoolean/getInt/contains to true.",
    default = false,
) {
    val extraKeys by stringOption(
        title = "Extra keys",
        default = "",
        key = "premiumCustomKeys",
        description = "Comma-separated extra SharedPreferences/DataStore keys to spoof (e.g. my_premium,my_pro). Leave empty for default list.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        val patchedMethods = mutableSetOf<String>()
        val extraSet = (extraKeys ?: "").split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

        fun isPremiumKey(lower: String): Boolean {
            if (extraSet.any { it.isNotEmpty() && lower == it }) return true
            val keys = listOf(
                "purchased", "is_purchased", "has_receipt", "hasreceipt", "bought",
                "premium", "is_premium", "ispremium", "premium_unlocked", "premium_status", "premium_expiry",
                "vip", "is_vip", "vip_status", "vip_level", "vip_expiry",
                "no_ads", "noads", "ads_removed", "adsremoved", "ad_free", "adfree", "remove_ads", "removeads",
                "full_version", "fullversion", "unlocked",
                "subscribed", "is_subscribed", "subscription_active", "subscription_expires", "has_subscription", "has_active_purchase",
                "lifetime", "is_lifetime", "annual", "monthly", "trial",
                "entitlement", "entitlements", "is_entitled", "has_entitlement",
                "paid", "is_paid", "member", "pro_version", "is_pro", "pro_member",
                "subscription_expiry", "premium_expiry", "subscription_expire_date", "premium_expire_date"
            )
            for (k in keys) {
                if (lower == k) return true
                if (lower.startsWith(k + "_")) return true
                if (lower.endsWith("_" + k)) return true
                if (lower.contains("_" + k + "_")) return true
            }
            if (lower == "pro" || lower == "vip" || lower == "pro_version" || lower == "is_pro" || lower == "is_vip") return true
            if (lower.contains("_pro_") || lower.endsWith("_pro") || lower.startsWith("pro_")) {
                if (lower.contains("provider") || lower.contains("product") || lower.contains("process") || lower.contains("progress") || lower.contains("project") || lower.contains("proceed")) return false
                return true
            }
            return false
        }

        fun patchAll(fp: Fingerprint, label: String, injector: (app.morphe.patcher.util.proxy.mutableTypes.MutableMethod) -> Unit) {
            try {
                val matches: List<app.morphe.patcher.Match> = try { with(this@execute) { fp.matchAll() } } catch (_: Exception) { emptyList() }
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
            val single = try { fp.methodOrNull } catch (_: Exception) { null }
            if (single?.implementation != null) {
                try {
                    injector(single)
                    patched++
                    patchedMethods.add(label)
                } catch (_: Exception) {}
            }
        }

        // ──────────────────────────────────────────────
        // PURCHASE STATE / OWNERSHIP CHECKS (Z -> true, scoped)
        // ──────────────────────────────────────────────

        for (checkName in listOf(
            "isPurchased", "isOwned", "isPremium", "hasPremium",
            "isSubscribed", "hasSubscription", "isVip", "hasVip",
            "isBought", "hasBought", "wasPurchased", "hasPurchased",
            "isPro", "hasPro", "isFullVersion", "hasFullVersion",
            "isUnlocked", "hasUnlocked", "isActive", "hasActive",
            "isLifetime", "hasLifetime", "isAnnual", "hasAnnual",
            "hasEntitlement", "isEntitled", "checkPremium", "verifyPremium",
            "isPremiumUser", "hasAdFree", "isPaidUser", "checkVip",
            "hasSubscriptionActive", "hasActivePurchase", "isProMember", "isVipUser",
        )) {
            val isGenericActive = checkName == "isActive" || checkName == "hasActive" || checkName == "isPro" || checkName == "hasPro"
            patchAll(
                Fingerprint(
                    name = checkName,
                    returnType = "Z",
                    custom = if (isGenericActive) { _, c ->
                        val t = c.type.lowercase()
                        !t.contains("okhttp") && !t.contains("ssl") && !t.contains("network") && (t.contains("premium") || t.contains("purchase") || t.contains("billing") || t.contains("subscription") || t.contains("user") || t.contains("entitle") || t.contains("vip") || t.contains("pro"))
                    } else null
                ), checkName
            ) { it.addInstructions(0, "const/4 v0, 0x1\nreturn v0") }
        }

        // Generic fallback for obfuscated/camelCase premium checks (e.g., hasPremiumAccess) — no hardcode, catches any Z method containing premium/pro/entitle in name where class is premium/billing/subscription
        classDefForEach { classDef ->
            val typeLower = classDef.type.lowercase()
            // skip obvious non-premium SDKs
            if (typeLower.contains("okhttp") || typeLower.contains("ssl") || typeLower.contains("network") || typeLower.contains("glide") || typeLower.contains("coil")) return@classDefForEach
            val isPremiumClass = typeLower.contains("premium") || typeLower.contains("billing") || typeLower.contains("purchase") || typeLower.contains("subscription") || typeLower.contains("entitle") || typeLower.contains("pro") && !typeLower.contains("provider") && !typeLower.contains("product") && !typeLower.contains("progress")
            // also consider method name itself
            val mutableClass = try { mutableClassDefBy(classDef) } catch (_: Exception) { return@classDefForEach }
            for (method in mutableClass.methods) {
                if (method.returnType != "Z") continue
                val n = method.name.lowercase()
                if (n.length < 3 || n.length > 40) continue
                val isPremiumName = n.contains("premium") || n.contains("haspremium") || n.contains("ispremium") || n.contains("entitle") || n.contains("haspro") || n.contains("ispro") || n.contains("hassubscription") || n.contains("issubscribed")
                if (!isPremiumName && !isPremiumClass) continue
                if (n.contains("provider") || n.contains("product") || n.contains("progress") || n.contains("proceed") || n.contains("providerenabled") || n.contains("probableprime")) continue
                // also avoid isProviderEnabled false positive
                if (n == "ispro" || n == "haspro" || n == "isprouser" || n == "hasprouser" || n.contains("premium")) {
                    // patch only if not already patched via patchAll (avoid double)
                    try {
                        if (method.implementation == null) continue
                        method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                        patched++
                        patchedMethods.add("Generic:${method.name}")
                    } catch (_: Exception) {}
                }
            }
        }

        // isExpired / isCancelled -> false (scoped to premium/subscription)
        for (negName in listOf("isExpired", "isCancelled", "isTrialExpired")) {
            patchAll(Fingerprint(name = negName, returnType = "Z", custom = { _, c ->
                val t = c.type.lowercase()
                t.contains("premium") || t.contains("subscription") || t.contains("entitle") || t.contains("vip") || t.contains("billing") || t.contains("purchase")
            }), negName) {
                it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            }
        }

        // I -> 1 (premium level/state)
        for (intName in listOf("getPremiumState", "getVipLevel", "getSubscriptionStatus", "getProState", "getVipStatus", "getUserType", "getPremiumStatusInt", "getEntitlementState")) {
            patchAll(Fingerprint(name = intName, returnType = "I"), intName) {
                it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            }
        }

        // J expiry -> far future 2099-01-01 (scoped)
        for (longName in listOf("getExpiryTime", "getExpireDate", "getSubscriptionExpiry", "getPremiumExpiry", "getVipExpiry", "getEntitlementExpiry")) {
            patchAll(Fingerprint(name = longName, returnType = "J", custom = { _, c ->
                val t = c.type.lowercase()
                t.contains("premium") || t.contains("subscription") || t.contains("entitle") || t.contains("vip") || t.contains("billing") || t.contains("purchase") || t.contains("pro")
            }), longName) {
                it.addInstructions(0, "const-wide v0, 0x17d2d0c0000L\nreturn-wide v0")
            }
        }

        // List entitlements -> singleton premium
        for (listName in listOf("getEntitlements", "getActivePurchases", "getActiveEntitlements")) {
            patchAll(Fingerprint(name = listName, custom = { m, _ -> m.returnType.contains("List") || m.returnType.contains("Collection") }), listName) {
                it.addInstructions(0, "const-string v0, \"premium\"\ninvoke-static {v0}, Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;\nmove-result-object v0\nreturn-object v0")
            }
        }

        // String premium status -> "premium"
        for (strName in listOf("getPremiumStatus", "getVipStatus", "getSubscriptionStatus", "getUserTypeString")) {
            patchAll(Fingerprint(name = strName, returnType = "Ljava/lang/String;"), strName) {
                it.addInstructions(0, "const-string v0, \"premium\"\nreturn-object v0")
            }
        }

        // ──────────────────────────────────────────────
        // RECEIPT CHECKS
        // ──────────────────────────────────────────────

        for (receiptName in listOf("hasReceipt", "getHasReceipt", "hasValidReceipt", "isReceiptValid", "hasActiveReceipt", "getReceipt")) {
            patchAll(Fingerprint(name = receiptName, returnType = "Z"), receiptName) {
                it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            }
        }
        for (receiptName in listOf("hasReceipt", "getHasReceipt", "getReceipt")) {
            patchAll(Fingerprint(name = receiptName, returnType = "Ljava/lang/String;"), "$receiptName:String") {
                it.addInstructions(0, "const-string v0, \"fake_receipt_data\"\nreturn-object v0")
            }
        }

        // ──────────────────────────────────────────────
        // classDefForEach: SharedPreferences / DataStore / MMKV bypass
        // ──────────────────────────────────────────────

        classDefForEach { classDef ->
            var foundPrefsCall = false
            for (method in classDef.methods) {
                val impl = method.implementation ?: continue
                for (insn in impl.instructions) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    val name = ref.name
                    val def = ref.definingClass
                    val isPrefs = def.contains("PlayerPrefs") || def == "Landroid/content/SharedPreferences;" || def == "Landroid/content/SharedPreferences\$Editor;" || def.contains("DataStore") || def.contains("MMKV") || def.contains("EncryptedSharedPreferences") || def.contains("Preferences")
                    if (isPrefs && (name == "GetInt" || name == "getInt" || name == "GetString" || name == "getString" || name == "GetBoolean" || name == "getBoolean" || name == "GetLong" || name == "getLong" || name == "HasKey" || name == "contains" || name == "getValue" || name == "getData" || name == "get")) {
                        foundPrefsCall = true; break
                    }
                }
                if (foundPrefsCall) break
            }
            if (!foundPrefsCall) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    val mname = ref.name
                    val def = ref.definingClass
                    val isPlayerPrefs = def.contains("PlayerPrefs")
                    val isSharedPrefs = def == "Landroid/content/SharedPreferences;"
                    val isDataStore = def.contains("DataStore") || def.contains("MMKV") || def.contains("EncryptedSharedPreferences") || def.contains("Preferences")
                    val isGetBoolean = (mname == "GetBoolean" || mname == "getBoolean" || mname == "getValue") && ref.returnType == "Z"
                    val isGetInt = (mname == "GetInt" || mname == "getInt" || mname == "getValue") && ref.returnType == "I" && ref.parameterTypes.isNotEmpty() && ref.parameterTypes[0] == "Ljava/lang/String;"
                    val isGetLong = (mname == "GetLong" || mname == "getLong") && ref.returnType == "J"
                    val isGetString = (mname == "GetString" || mname == "getString" || mname == "getValue") && ref.returnType == "Ljava/lang/String;"
                    val isHasKey = (mname == "HasKey" || mname == "contains" || mname == "containsKey" || mname == "hasKey") && ref.returnType == "Z"
                    val isDataStoreGet = (mname == "get" && isDataStore && ref.parameterTypes.isNotEmpty() && ref.parameterTypes[0].contains("Key"))

                    if (!isPlayerPrefs && !isSharedPrefs && !isDataStore) continue
                    if (!isGetBoolean && !isGetInt && !isGetLong && !isGetString && !isHasKey && !isDataStoreGet) continue

                    // generic register extraction
                    val keyRegister = when (insn) {
                        is com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c -> when (insn.registerCount) {
                            1 -> insn.registerC
                            2 -> insn.registerC
                            else -> insn.registerD
                        }
                        is com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc -> insn.startRegister + 1
                        else -> continue
                    }

                    var keyValue: String? = null
                    // strict same-register backtrace 6
                    for (j in index - 1 downTo maxOf(0, index - 6)) {
                        val prev = instructions[j]
                        if (prev.opcode != Opcode.CONST_STRING) continue
                        val reg = (prev as? com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction)?.registerA ?: continue
                        if (reg != keyRegister) continue
                        keyValue = ((prev as? ReferenceInstruction)?.reference as? StringReference)?.string
                        break
                    }
                    // fallback any premium-ish const-string in last 8
                    if (keyValue == null) {
                        for (j in index - 1 downTo maxOf(0, index - 8)) {
                            val prev = instructions[j]
                            if (prev.opcode != Opcode.CONST_STRING) continue
                            val s = ((prev as? ReferenceInstruction)?.reference as? StringReference)?.string ?: continue
                            if (isPremiumKey(s.lowercase())) { keyValue = s; break }
                        }
                    }
                    if (keyValue == null || !isPremiumKey(keyValue!!.lowercase())) continue

                    val next = instructions.getOrNull(index + 1) ?: continue
                    when {
                        isDataStoreGet && next.opcode == Opcode.MOVE_RESULT_OBJECT -> {
                            val r = (next as com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction).registerA
                            // DataStore Preferences.get(Key) returns Object (boxed Boolean for premium_purchased)
                            // Return Boolean.TRUE via valueOf
                            if (r <= 0xff) {
                                method.addInstructions(index, "const/4 v$r, 0x1\ninvoke-static {v$r}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;\nmove-result-object v$r")
                                // original invoke now at index+3, move at index+4
                                method.replaceInstruction(index + 3, "nop")
                                method.replaceInstruction(index + 4, "nop")
                            } else {
                                method.addInstructions(index, "const/4 v0, 0x1\ninvoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;\nmove-result-object v0\nmove-object v$r, v0")
                                method.replaceInstruction(index + 4, "nop")
                                method.replaceInstruction(index + 5, "nop")
                            }
                            patchedMethods.add("Prefs:${keyValue}:DataStore.get")
                            patched++
                        }
                        isGetBoolean && next.opcode == Opcode.MOVE_RESULT -> {
                            val r = (next as com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction).registerA
                            if (r <= 0xf) {
                                method.replaceInstruction(index, "const/4 v$r, 0x1")
                                method.replaceInstruction(index + 1, "nop")
                            } else if (r <= 0xff) {
                                method.replaceInstruction(index, "const/16 v$r, 0x1")
                                method.replaceInstruction(index + 1, "nop")
                            } else {
                                method.replaceInstruction(index, "const/4 v0, 0x1")
                                method.replaceInstruction(index + 1, "move v$r, v0")
                            }
                            patchedMethods.add("Prefs:${keyValue}:getBoolean")
                            patched++
                        }
                        isHasKey && next.opcode == Opcode.MOVE_RESULT -> {
                            val r = (next as com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction).registerA
                            if (r <= 0xf) {
                                method.replaceInstruction(index, "const/4 v$r, 0x1")
                                method.replaceInstruction(index + 1, "nop")
                            } else if (r <= 0xff) {
                                method.replaceInstruction(index, "const/16 v$r, 0x1")
                                method.replaceInstruction(index + 1, "nop")
                            } else {
                                method.replaceInstruction(index, "const/4 v0, 0x1")
                                method.replaceInstruction(index + 1, "move v$r, v0")
                            }
                            patchedMethods.add("Prefs:${keyValue}:contains")
                            patched++
                        }
                        isGetInt && next.opcode == Opcode.MOVE_RESULT -> {
                            val r = (next as com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction).registerA
                            if (r <= 0xf) {
                                method.replaceInstruction(index, "const/4 v$r, 0x1")
                                method.replaceInstruction(index + 1, "nop")
                            } else if (r <= 0xff) {
                                method.replaceInstruction(index, "const/16 v$r, 0x1")
                                method.replaceInstruction(index + 1, "nop")
                            } else {
                                method.replaceInstruction(index, "const/4 v0, 0x1")
                                method.replaceInstruction(index + 1, "move v$r, v0")
                            }
                            patchedMethods.add("Prefs:${keyValue}:getInt")
                            patched++
                        }
                        isGetLong && next.opcode == Opcode.MOVE_RESULT_WIDE -> {
                            val r = (next as com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction).registerA
                            if (r <= 0xff) {
                                method.replaceInstruction(index, "const-wide/16 v$r, 0x1")
                                method.replaceInstruction(index + 1, "nop")
                            } else {
                                method.replaceInstruction(index, "const-wide/16 v0, 0x1")
                                method.replaceInstruction(index + 1, "move-wide v$r, v0")
                            }
                            patchedMethods.add("Prefs:${keyValue}:getLong")
                            patched++
                        }
                        isGetString && next.opcode == Opcode.MOVE_RESULT_OBJECT -> {
                            val r = (next as com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction).registerA
                            if (r <= 0xff) {
                                method.replaceInstruction(index, "const-string v$r, \"premium\"")
                                method.replaceInstruction(index + 1, "nop")
                            } else if (r <= 0xffff) {
                                method.replaceInstruction(index, "const-string/jumbo v$r, \"premium\"")
                                method.replaceInstruction(index + 1, "nop")
                            } else {
                                method.replaceInstruction(index, "const-string v0, \"premium\"")
                                method.replaceInstruction(index + 1, "move-object v$r, v0")
                            }
                            patchedMethods.add("Prefs:${keyValue}:getString")
                            patched++
                        }
                    }
                }
            }
        }

        // ──────────────────────────────────────────────
        // REPORT
        // ──────────────────────────────────────────────

        if (patched > 0) {
            logger.info("Unlock Premium: patched $patched check(s)")
            logger.info("Patched methods: ${patchedMethods.sorted().joinToString(", ")}")
        } else {
            logger.warning("No premium/ownership checks found. No changes applied.")
        }
    }
}
