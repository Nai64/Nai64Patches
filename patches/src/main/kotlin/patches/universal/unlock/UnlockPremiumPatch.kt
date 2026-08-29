package patches.universal.unlock

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.logging.Logger

@Suppress("unused")
val unlockPremiumPatch = bytecodePatch(
    name = "Unlock Premium",
    description = "Unlocks premium features, subscriptions, and ownership checks. Makes isPremium/isOwned/isVip/hasReceipt return true, and forces SharedPreferences purchase keys to true.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        val patchedMethods = mutableSetOf<String>()

        // ──────────────────────────────────────────────
        // PURCHASE STATE / OWNERSHIP CHECKS (by method name)
        // ──────────────────────────────────────────────

        for (checkName in listOf(
            "isPurchased", "isOwned", "isPremium", "hasPremium",
            "isSubscribed", "hasSubscription", "isVip", "hasVip",
            "isBought", "hasBought", "wasPurchased", "hasPurchased",
            "isPro", "hasPro", "isFullVersion", "hasFullVersion",
            "isUnlocked", "hasUnlocked", "isActive", "hasActive",
            "isLifetime", "hasLifetime", "isAnnual", "hasAnnual",
        )) {
            val fp = object : Fingerprint(
                name = checkName,
                returnType = "Z",
            ) {}
            val method = fp.methodOrNull
            if (method?.implementation != null) {
                try {
                    method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                    patchedMethods.add(checkName)
                    patched++
                } catch (_: Exception) {}
            }
        }

        // ──────────────────────────────────────────────
        // RECEIPT CHECKS
        // ──────────────────────────────────────────────

        for (receiptName in listOf(
            "hasReceipt", "getHasReceipt", "hasValidReceipt",
            "isReceiptValid", "hasActiveReceipt", "getReceipt",
        )) {
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

        // Also catch hasReceipt / getHasReceipt returning String (some games return receipt data)
        for (receiptName in listOf("hasReceipt", "getHasReceipt", "getReceipt")) {
            val fp = object : Fingerprint(
                name = receiptName,
                returnType = "Ljava/lang/String;",
            ) {}
            val method = fp.methodOrNull
            if (method?.implementation != null) {
                try {
                    method.addInstructions(0, "const-string v0, \"fake_receipt_data\"\nreturn-object v0")
                    patchedMethods.add("$receiptName:String")
                    patched++
                } catch (_: Exception) {}
            }
        }

        // ──────────────────────────────────────────────
        // classDefForEach: SharedPreferences bypass
        // ──────────────────────────────────────────────

        classDefForEach { classDef ->
            var foundPrefsCall = false
            for (method in classDef.methods) {
                val impl = method.implementation ?: continue
                for (insn in impl.instructions) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    val name = ref.name
                    val def = ref.definingClass
                    val isPrefs = def.contains("PlayerPrefs") || def == "Landroid/content/SharedPreferences;" || def == "Landroid/content/SharedPreferences\$Editor;"
                    if (isPrefs && (name == "GetInt" || name == "getInt" || name == "GetString" || name == "getString" || name == "HasKey" || name == "contains")) {
                        foundPrefsCall = true; break
                    }
                }
                if (foundPrefsCall) break
            }
            if (!foundPrefsCall) return@classDefForEach

            val premiumKeys = setOf(
                "purchased", "is_purchased", "has_receipt", "hasreceipt", "bought",
                "premium", "is_premium", "ispremium", "vip", "is_vip",
                "no_ads", "noads", "ads_removed", "adsremoved", "ad_free", "adfree",
                "full_version", "fullversion", "pro", "unlocked",
                "subscribed", "is_subscribed", "subscription_active",
                "lifetime", "is_lifetime", "annual", "monthly",
            )

            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    val mname = ref.name
                    val def = ref.definingClass
                    val isSharedPrefs = def == "Landroid/content/SharedPreferences;"
                    val isHasKey = (mname == "HasKey" || mname == "contains") && ref.returnType == "Z"

                    if (!isHasKey) continue

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

                    if (keyValue != null && premiumKeys.any { keyValue!!.lowercase().contains(it) }) {
                        val next = instructions.getOrNull(index + 1) ?: continue
                        if (next.opcode == Opcode.MOVE_RESULT) {
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
                            patchedMethods.add("SharedPrefs:${keyValue}")
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
