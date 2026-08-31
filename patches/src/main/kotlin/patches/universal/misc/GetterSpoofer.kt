package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference

private fun escapeSmali(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n")
        .replace("\r", "\\n")
        .replace("\t", "\\t")

/**
 * Emits a `const-string` safe for any register: `const-string` (21c, <= v255)
 * or `const-string/jumbo` (31c, <= v65535).
 */
private fun constString(reg: Int, value: String): String {
    val body = "\"${escapeSmali(value)}\""
    return if (reg <= 0xff) {
        "const-string v$reg, $body"
    } else {
        "const-string/jumbo v$reg, $body"
    }
}

/**
 * Folds a Settings provider getter (`getInt`/`getFloat`/`getString`) into a
 * constant keyed by the name argument:
 *  - `getInt`   -> `0`
 *  - `getFloat` -> `0.0`
 *  - `getString`-> [stringValue]
 *
 * This patches the app's *invocations* of the framework getters (the patcher
 * cannot modify framework classes), which is the standard detection path apps
 * use.
 *
 * @return number of patched call sites.
 */
internal fun BytecodePatchContext.foldSettingsGetterConst(
    classes: Set<String>,
    keys: Set<String>,
    stringValue: String = "",
): Int {
    var patched = 0
    classDefForEach { classDef ->
        var hasRef = false
        for (m in classDef.methods) {
            val impl = m.implementation ?: continue
            for (insn in impl.instructions) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.definingClass in classes && (ref.name == "getInt" || ref.name == "getString" || ref.name == "getFloat") && ref.parameterTypes.size >=2) { hasRef = true; break }
            }
            if (hasRef) break
        }
        if (!hasRef) return@classDefForEach
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val implementation = method.implementation ?: continue
            // Snapshot; one-for-one replacements keep indices valid.
            val instructions: List<Instruction> = implementation.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: continue
                if (reference.definingClass !in classes) continue
                if (reference.name != "getInt" &&
                    reference.name != "getString" &&
                    reference.name != "getFloat"
                ) {
                    continue
                }

                val params = reference.parameterTypes
                if (params.size < 2) continue
                if (params[0] != "Landroid/content/ContentResolver;") continue
                if (params[1] != "Ljava/lang/String;") continue

                // The key (name) argument is the second invoke register:
                // 35c layout: B=first, C=second; 3rc (range): start + 1.
                val keyRegister = when (instruction) {
                    is BuilderInstruction35c -> instruction.registerC
                    is BuilderInstruction3rc -> instruction.startRegister + 1
                    else -> continue
                }

                // Walk backwards for the const-string or sget-object that fills the key register.
                var keyValue: String? = null
                for (j in index - 1 downTo 0) {
                    val prev = instructions[j]
                    val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                    if (reg != keyRegister) continue
                    when (prev.opcode) {
                        Opcode.CONST_STRING -> {
                            keyValue = ((prev as? ReferenceInstruction)?.reference as? StringReference)?.string
                            break
                        }
                        Opcode.SGET_OBJECT -> {
                            val field = (prev as? ReferenceInstruction)?.reference as? FieldReference ?: continue
                            // Field like Settings.Secure.ANDROID_ID -> "android_id"
                            val fieldKey = field.name.lowercase()
                            if (fieldKey in keys) {
                                keyValue = fieldKey
                                break
                            }
                            // also handle exact match (some keys are not just lowercased field)
                            if (field.name in keys) {
                                keyValue = field.name
                                break
                            }
                        }
                        else -> continue
                    }
                }
                if (keyValue !in keys) continue

                val next = instructions.getOrNull(index + 1)
                if (next != null &&
                    (next.opcode == Opcode.MOVE_RESULT ||
                        next.opcode == Opcode.MOVE_RESULT_OBJECT ||
                        next.opcode == Opcode.MOVE_RESULT_WIDE)
                ) {
                    val resultRegister = (next as OneRegisterInstruction).registerA
                    val replacement = when (reference.returnType) {
                        "I" -> if (resultRegister <= 0xff) {
                            "const/16 v$resultRegister, 0x0"
                        } else {
                            "const v$resultRegister, 0x0"
                        }
                        "F" -> if (resultRegister <= 0xf) {
                            "const/4 v$resultRegister, 0x0"
                        } else {
                            "const/16 v$resultRegister, 0x0"
                        }
                        else -> constString(resultRegister, stringValue)
                    }
                    method.replaceInstruction(index, replacement)
                    method.replaceInstruction(index + 1, "nop")
                } else {
                    method.replaceInstruction(index, "nop")
                }
                patched++
            }
        }
    }
    return patched
}

