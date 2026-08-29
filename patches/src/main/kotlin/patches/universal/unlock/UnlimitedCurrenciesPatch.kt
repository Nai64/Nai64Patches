package patches.universal.unlock

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.stringOption
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.logging.Logger

private val currencySubstrings = listOf(
    "coin", "coins", "gem", "gems", "diamond", "diamonds", "gold", "cash", "money", "currency",
    "crystal", "crystals", "ruby", "rubies", "token", "tokens", "chip", "chips", "credit", "credits",
    "premium", "soft_currency", "hard_currency", "balance", "wallet", "purse", "funds",
    "energy", "hearts", "heart", "ticket", "tickets", "key", "keys", "star", "stars", "trophy", "trophies",
    "emerald", "emeralds", "goldkey", "chestgoldkey", "piggygem", "currentgem", "totalcollectedgem",
    "diamondcard", "energycard", "goldreward", "heartreward", "ads_free_gem", "gemprice"
)

private val il2cppCurrencyMethods = setOf(
    "get_CurrentGem", "GetCurrentGem", "ChangeGem", "get_TotalCollectedGem", "GetTotalCollectedGem",
    "get_CurrentEnergy", "GetCurrentEnergy", "ChangeEnergy", "GetEnergyJarCount", "GetPiggyGemBankCurrentGem", "get_PiggyGemBankCurrentGem",
    "GetDiamond", "ChangeDiamond", "get_Diamond", "GetDiamondCard", "GetCash", "ChangeCash", "get_Cash",
    "GetGoldKey", "ChangeGoldKey", "get_GoldKey", "GetTrophy", "ChangeTrophy", "get_Trophy",
    "GetTicketCapacity", "GetGemPrice", "IsGemPaymentEnabled", "GetEnergyCard", "GetGoldReward", "GetHeartReward"
)

private fun String.isCurrencyKey(customKeys: Set<String>): Boolean {
    val lower = lowercase()
    if (customKeys.any { it.isNotEmpty() && lower.contains(it) }) return true
    return currencySubstrings.any { lower.contains(it) }
}

private fun isPriceMethod(name: String): Boolean {
    val lower = name.lowercase()
    return lower.contains("price") || lower.contains("cost") || lower.contains("paymentenabled")
}

