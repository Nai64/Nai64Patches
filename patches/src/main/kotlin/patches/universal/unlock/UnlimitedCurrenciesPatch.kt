package patches.universal.unlock

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

// Exact Time Evolve currency keys from global-metadata.dat (only currency, no level/era/timeline)
private val timeEvolveCurrencyKeys = setOf(
    "currentgem", "totalcollectedgem", "diamond", "cash", "goldkey", "chestgoldkey", "trophy",
    "currentenergy", "maxenergy", "energycapacity", "energyrecharge", "energyjarcount",
    "piggygembankcurrentgem", "piggygembankdata", "pendingdiamondcardgems", "pendingevolutioncardgems",
    "goldreward", "heartreward", "freegemamount", "adsgemamount", "gemprice", "priceingems",
    "ads_free_gem", "ads_free_energy", "ads_gold_chest", "diamondexpiryseconds", "energycardexpiryseconds",
    "ticketcapacity", "ticketsavedata", "diamondcard", "energycard", "piggygembank"
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
    if (timeEvolveCurrencyKeys.contains(lower)) return true
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

        // 1) PlayerPrefs / SharedPreferences get* with key check
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                val instructions: List<Instruction> = impl.instructions.toList()
                for ((index, insn) in instructions.withIndex()) {
                    val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                    val name = ref.name
                    val def = ref.definingClass
                    val isPlayerPrefs = def.contains("PlayerPrefs")
                    val isSharedPrefs = def == "Landroid/content/SharedPreferences;"
                    val isEditor = def == "Landroid/content/SharedPreferences\$Editor;"
                    val isGetInt = (name == "GetInt" || name == "getInt") && ref.returnType == "I" && ref.parameterTypes.size == 2 && ref.parameterTypes[0] == "Ljava/lang/String;"
                    val isGetLong = (name == "GetLong" || name == "getLong") && (ref.returnType == "J") && ref.parameterTypes.size == 2
                    val isGetFloat = (name == "GetFloat" || name == "getFloat") && ref.returnType == "F"
                    val isGetString = (name == "GetString" || name == "getString") && ref.returnType == "Ljava/lang/String;"
                    val isHasKey = (name == "HasKey" || name == "contains") && ref.returnType == "Z"
                    val isPutInt = isEditor && name == "putInt" && ref.parameterTypes.size == 2
                    if (!isPlayerPrefs && !isSharedPrefs && !isEditor) {
                        // Also allow generic getInt with String key for other prefs
                        if (!isGetInt && !isGetLong && !isGetFloat && !isGetString && !isHasKey && !isPutInt) continue
                    }
                    if (!isGetInt && !isGetLong && !isGetFloat && !isGetString && !isHasKey && !isPutInt) continue

                    // Find key register: for static PlayerPrefs GetInt is registerC, for instance SharedPrefs getInt is registerD
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
                        // Fallback: check any nearby const string with currency substring
                        for (j in index - 1 downTo maxOf(0, index - 6)) {
                            val prev = instructions[j]
                            if (prev.opcode != Opcode.CONST_STRING) continue
                            val s = ((prev as? ReferenceInstruction)?.reference as? StringReference)?.string ?: continue
                            if (s.isCurrencyKey(customSet)) { keyValue = s; break }
                        }
                    }
                    if (keyValue == null || !keyValue.isCurrencyKey(customSet)) continue

                    val next = instructions.getOrNull(index + 1)
                    // Handle putInt -> make it keep large value (nop put, keep chain)
                    if (isPutInt) {
                        // Editor.putInt returns Editor, we can just nop the put and keep builder
                        // Instead of nopping, we could force the int arg to large value
                        // Find int arg register (third reg)
                        val intReg = when (insn) {
                            is BuilderInstruction35c -> if (insn.registerCount >= 3) insn.registerE else null
                            is BuilderInstruction3rc -> insn.startRegister + 2
                            else -> null
                        } ?: continue
                        // Walk back to const that loads int value and set to target
                        for (j in index - 1 downTo maxOf(0, index - 6)) {
                            val prev = instructions[j]
                            val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                            if (reg != intReg) continue
                            if (prev.opcode == Opcode.CONST_4 || prev.opcode == Opcode.CONST_16 || prev.opcode == Opcode.CONST) {
                                val constInstr = if (intReg <= 0xf) "const/4 v$intReg, 0x${target.toString(16)}" else "const/16 v$intReg, $target"
                                // Actually need proper const for int: use const
                                val instr = if (target in -8..7 && intReg <= 0xf) "const/4 v$intReg, $target" else if (intReg <= 0xff) "const/16 v$intReg, $target" else "const v$intReg, $target"
                                method.replaceInstruction(j, instr)
                                patched++
                                break
                            }
                        }
                        continue
                    }

                    if (next == null) continue
                    when {
                        isGetInt && next.opcode == Opcode.MOVE_RESULT -> {
                            val r = (next as OneRegisterInstruction).registerA
                            val instr = if (r <= 0xf && target in -8..7) "const/4 v$r, $target" else if (r <= 0xff) "const/16 v$r, $target" else "const v$r, $target"
                            method.replaceInstruction(index, instr)
                            method.replaceInstruction(index + 1, "nop")
                            patched++
                        }
                        isGetLong && next.opcode == Opcode.MOVE_RESULT_WIDE -> {
                            val r = (next as OneRegisterInstruction).registerA
                            val hex = "0x" + target.toString(16)
                            val instr = if (r <= 0xff) "const-wide/32 v$r, $hex" else "const-wide v$r, $hex"
                            method.replaceInstruction(index, instr)
                            method.replaceInstruction(index + 1, "nop")
                            patched++
                        }
                        isGetFloat && next.opcode == Opcode.MOVE_RESULT -> {
                            val r = (next as OneRegisterInstruction).registerA
                            val bits = java.lang.Float.floatToRawIntBits(target.toFloat())
                            val hex = "0x" + Integer.toHexString(bits)
                            method.replaceInstruction(index, "const/high16 v$r, $hex")
                            method.replaceInstruction(index + 1, "nop")
                            patched++
                        }
                        isGetString && next.opcode == Opcode.MOVE_RESULT_OBJECT -> {
                            val r = (next as OneRegisterInstruction).registerA
                            val instr = if (r <= 0xff) "const-string v$r, \"$target\"" else "const-string/jumbo v$r, \"$target\""
                            method.replaceInstruction(index, instr)
                            method.replaceInstruction(index + 1, "nop")
                            patched++
                        }
                        isHasKey && next.opcode == Opcode.MOVE_RESULT -> {
                            val r = (next as OneRegisterInstruction).registerA
                            method.replaceInstruction(index, "const/4 v$r, 0x1")
                            method.replaceInstruction(index + 1, "nop")
                            patched++
                        }
                    }
                }
            }
        }

        // 2) IL2CPP fields: Change* / Get* for currency
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (m in mutableClass.methods) {
                val name = m.name
                val lower = name.lowercase()
                val isCurrencyMethod = il2cppCurrencyMethods.contains(name) || currencySubstrings.any { lower.contains(it) }
                if (!isCurrencyMethod) continue
                if (m.implementation == null) continue
                // Price methods should be 0
                if (isPriceMethod(name)) {
                    if (m.returnType == "I" || m.returnType == "F") {
                        val next = m.implementation!!.instructions.firstOrNull()
                        // Simple early return 0
                        try {
                            m.addInstructions(0, if (m.returnType == "I") "const/4 v0, 0x0\nreturn v0" else "const/4 v0, 0x0\nreturn v0")
                            patched++
                        } catch (_: Exception) {}
                    }
                    continue
                }
                // For getters like get_CurrentGem, GetCurrentGem -> return large
                if (name.startsWith("get_") || name.startsWith("Get") || name.startsWith("get")) {
                    if (m.returnType == "I") {
                        try {
                            m.addInstructions(0, if (target in -8..7) "const/4 v0, $target\nreturn v0" else "const/16 v0, $target\nreturn v0")
                            patched++
                        } catch (_: Exception) {}
                    } else if (m.returnType == "J") {
                        try {
                            m.addInstructions(0, "const-wide v0, 0x${target.toString(16)}\nreturn-wide v0")
                            patched++
                        } catch (_: Exception) {}
                    } else if (m.returnType == "F") {
                        try {
                            val bits = java.lang.Float.floatToRawIntBits(target.toFloat())
                            m.addInstructions(0, "const/high16 v0, 0x${Integer.toHexString(bits)}\nreturn v0")
                            patched++
                        } catch (_: Exception) {}
                    } else if (m.returnType == "Ljava/lang/String;") {
                        try {
                            m.addInstructions(0, "const-string v0, \"$target\"\nreturn-object v0")
                            patched++
                        } catch (_: Exception) {}
                    }
                } else if (name.startsWith("Change") || name.startsWith("change")) {
                    // ChangeGem etc. - make it add large amount instead of spending: just return void or set to large
                    if (m.returnType == "V") {
                        // For void Change methods, we can nop the spend logic by early return, but keep gem at max via getter spoof already
                        // To also give currency, we could make ChangeGem set to target via field, but simpler: just early return (no spend)
                        try {
                            m.addInstructions(0, "return-void")
                            patched++
                        } catch (_: Exception) {}
                    } else if (m.returnType == "I" || m.returnType == "Z") {
                        try {
                            m.addInstructions(0, "const/16 v0, $target\nreturn v0")
                            patched++
                        } catch (_: Exception) {}
                    }
                } else if (name == "IsGemPaymentEnabled" || lower.contains("isgempaymentenabled")) {
                    if (m.returnType == "Z") {
                        try {
                            m.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                            patched++
                        } catch (_: Exception) {}
                    }
                }
            }
        }

        // 3) Xsolla virtual_currency network spoof (if any getAmount)
        classDefForEach { classDef ->
            if (!classDef.type.lowercase().contains("virtualcurrency") && !classDef.type.lowercase().contains("xsolla")) return@classDefForEach
            val mutableClass = mutableClassDefBy(classDef)
            for (m in mutableClass.methods) {
                if (m.name != "getAmount" && m.name != "getBalance" && !m.name.lowercase().contains("amount")) continue
                if (m.returnType != "I" && m.returnType != "J" && m.returnType != "F") continue
                if (m.implementation == null) continue
                try {
                    when (m.returnType) {
                        "I" -> m.addInstructions(0, "const v0, $target\nreturn v0")
                        "J" -> m.addInstructions(0, "const-wide v0, 0x${target.toString(16)}\nreturn-wide v0")
                        "F" -> {
                            val bits = java.lang.Float.floatToRawIntBits(target.toFloat())
                            m.addInstructions(0, "const/high16 v0, 0x${Integer.toHexString(bits)}\nreturn v0")
                        }
                    }
                    patched++
                } catch (_: Exception) {}
            }
        }

        if (patched > 0) logger.info("Unlimited currencies: spoofed $patched currency check(s) to $target")
        else logger.warning("No currency keys found. Try adding custom keys via extra keys option.")
    }
}