/**
 * Folds a no-argument `String` getter (e.g. `TelephonyManager.getSimCountryIso`)
 * into a constant [value]. The invoke is replaced by a `const-string` into the
 * result register and the following `move-result-object` by a `nop`.
 *
 * @return number of patched call sites.
 */
internal fun BytecodePatchContext.foldNoArgStringGetter(
    definingClass: String,
    methodNames: Set<String>,
    value: String,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        var hasRef = false
        for (m in classDef.methods) {
            val impl = m.implementation ?: continue
            for (insn in impl.instructions) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.definingClass == definingClass && ref.name in methodNames && ref.parameterTypes.isEmpty()) { hasRef = true; break }
            }
            if (hasRef) break
        }
        if (!hasRef) return@classDefForEach
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val implementation = method.implementation ?: continue
            val instructions: List<Instruction> = implementation.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: continue
                if (reference.definingClass != definingClass) continue
                if (reference.name !in methodNames) continue
                if (reference.parameterTypes.isNotEmpty()) continue
                if (reference.returnType != "Ljava/lang/String;") continue

                val next = instructions.getOrNull(index + 1)
                if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                    val resultRegister = (next as OneRegisterInstruction).registerA
                    method.replaceInstruction(index, constString(resultRegister, value))
                    method.replaceInstruction(index + 1, "nop")
                    patched++
                }
            }
        }
    }
    return patched
}

/**
 * Folds a no-argument `int` getter (e.g. `TelephonyManager.getDataState`) into a
 * constant [value]. The invoke is replaced by a `const/4`/`const/16` into the
 * result register and the following `move-result` by a `nop`.
 *
 * @return number of patched call sites.
 */
internal fun BytecodePatchContext.foldNoArgIntGetter(
    definingClass: String,
    methodNames: Set<String>,
    value: Int,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        var hasRef = false
        for (m in classDef.methods) {
            val impl = m.implementation ?: continue
            for (insn in impl.instructions) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.definingClass == definingClass && ref.name in methodNames && ref.parameterTypes.isEmpty()) { hasRef = true; break }
            }
            if (hasRef) break
        }
        if (!hasRef) return@classDefForEach
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val implementation = method.implementation ?: continue
            val instructions: List<Instruction> = implementation.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: continue
                if (reference.definingClass != definingClass) continue
                if (reference.name !in methodNames) continue
                if (reference.parameterTypes.isNotEmpty()) continue
                if (reference.returnType != "I") continue

                val next = instructions.getOrNull(index + 1)
                val resultRegister = (next as? OneRegisterInstruction)?.registerA
                if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                    val const = if (value in -8..7) {
                        "const/4 v$resultRegister, $value"
                    } else {
                        "const/16 v$resultRegister, $value"
                    }
                    method.replaceInstruction(index, const)
                    method.replaceInstruction(index + 1, "nop")
                    patched++
                }
            }
        }
    }
    return patched
}

/**
 * Redirects `Locale.getDefault()` to `Locale.forLanguageTag([tag])` so the call
 * returns the chosen locale everywhere. The original invoke is rewritten as:
 *   const-string vR, "[tag]"
 *   invoke-static {vR}, Locale->forLanguageTag:(String)Locale   (move-result stays)
 * The existing `move-result-object vR` already follows and now reads the new
 * locale, so no extra nop is needed.
 *
 * @return number of patched call sites.
 */
