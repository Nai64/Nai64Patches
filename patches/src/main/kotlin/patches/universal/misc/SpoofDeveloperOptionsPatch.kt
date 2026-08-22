package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction35c
import com.android.tools.smali.dexlib2.builder.instruction.BuilderInstruction3rc
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.util.logging.Logger

/**
 * Settings provider classes whose `getInt`/`getString` are read by apps to
 * detect developer options, USB debugging and wireless debugging.
 */
private val DEV_OPTIONS_CLASSES = setOf(
    "Landroid/provider/Settings\$Global;",
    "Landroid/provider/Settings\$Secure;",
    "Landroid/provider/Settings\$System;",
)

/**
 * Keys that, when read, reveal developer options / USB / wireless debugging
 * state. Folding these getters to 0 / "" makes the app believe they are off.
 */
private val DEV_OPTIONS_KEYS = setOf(
    "development_settings_enabled",
    "adb_enabled",
    "adb_wifi_enabled",
    "wireless_adb_debugging_enabled",
)

/**
 * Finds every call site in the app that reads one of [keys] from a
 * Settings provider and folds the call (and its `move-result`) into a constant,
 * key-aware (unlike [foldBooleanReturns], which is key-agnostic).
 *
 * This patches the app's *invocations* of the framework getters — the patcher
 * cannot modify framework classes — which covers the standard detection path
 * apps use.
 *
 * @return number of patched call sites.
 */
internal fun BytecodePatchContext.foldSettingsGetters(
    classes: Set<String>,
    keys: Set<String>,
): Int {
    var patched = 0
    classDefForEach { classDef ->
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
                if (reference.name != "getInt" && reference.name != "getString") continue

                val params = reference.parameterTypes
                if (params.size < 2) continue
                if (params[0] != "Landroid/content/ContentResolver;") continue
                if (params[1] != "Ljava/lang/String;") continue

                // The key (name) argument is the second invoke register.
                // 35c layout: B=first, C=second; 3rc (range): start + 1.
                val keyRegister = when (instruction) {
                    is BuilderInstruction35c -> instruction.registerC
                    is BuilderInstruction3rc -> instruction.startRegister + 1
                    else -> continue
                }

                // Walk backwards for the const-string that fills the key register.
                var keyValue: String? = null
                for (j in index - 1 downTo 0) {
                    val prev = instructions[j]
                    if (prev.opcode != Opcode.CONST_STRING) continue
                    val reg = (prev as? OneRegisterInstruction)?.registerA ?: continue
                    if (reg != keyRegister) continue
                    keyValue = ((prev as? ReferenceInstruction)?.reference as? StringReference)?.string
                    break
                }
                if (keyValue !in keys) continue

                val next = instructions.getOrNull(index + 1)
                val resultRegister = (next as? OneRegisterInstruction)?.registerA
                if (next != null &&
                    (next.opcode == Opcode.MOVE_RESULT ||
                        next.opcode == Opcode.MOVE_RESULT_OBJECT ||
                        next.opcode == Opcode.MOVE_RESULT_WIDE)
                ) {
                    val replacement = if (reference.returnType == "I") {
                        "const/16 v$resultRegister, 0x0"
                    } else {
                        "const-string v$resultRegister, \"\""
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

@Suppress("unused")
val spoofDeveloperOptionsPatch = bytecodePatch(
    name = "Spoof Developer Options",
    description =
        "Forces Settings.Global/Settings.Secure getters for developer options, USB " +
            "debugging and wireless debugging to report disabled (0), so apps that refuse " +
            "to run or crash when they detect debugging cannot tell it is on.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)

        val patched = foldSettingsGetters(DEV_OPTIONS_CLASSES, DEV_OPTIONS_KEYS)

        if (patched > 0) {
            logger.info("Spoofed $patched developer-option check(s)")
        } else {
            logger.warning("No developer-option checks found. No changes applied.")
        }
    }
}
