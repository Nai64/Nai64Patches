package patches.universal.unlock

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.intOption
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.patch.booleanOption
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
    "coin", "gem", "diamond", "gold", "silver", "platinum",
    "money", "cash", "credit", "point", "star",
    "energy", "stamina", "heart", "life", "ticket",
    "soul", "shard", "crystal", "trophy",
    "pearl", "ruby", "emerald", "sapphire", "amber",
    "essence", "dust", "fragment", "rune", "orb",
    "elixir", "badge", "medal", "crown", "flame",
    "scroll", "blessing", "favor", "glory", "honor",
    "relic", "artifact", "bounty",
)

private val excludedKeySubstrings = setOf(
    "pass", "subscription", "premium", "ads_enabled", "adsremoved",
    "sound", "music", "sfx", "vibrate", "vibration", "notification",
    "tutorial", "first_launch", "first_time", "onboarded",
    "is_purchased", "owned", "unlocked", "bought", "claimed",
    "enabled", "disabled", "active", "show", "seen",
    "rate", "review", "share", "follow", "like",
    "language", "locale", "server", "version", "build",
    "height", "weight", "width", "scale", "size",
)

private val il2cppCurrencyMethods = setOf(
    "get_Coins", "set_Coins", "GetCoins", "SetCoins", "AddCoins",
    "get_Gems", "set_Gems", "GetGems", "SetGems", "AddGems",
    "GetGemPrice", "CanAffordGem",
    "get_Diamonds", "set_Diamonds", "GetDiamonds", "SetDiamonds", "AddDiamonds",
    "get_Gold", "set_Gold", "GetGold", "SetGold", "AddGold",
    "get_Silver", "set_Silver", "GetSilver", "SetSilver",
    "get_Money", "set_Money", "GetMoney", "SetMoney",
    "get_Cash", "set_Cash", "GetCash", "SetCash",
    "get_Credits", "set_Credits", "GetCredits", "SetCredits", "AddCredits",
    "get_Tokens", "set_Tokens", "GetTokens", "SetTokens", "AddTokens",
    "get_Points", "set_Points", "GetPoints", "SetPoints", "AddPoints",
    "get_Stars", "set_Stars", "GetStars", "SetStars", "AddStars",
    "get_Keys", "set_Keys", "GetKeys", "SetKeys", "AddKeys",
    "get_Energy", "set_Energy", "GetEnergy", "SetEnergy", "AddEnergy",
    "get_Stamina", "set_Stamina", "GetStamina", "SetStamina",
    "get_Hearts", "set_Hearts", "GetHearts", "SetHearts",
    "get_Lives", "set_Lives", "GetLives", "SetLives",
    "get_Tickets", "set_Tickets", "GetTickets", "SetTickets",
    "get_Souls", "set_Souls", "GetSouls", "SetSouls",
    "get_Shard", "set_Shard", "GetShard", "SetShard",
    "get_Crystals", "set_Crystals", "GetCrystals", "SetCrystals",
    "get_Trophies", "set_Trophies", "GetTrophies", "SetTrophies",
    "get_Pearls", "set_Pearls", "GetPearls", "SetPearls",
    "get_Rubies", "set_Rubies", "GetRubies", "SetRubies",
    "get_Essence", "set_Essence", "GetEssence", "SetEssence",
    "get_Dust", "set_Dust", "GetDust", "SetDust",
    "get_Fragments", "set_Fragments", "GetFragments", "SetFragments",
    "get_Badges", "set_Badges", "GetBadges", "SetBadges",
    "get_Medals", "set_Medals", "GetMedals", "SetMedals",
    "get_Elixir", "set_Elixir", "GetElixir", "SetElixir",
    "get_Prestige", "set_Prestige", "GetPrestige", "SetPrestige",
    "get_RebirthCount", "SetRebirthCount", "GetRebirth", "SetRebirth",
    "get_EpicChest", "SetEpicChest", "GetNormalChest", "SetNormalChest",
    "GetFreeClaimCount", "get_Trophy", "set_Trophy", "GetTrophy", "SetTrophy",
)

private fun String.isCurrencyKey(customKeys: Set<String>): Boolean {
    val lower = lowercase()
    if (excludedKeySubstrings.any { lower.contains(it) }) return false
    if (customKeys.any { it.isNotEmpty() && lower == it }) return true
    for (sub in currencySubstrings) {
        val subLower = sub.lowercase()
        if (lower == subLower) return true
        if (lower.startsWith(subLower + "_")) return true
        if (lower.endsWith("_" + subLower)) return true
        if (lower.contains("_" + subLower + "_")) return true
    }
    return false
}