internal fun BytecodePatchContext.foldLocaleGetDefault(tag: String): Int {
    var patched = 0
    classDefForEach { classDef ->
        var hasRef = false
        for (m in classDef.methods) {
            val impl = m.implementation ?: continue
            for (insn in impl.instructions) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.definingClass == "Ljava/util/Locale;" && ref.name == "getDefault" && ref.parameterTypes.isEmpty()) { hasRef = true; break }
            }
            if (hasRef) break
        }
        if (!hasRef) return@classDefForEach
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val implementation = method.implementation ?: continue
            val instructions: List<Instruction> = implementation.instructions.toList()

            // The rewrite inserts an extra instruction, which would shift every
            // later index. Collect matches first, then apply them in descending
            // order so earlier insertions never move a not-yet-applied match.
            val matches = mutableListOf<Pair<Int, Int>>() // (invoke index, result register)
            for ((index, instruction) in instructions.withIndex()) {
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: continue
                if (reference.definingClass != "Ljava/util/Locale;") continue
                if (reference.name != "getDefault") continue
                if (reference.parameterTypes.isNotEmpty()) continue
                if (reference.returnType != "Ljava/util/Locale;") continue

                val next = instructions.getOrNull(index + 1)
                if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                    matches.add(index to (next as OneRegisterInstruction).registerA)
                }
            }

            for ((index, resultRegister) in matches.asReversed()) {
                method.replaceInstruction(
                    index,
                    "const-string v$resultRegister, \"${escapeSmali(tag)}\"",
                )
                method.addInstruction(
                    index + 1,
                    "invoke-static {v$resultRegister}, " +
                        "Ljava/util/Locale;->forLanguageTag(Ljava/lang/String;)Ljava/util/Locale;",
                )
                patched++
            }
        }
    }
    return patched
}

/**
 * Folds any `String`-returning getter (regardless of its arguments) into a
 * constant [value]: the invoke becomes a `const-string` into the result
 * register and the following `move-result-object` becomes a `nop`.
 */
internal fun BytecodePatchContext.foldStringGetterConst(
    definingClass: String,
    methodNames: Set<String>,
    value: String,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        var hasRef = false
        for (m in classDef.methods) {
            val impl = m.implementation ?: continue
            for (insn in impl.instructions) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.definingClass == definingClass && ref.name in methodNames && ref.returnType == "Ljava/lang/String;") { hasRef = true; break }
            }
            if (hasRef) break
        }
        if (!hasRef) return@classDefForEach
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val implementation = method.implementation ?: continue
            val instructions: List<Instruction> = implementation.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: continue
                if (reference.definingClass != definingClass) continue
                if (reference.name !in methodNames) continue
                if (reference.returnType != "Ljava/lang/String;") continue

                val next = instructions.getOrNull(index + 1)
                if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                    val resultRegister = (next as OneRegisterInstruction).registerA
                    method.replaceInstruction(index, constString(resultRegister, value))
                    method.replaceInstruction(index + 1, "nop")
                    patched++
                }
            }
        }
    }
    return patched
}

/**
 * Folds any `boolean`-returning getter into [value] (true -> 0x1, false -> 0x0).
 */
internal fun BytecodePatchContext.foldBooleanGetterConst(
    definingClass: String,
    methodNames: Set<String>,
    value: Boolean,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        // prefilter: skip classes that never reference the target definingClass
        var hasRef = false
        for (m in classDef.methods) {
            val impl = m.implementation ?: continue
            for (insn in impl.instructions) {
                val ref = (insn as? ReferenceInstruction)?.reference ?: continue
                if (ref is MethodReference && ref.definingClass == definingClass && ref.name in methodNames) { hasRef = true; break }
            }
            if (hasRef) break
        }
        if (!hasRef) return@classDefForEach
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val implementation = method.implementation ?: continue
            val instructions: List<Instruction> = implementation.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: continue
                if (reference.definingClass != definingClass) continue
                if (reference.name !in methodNames) continue
                if (reference.returnType != "Z") continue

                val next = instructions.getOrNull(index + 1)
                if (next != null && next.opcode == Opcode.MOVE_RESULT) {
                    val resultRegister = (next as OneRegisterInstruction).registerA
                    val const = if (value) {
                        "const/4 v$resultRegister, 0x1"
                    } else {
                        "const/4 v$resultRegister, 0x0"
                    }
                    method.replaceInstruction(index, const)
                    method.replaceInstruction(index + 1, "nop")
                    patched++
                }
            }
        }
    }
    return patched
}

