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
    description = "Unlock premium features and remove paywalls.",
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

        // Optimized isPremiumKey: single contains check, no per-key underscore branching
        val premiumSubstrings = listOf(
            "purchased", "has_receipt", "hasreceipt", "bought",
            "premium", "is_premium", "ispremium", "premium_unlocked", "premium_status", "premium_expiry", "premiumaccess", "haspremiumaccess",
            "vip", "is_vip", "vip_status", "vip_level", "vip_expiry",
            "no_ads", "noads", "ads_removed", "adsremoved", "ad_free", "adfree", "remove_ads", "removeads",
            "full_version", "fullversion", "unlocked",
            "subscribed", "is_subscribed", "subscription_active", "subscription_expires", "has_subscription", "has_active_purchase",
            "lifetime", "is_lifetime", "annual", "monthly", "trial",
            "entitlement", "entitlements", "is_entitled", "has_entitlement",
            "paid", "is_paid", "member", "pro_version", "is_pro", "pro_member",
            "subscription_expiry", "premium_expiry", "key_subs", "key_sub", "subs"
        )
        fun isPremiumKey(lower: String): Boolean {
            if (extraSet.any { it.isNotEmpty() && lower == it }) return true
            for (k in premiumSubstrings) {
                if (lower.contains(k)) {
                    // guard generic "pro" inside provider/product
                    if (k == "pro_version" || k == "is_pro") return true
                    if (lower.contains("provider") || lower.contains("product") && k == "pro") continue
                    return true
                }
            }
            // standalone vip/pro
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
        // 1) Fingerprint premium checks (cheap, indexed)
        // ──────────────────────────────────────────────

        for (checkName in listOf(
            "isPurchased", "isOwned", "isPremium", "hasPremium", "hasPremiumAccess", "isPremiumAccess",
            "isSubscribed", "hasSubscription", "isVip", "hasVip",
            "isBought", "hasBought", "wasPurchased", "hasPurchased",
            "isPro", "hasPro", "isProUser", "hasProUser", "isFullVersion", "hasFullVersion",
            "isUnlocked", "hasUnlocked", "isActive", "hasActive",
            "isLifetime", "hasLifetime", "isAnnual", "hasAnnual",
            "hasEntitlement", "isEntitled", "checkPremium", "verifyPremium",
            "isPremiumUser", "hasAdFree", "isPaidUser", "checkVip",
            "hasSubscriptionActive", "hasActivePurchase", "isProMember", "isVipUser", "hasPremiumAccessChanged",
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

        for (negName in listOf("isExpired", "isCancelled", "isTrialExpired")) {
            patchAll(Fingerprint(name = negName, returnType = "Z", custom = { _, c ->
                val t = c.type.lowercase()
                t.contains("premium") || t.contains("subscription") || t.contains("entitle") || t.contains("vip") || t.contains("billing") || t.contains("purchase")
            }), negName) {
                it.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
            }
        }

        for (intName in listOf("getPremiumState", "getVipLevel", "getSubscriptionStatus", "getProState", "getVipStatus", "getUserType", "getPremiumStatusInt", "getEntitlementState")) {
            patchAll(Fingerprint(name = intName, returnType = "I"), intName) {
                it.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
            }
        }

        for (longName in listOf("getExpiryTime", "getExpireDate", "getSubscriptionExpiry", "getPremiumExpiry", "getVipExpiry", "getEntitlementExpiry")) {
            patchAll(Fingerprint(name = longName, returnType = "J", custom = { _, c ->
                val t = c.type.lowercase()
                t.contains("premium") || t.contains("subscription") || t.contains("entitle") || t.contains("vip") || t.contains("billing") || t.contains("purchase") || t.contains("pro")
            }), longName) {
                it.addInstructions(0, "const-wide v0, 0x17d2d0c0000L\nreturn-wide v0")
            }
        }

        for (listName in listOf("getEntitlements", "getActivePurchases", "getActiveEntitlements")) {
            patchAll(Fingerprint(name = listName, custom = { m, _ -> m.returnType.contains("List") || m.returnType.contains("Collection") }), listName) {
                it.addInstructions(0, "const-string v0, \"premium\"\ninvoke-static {v0}, Ljava/util/Collections;->singletonList(Ljava/lang/Object;)Ljava/util/List;\nmove-result-object v0\nreturn-object v0")
            }
        }

        for (strName in listOf("getPremiumStatus", "getVipStatus", "getSubscriptionStatus", "getUserTypeString")) {
            patchAll(Fingerprint(name = strName, returnType = "Ljava/lang/String;"), strName) {
                it.addInstructions(0, "const-string v0, \"premium\"\nreturn-object v0")
            }
        }

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
        // 2) Single-pass classDefForEach: Prefs + generic premium fallback
        // Merged from 3 passes -> 1 pass to save heap (was 6 scans via helpers)
        // ──────────────────────────────────────────────

        classDefForEach { classDef ->
            val typeLower = classDef.type.lowercase()
            // skip obvious non-premium SDKs early to avoid mutableClassDefBy
            if (typeLower.contains("okhttp") || typeLower.contains("ssl") || typeLower.contains("network") || typeLower.contains("glide") || typeLower.contains("coil") && !typeLower.contains("premium") && !typeLower.contains("purchase")) {
                // still need to check generic premium methods in premium classes, so don't return yet for those
                if (!typeLower.contains("premium") && !typeLower.contains("billing") && !typeLower.contains("purchase") && !typeLower.contains("subscription") && !typeLower.contains("entitle")) {
                    // quick check: does class contain any premium method name? if not, skip
                    var hasPremiumMethod = false
                    for (m in classDef.methods) {
                        val n = m.name.lowercase()
                        if (n.contains("premium") || n.contains("haspremium") || n.contains("ispremium") || n.contains("entitle") || n.contains("haspro") || n.contains("ispro")) {
                            hasPremiumMethod = true; break
                        }
                    }
                    if (!hasPremiumMethod) {
                        // also check if any method calls prefs - if not, skip
                        var callsPrefs = false
                        for (m in classDef.methods) {
                            val impl = m.implementation ?: continue
                            for (insn in impl.instructions) {
                                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                                val def = ref.definingClass
                                val isPrefs = def.contains("PlayerPrefs") || def == "Landroid/content/SharedPreferences;" || def.contains("DataStore") || def.contains("MMKV") || def.contains("Preferences")
                                if (isPrefs && (ref.name == "getBoolean" || ref.name == "getInt" || ref.name == "getString" || ref.name == "contains" || ref.name == "getValue" || ref.name == "get")) {
                                    callsPrefs = true; break
                                }
                            }
                            if (callsPrefs) break
                        }
                        if (!callsPrefs) return@classDefForEach
                    }
                }
            }

            val mutableClass = try { mutableClassDefBy(classDef) } catch (_: Exception) { return@classDefForEach }

            // 2a) Generic premium Z methods (hasPremiumAccess etc.) in this class
            for (method in mutableClass.methods) {
                if (method.returnType != "Z") continue
                val n = method.name.lowercase()
                if (n.length < 3 || n.length > 40) continue
                if (n.contains("provider") || n.contains("product") || n.contains("progress") || n.contains("probableprime") || n.contains("providerenabled")) continue
                val isPremiumName = n.contains("premium") || n.contains("haspremium") || n.contains("ispremium") || n.contains("entitle") || n.contains("haspro") || n.contains("ispro") || n.contains("hassubscription") || n.contains("issubscribed")
                val isPremiumClass = typeLower.contains("premium") || typeLower.contains("billing") || typeLower.contains("purchase") || typeLower.contains("subscription") || typeLower.contains("entitle")
                if (!isPremiumName && !isPremiumClass) continue
                if (n == "ispro" || n == "haspro" || n == "isprouser" || n == "hasprouser" || n.contains("premium")) {
                    try {
                        if (method.implementation == null) continue
                        // avoid double-patching if already patched via Fingerprint
                        method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                        patched++
                        patchedMethods.add("Generic:${method.name}")
                    } catch (_: Exception) {}
                }
            }

            // 2b) Prefs/DataStore getBoolean/getInt/contains -> premium keys
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
                    for (j in index - 1 downTo maxOf(0, index - 6)) {
                        val prev = instructions[j]
                        if (prev.opcode != Opcode.CONST_STRING) continue
                        val reg = (prev as? com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction)?.registerA ?: continue
                        if (reg != keyRegister) continue
                        keyValue = ((prev as? ReferenceInstruction)?.reference as? StringReference)?.string
                        break
                    }
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

        if (patched > 0) {
            logger.info("Unlock Premium: patched $patched check(s)")
            logger.info("Patched methods: ${patchedMethods.sorted().joinToString(", ")}")
        } else {
            logger.warning("No premium/ownership checks found. No changes applied.")
        }
    }
}