private fun isPriceMethod(name: String): Boolean {
    val lower = name.lowercase()
    return lower.contains("price") || lower.contains("cost") || lower.contains("paymentenabled")
}

@Suppress("unused")
val unlimitedCurrenciesPatch = bytecodePatch(
    name = "Unlimited Currencies",
    description = "Get unlimited coins, gems and other currencies. Works on most games. Not supported on newer Unity games.",
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
    val infiniteMode by booleanOption(
        title = "Infinite mode",
        default = true,
        key = "infiniteMode",
        description = "ON: all currencies set to huge amount. OFF: currencies set to the Amount value above.",
    )
    val multiplyMode by booleanOption(
        title = "Multiply income",
        default = false,
        key = "multiplyMode",
        description = "When ON, every time a currency is saved it gets multiplied by the Multiplier value. Useful when infinite mode doesn't work.",
    )
    val multiplier by intOption(
        title = "Multiplier",
        default = 10,
        key = "currencyMultiplier",
        description = "Multiplier value used in Multiply income mode.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val isInfinite = infiniteMode ?: true
        val isMultiply = multiplyMode ?: false
        val target = if (isInfinite) 999999 else (amount ?: 999999).coerceIn(1, 999999999)
        val mult = (multiplier ?: 10).coerceIn(2, 10000)
        val customSet = (customKeys ?: "").split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
        var patched = 0
        val affectedCurrencies = mutableSetOf<String>()

        // 1) IL2CPP currency methods via Fingerprint
        for (methodName in il2cppCurrencyMethods) {
            val fp = object : Fingerprint(name = methodName) {}
            val method = fp.methodOrNull ?: continue
            if (method.implementation == null) continue
            try {
                if (isPriceMethod(methodName)) {
                    if (method.returnType == "I" || method.returnType == "F") {
                        method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                        affectedCurrencies.add(methodName)
                        patched++
                    }
                } else if (methodName.startsWith("get_") || methodName.startsWith("Get") || methodName.startsWith("Add")) {
                    when (method.returnType) {
                        "I" -> {
                            val instr = if (target in -32768..32767) "const/16 v0, $target\nreturn v0" else "const v0, $target\nreturn v0"
                            method.addInstructions(0, instr)
                            affectedCurrencies.add(methodName)
                            patched++
                        }
                        "J" -> { method.addInstructions(0, "const-wide v0, 0x${target.toString(16)}\nreturn-wide v0"); affectedCurrencies.add(methodName); patched++ }
                        "F" -> {
                            val bits = java.lang.Float.floatToRawIntBits(target.toFloat())
                            method.addInstructions(0, "const v0, 0x${Integer.toHexString(bits)}\nreturn v0"); affectedCurrencies.add(methodName); patched++
                        }
                        "Ljava/lang/String;" -> { method.addInstructions(0, "const-string v0, \"$target\"\nreturn-object v0"); affectedCurrencies.add(methodName); patched++ }
                    }
                } else if (methodName.startsWith("Set") || methodName.startsWith("set")) {
                    when (method.returnType) {
                        "V" -> { method.addInstructions(0, "return-void"); affectedCurrencies.add(methodName); patched++ }
                        "I", "Z" -> {
                            val instr = if (target in -32768..32767) "const/16 v0, $target\nreturn v0" else "const v0, $target\nreturn v0"
                            method.addInstructions(0, instr); affectedCurrencies.add(methodName); patched++
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        // 2) SharedPreferences / PlayerPrefs: single-pass classDefForEach with pre-filter
        classDefForEach { classDef ->
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
                    if (isPutInt) {
                        if (!isMultiply) continue
                        val valueReg = when (insn) {
                            is BuilderInstruction35c -> insn.registerE
                            is BuilderInstruction3rc -> insn.startRegister + 2
                            else -> continue
                        }
                        try {
                            method.addInstructions(index, "mul-int/lit8 v$valueReg, v$valueReg, $mult")
                            affectedCurrencies.add(keyValue!!)
                            patched++
                        } catch (_: Exception) {}
                        continue
                    }
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
                            affectedCurrencies.add(keyValue!!)
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
                            affectedCurrencies.add(keyValue!!)
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
                            affectedCurrencies.add(keyValue!!)
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
                            affectedCurrencies.add(keyValue!!)
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
                            affectedCurrencies.add(keyValue!!)
                            patched++
                        }
                    }
                }
            }
        }

        if (patched > 0) {
            logger.info("Unlimited currencies: spoofed $patched currency check(s) to $target")
            if (affectedCurrencies.isNotEmpty()) {
                logger.info("Affected currencies: ${affectedCurrencies.sorted().joinToString(", ")}")
            }
        } else {
            logger.warning("No currency keys found. Try adding custom keys via extra keys option.")
        }
    }
}