/**
 * Folds any object-returning getter into `null` (const/4 0x0).
 */
internal fun BytecodePatchContext.foldObjectGetterToNull(
    definingClass: String,
    methodNames: Set<String>,
    returnType: String,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        var hasRef = false
        for (m in classDef.methods) {
            val impl = m.implementation ?: continue
            for (insn in impl.instructions) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.definingClass == definingClass && ref.name in methodNames && ref.returnType == returnType) { hasRef = true; break }
            }
            if (hasRef) break
        }
        if (!hasRef) return@classDefForEach
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val implementation = method.implementation ?: continue
            val instructions: List<Instruction> = implementation.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: continue
                if (reference.definingClass != definingClass) continue
                if (reference.name !in methodNames) continue
                if (reference.returnType != returnType) continue

                val next = instructions.getOrNull(index + 1)
                if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                    val resultRegister = (next as OneRegisterInstruction).registerA
                    method.replaceInstruction(index, "const/4 v$resultRegister, 0x0")
                    method.replaceInstruction(index + 1, "nop")
                    patched++
                }
            }
        }
    }
    return patched
}

/**
 * Replaces a getter call with an arbitrary [replacementInvoke] (typically an
 * `invoke-static` with no arguments) that returns the same type, keeping the
 * existing `move-result-object` so the caller receives the new value. Used to
 * swap e.g. `SensorManager.getSensorList()` for `Collections.emptyList()`.
 */
internal fun BytecodePatchContext.replaceGetterWithStaticCall(
    definingClass: String,
    methodNames: Set<String>,
    returnType: String,
    replacementInvoke: String,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        var hasRef = false
        for (m in classDef.methods) {
            val impl = m.implementation ?: continue
            for (insn in impl.instructions) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.definingClass == definingClass && ref.name in methodNames && ref.returnType == returnType) { hasRef = true; break }
            }
            if (hasRef) break
        }
        if (!hasRef) return@classDefForEach
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val implementation = method.implementation ?: continue
            val instructions: List<Instruction> = implementation.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: continue
                if (reference.definingClass != definingClass) continue
                if (reference.name !in methodNames) continue
                if (reference.returnType != returnType) continue

                val next = instructions.getOrNull(index + 1)
                if (next != null && next.opcode == Opcode.MOVE_RESULT_OBJECT) {
                    method.replaceInstruction(index, replacementInvoke)
                    patched++
                }
            }
        }
    }
    return patched
}

/**
 * Replaces every `sget-object vX, <definingClass>-><FIELD>:Ljava/lang/String;`
 * with `const-string vX, "<value>"` for fields present in [values], so an app
 * sees the chosen static string field instead of the real device value.
 *
 * @return number of patched field reads.
 */
internal fun BytecodePatchContext.foldStaticStringField(
    definingClass: String,
    values: Map<String, String>,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        var hasRef = false
        for (m in classDef.methods) {
            val impl = m.implementation ?: continue
            for (insn in impl.instructions) {
                if (insn.opcode != Opcode.SGET_OBJECT) continue
                val ref = (insn as? ReferenceInstruction)?.reference as? FieldReference ?: continue
                if (ref.definingClass == definingClass && ref.type == "Ljava/lang/String;" && ref.name in values) { hasRef = true; break }
            }
            if (hasRef) break
        }
        if (!hasRef) return@classDefForEach
        val mutableClass = mutableClassDefBy(classDef)
        for (method in mutableClass.methods) {
            val implementation = method.implementation ?: continue
            val instructions: List<Instruction> = implementation.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                if (instruction.opcode != Opcode.SGET_OBJECT) continue
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? FieldReference
                        ?: continue
                if (reference.definingClass != definingClass) continue
                if (reference.type != "Ljava/lang/String;") continue
                val value = values[reference.name] ?: continue

                val register = (instruction as? OneRegisterInstruction)?.registerA ?: continue
                method.replaceInstruction(
                    index,
                    "const-string v$register, \"${escapeSmali(value)}\"",
                )
                patched++
            }
        }
    }
    return patched
}