@Suppress("unused")
val unlimitedCurrenciesPatch = bytecodePatch(
    name = "Unlimited Currencies",
    description = "Makes currency checks return a huge amount so you never run out. Covers PlayerPrefs, SharedPreferences and common Unity fields.",
    default = false,
) {
    val amount by intOption(
        title = "Amount",
        default = 999999,
        key = "currencyAmount",
        description = "Amount to report for currency. Price fields will be 0.",
    )
    val customKeys by stringOption(
        title = "Extra keys",
        default = "",
        key = "currencyCustomKeys",
        description = "Comma-separated extra keys to spoof (e.g. my_gem,my_coin). Leave empty for default list.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val target = (amount ?: 999999).coerceIn(1, 999999999)
        val customSet = (customKeys ?: "").split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        var patched = 0

        // 1) IL2CPP currency methods via Fingerprint (unique names, no classDefForEach needed)
        for (methodName in il2cppCurrencyMethods) {
            val fp = object : Fingerprint(name = methodName) {}
            val method = fp.methodOrNull ?: continue
            if (method.implementation == null) continue
            try {
                if (isPriceMethod(methodName)) {
                    if (method.returnType == "I" || method.returnType == "F") {
                        method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                        patched++
                    }
                } else if (methodName.startsWith("get_") || methodName.startsWith("Get")) {
                    when (method.returnType) {
                        "I" -> {
                            val instr = if (target in -32768..32767) "const/16 v0, $target\nreturn v0" else "const v0, $target\nreturn v0"
                            method.addInstructions(0, instr)
                            patched++
                        }
                        "J" -> { method.addInstructions(0, "const-wide v0, 0x${target.toString(16)}\nreturn-wide v0"); patched++ }
                        "F" -> {
                            val bits = java.lang.Float.floatToRawIntBits(target.toFloat())
                            method.addInstructions(0, "const v0, 0x${Integer.toHexString(bits)}\nreturn v0"); patched++
                        }
                        "Ljava/lang/String;" -> { method.addInstructions(0, "const-string v0, \"$target\"\nreturn-object v0"); patched++ }
                    }
                } else if (methodName.startsWith("Change") || methodName.startsWith("change")) {
                    when (method.returnType) {
                        "V" -> { method.addInstructions(0, "return-void"); patched++ }
                        "I", "Z" -> {
                            val instr = if (target in -32768..32767) "const/16 v0, $target\nreturn v0" else "const v0, $target\nreturn v0"
                            method.addInstructions(0, instr); patched++
                        }
                    }
                } else if (methodName.lowercase().contains("isgempaymentenabled")) {
                    if (method.returnType == "Z") { method.addInstructions(0, "const/4 v0, 0x1\nreturn v0"); patched++ }
                }
            } catch (_: Exception) {}
        }

        // 2) SharedPreferences / PlayerPrefs: single-pass classDefForEach with pre-filter
        //    Only creates mutable copies for classes that actually contain currency strings
        classDefForEach { classDef ->
            // Pre-filter: check if any method in this class references a SharedPreferences call
            // with a currency-related const-string nearby. Skip classes that don't match.
            var foundPrefsCall = false
            for (method in classDef.methods) {
                val impl = method.implementation ?: continue
                for (insn in impl.instructions) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    val name = ref.name
                    val def = ref.definingClass
                    val isPrefs = def.contains("PlayerPrefs") || def == "Landroid/content/SharedPreferences;" || def == "Landroid/content/SharedPreferences\$Editor;"
                    val isGetOrCheck = (name == "GetInt" || name == "getInt" || name == "GetLong" || name == "getLong" ||
                        name == "GetFloat" || name == "getFloat" || name == "GetString" || name == "getString" ||
                        name == "HasKey" || name == "contains")
                    if (isPrefs && isGetOrCheck) { foundPrefsCall = true; break }
                }
                if (foundPrefsCall) break
            }
            if (!foundPrefsCall) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions: List<Instruction> = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    val mname = ref.name
                    val def = ref.definingClass
                    val isPlayerPrefs = def.contains("PlayerPrefs")
                    val isSharedPrefs = def == "Landroid/content/SharedPreferences;"
                    val isEditor = def == "Landroid/content/SharedPreferences\$Editor;"
                    val isGetInt = (mname == "GetInt" || mname == "getInt") && ref.returnType == "I" && ref.parameterTypes.size == 2 && ref.parameterTypes[0] == "Ljava/lang/String;"
                    val isGetLong = (mname == "GetLong" || mname == "getLong") && (ref.returnType == "J") && ref.parameterTypes.size == 2
                    val isGetFloat = (mname == "GetFloat" || mname == "getFloat") && ref.returnType == "F"
                    val isGetString = (mname == "GetString" || mname == "getString") && ref.returnType == "Ljava/lang/String;"
                    val isHasKey = (mname == "HasKey" || mname == "contains") && ref.returnType == "Z"
                    val isPutInt = isEditor && mname == "putInt" && ref.parameterTypes.size == 2
                    if (!isPlayerPrefs && !isSharedPrefs && !isEditor && !isGetInt && !isGetLong && !isGetFloat && !isGetString && !isHasKey && !isPutInt) continue
                    if (!isGetInt && !isGetLong && !isGetFloat && !isGetString && !isHasKey && !isPutInt) continue

                    val keyRegister = when (insn) {
                        is BuilderInstruction35c -> when (insn.registerCount) {
                            1 -> insn.registerC
                            2 -> insn.registerC
                            else -> insn.registerD
                        }
                        is BuilderInstruction3rc -> insn.startRegister + 1
                        else -> continue
                    }
                    var keyValue: String? = null
                    for (j in index - 1 downTo maxOf(0, index - 6)) {
                        val prev = instructions[j]
                        if (prev.opcode != Opcode.CONST_STRING) continue
                        val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                        if (reg != keyRegister) continue
                        keyValue = ((prev as? ReferenceInstruction)?.reference as? StringReference)?.string
                        break
                    }
                    if (keyValue == null) {
                        for (j in index - 1 downTo maxOf(0, index - 6)) {
                            val prev = instructions[j]
                            if (prev.opcode != Opcode.CONST_STRING) continue
                            val s = ((prev as? ReferenceInstruction)?.reference as? StringReference)?.string ?: continue
                            if (s.isCurrencyKey(customSet)) { keyValue = s; break }
                        }
                    }
                    if (keyValue == null || !keyValue.isCurrencyKey(customSet)) continue

                    val next = instructions.getOrNull(index + 1)
                    if (isPutInt) continue
                    if (next == null) continue
                    when {
                        isGetInt && next.opcode == Opcode.MOVE_RESULT -> {
                            val r = (next as OneRegisterInstruction).registerA
                            if (r <= 0xff) {
                                val instr = when {
                                    target in -8..7 && r <= 0xf -> "const/4 v$r, $target"
                                    target in -32768..32767 -> "const/16 v$r, $target"
                                    else -> "const v$r, $target"
                                }
                                method.replaceInstruction(index, instr)
                                method.replaceInstruction(index + 1, "nop")
                            } else {
                                val instr = when {
                                    target in -32768..32767 -> "const/16 v0, $target"
                                    else -> "const v0, $target"
                                }
                                method.replaceInstruction(index, instr)
                                method.replaceInstruction(index + 1, "move v$r, v0")
                            }
                            patched++
                        }
                        isGetLong && next.opcode == Opcode.MOVE_RESULT_WIDE -> {
                            val r = (next as OneRegisterInstruction).registerA
                            val hex = "0x" + target.toString(16)
                            if (r <= 0xff) {
                                method.replaceInstruction(index, "const-wide/32 v$r, $hex")
                                method.replaceInstruction(index + 1, "nop")
                            } else {
                                method.replaceInstruction(index, "const-wide/32 v0, $hex")
                                method.replaceInstruction(index + 1, "move-wide v$r, v0")
                            }
                            patched++
                        }
                        isGetFloat && next.opcode == Opcode.MOVE_RESULT -> {
                            val r = (next as OneRegisterInstruction).registerA
                            val bits = java.lang.Float.floatToRawIntBits(target.toFloat())
                            val hex = "0x" + Integer.toHexString(bits)
                            if (r <= 0xff) {
                                method.replaceInstruction(index, "const v$r, $hex")
                                method.replaceInstruction(index + 1, "nop")
                            } else {
                                method.replaceInstruction(index, "const v0, $hex")
                                method.replaceInstruction(index + 1, "move v$r, v0")
                            }
                            patched++
                        }
                        isGetString && next.opcode == Opcode.MOVE_RESULT_OBJECT -> {
                            val r = (next as OneRegisterInstruction).registerA
                            if (r <= 0xff) {
                                method.replaceInstruction(index, "const-string v$r, \"$target\"")
                                method.replaceInstruction(index + 1, "nop")
                            } else if (r <= 0xffff) {
                                method.replaceInstruction(index, "const-string/jumbo v$r, \"$target\"")
                                method.replaceInstruction(index + 1, "nop")
                            } else {
                                method.replaceInstruction(index, "const-string v0, \"$target\"")
                                method.replaceInstruction(index + 1, "move-object v$r, v0")
                            }
                            patched++
                        }
                        isHasKey && next.opcode == Opcode.MOVE_RESULT -> {
                            val r = (next as OneRegisterInstruction).registerA
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
                            patched++
                        }
                    }
                }
            }
        }

        // 3) Xsolla: Fingerprint per method name
        for (xsollaMethod in listOf("getAmount", "getBalance")) {
            val fp = object : Fingerprint(
                name = xsollaMethod,
                custom = { _, classDef ->
                    classDef.type.lowercase().let { it.contains("virtualcurrency") || it.contains("xsolla") }
                },
            ) {}
            val method = fp.methodOrNull ?: continue
            if (method.implementation == null) continue
            if (method.returnType != "I" && method.returnType != "J" && method.returnType != "F") continue
            try {
                when (method.returnType) {
                    "I" -> method.addInstructions(0, "const v0, $target\nreturn v0")
                    "J" -> method.addInstructions(0, "const-wide v0, 0x${target.toString(16)}\nreturn-wide v0")
                    "F" -> {
                        val bits = java.lang.Float.floatToRawIntBits(target.toFloat())
                        method.addInstructions(0, "const v0, 0x${Integer.toHexString(bits)}\nreturn v0")
                    }
                }
                patched++
            } catch (_: Exception) {}
        }

        if (patched > 0) logger.info("Unlimited currencies: spoofed $patched currency check(s) to $target")
        else logger.warning("No currency keys found. Try adding custom keys via extra keys option.")
    }
}
