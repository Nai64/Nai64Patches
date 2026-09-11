/*
 * Copyright 2025 Morphe.
 * https://github.com/MorpheApp/morphe-patches-library
 *
 * File-Specific License Notice (GPLv3 Section 7 Terms)
 *
 * This file is part of the Morphe project and is licensed under
 * the GNU General Public License version 3 (GPLv3), with the Additional
 * Terms under Section 7 described in the LICENSE file.
 *
 * https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Section 7b: Notice Preservation
 * -------------------------------
 * This entire comment block must be preserved in all copies,
 * distributions, and derivative works of this file, in both
 * original and modified source forms.
 *
 * Portions of this software are provided "AS IS" by the Morphe software project.
 * Any express or implied warranties, including the implied warranties of
 * merchantability and fitness for a particular purpose, are disclaimed.
 */

package patches.universal.ads.util

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.util.proxy.mutableTypes.MutableClass
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod.Companion.toMutable
import com.android.tools.smali.dexlib2.AccessFlags
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.MethodParameter
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ThreeRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.immutable.ImmutableMethod
import com.android.tools.smali.dexlib2.immutable.ImmutableMethodImplementation
import com.android.tools.smali.dexlib2.util.MethodUtil

/**
 * Find the [MutableMethod] from a given [Method] in a [MutableClass].
 *
 * @param method The [Method] to find.
 * @return The [MutableMethod].
 */
fun MutableClass.findMutableMethodOf(method: MethodReference) = this.methods.first {
    MethodUtil.methodSignaturesMatch(it, method)
}

/**
 * @return The registers used by this instruction.
 */
val Instruction.registersUsed: List<Int>
    get() = when (this) {
        is FiveRegisterInstruction -> {
            when (registerCount) {
                0 -> listOf()
                1 -> listOf(registerC)
                2 -> listOf(registerC, registerD)
                3 -> listOf(registerC, registerD, registerE)
                4 -> listOf(registerC, registerD, registerE, registerF)
                else -> listOf(registerC, registerD, registerE, registerF, registerG)
            }
        }

        is ThreeRegisterInstruction -> listOf(registerA, registerB, registerC)
        is TwoRegisterInstruction -> listOf(registerA, registerB)
        is OneRegisterInstruction -> listOf(registerA)
        is RegisterRangeInstruction -> (startRegister until (startRegister + registerCount)).toList()
        else -> emptyList()
    }

/**
 * @return The number of registers for all parameters, including p0.
 * This includes 2 registers for each wide parameter.
 */
val Method.numberOfParameterRegisters: Int
    get() {
        var count = 0

        if (!AccessFlags.STATIC.isSet(accessFlags)) {
            count += 1
        }

        for (param in parameters) {
            count += when (param.type) {
                "J", "D" -> 2   // wide
                else -> 1       // normal
            }
        }

        return count
    }

/**
 * @return The number of parameter registers, including p0 as 'this' if method is not static.
 *   This differs from [numberOfParameterRegisters] in that long/double parameters are counted only once each.
 */
val Method.numberOfParameterRegistersLogical: Int
    get() = parameters.count() + if (AccessFlags.STATIC.isSet(accessFlags)) {
        0
    } else {
        1
    }

/**
 * @return The actual register number of p0 for this method.
 * Throws if the method has no implementation.
 */
val Method.p0Register: Int
    get() {
        val impl = implementation ?: throw IllegalStateException("Method has no implementation: $this")
        var paramRegs = 0

        for (type in this.parameterTypes) {
            paramRegs += if (type == "J" || type == "D") 2 else 1
        }

        if (!AccessFlags.STATIC.isSet(this.accessFlags)) {
            paramRegs += 1
        }

        return impl.registerCount - paramRegs
    }

/**
 * Adapted from BiliRoamingX:
 * https://github.com/BiliRoamingX/BiliRoamingX/blob/ae58109f3acdd53ec2d2b3fb439c2a2ef1886221/patches/src/main/kotlin/app/revanced/patches/bilibili/utils/Extenstions.kt#L51
 *
 * Additional registers effectively take the place of the pX parameters (p0, p1, p2, etc.)
 * and contain the original contents of the method parameters.
 * Added registers always start at index: `originalMethod.implementation!!.registerCount` of the
 * original uncloned method.
 *
 * **Fingerprint match indexes will be increased positively by
 * [numberOfParameterRegistersLogical] of the cloned method**.
 */
fun Method.cloneMutable(
    name: String = this.name,
    accessFlags: Int = this.accessFlags,
    parameters: List<MethodParameter> = this.parameters,
    returnType: String = this.returnType,
    additionalRegisters: Int = 0,
): MutableMethod {
    check(additionalRegisters >= 0) {
        "Additional registers cannot be negative"
    }

    val implementationExists = implementation != null
    val oldFirstParameterRegister = if (implementationExists) p0Register else 0

    val clonedImplementation = implementation?.let {
        ImmutableMethodImplementation(
            it.registerCount + additionalRegisters,
            it.instructions,
            it.tryBlocks,
            it.debugItems,
        )
    }

    return ImmutableMethod(
        definingClass,
        name,
        parameters,
        returnType,
        accessFlags,
        annotations,
        hiddenApiRestrictions,
        clonedImplementation
    ).toMutable().apply {
        var insertIndex = 0
        var addedInstructions = 0
        val isNotStatic = !AccessFlags.STATIC.isSet(accessFlags)

        if (implementationExists && additionalRegisters > 0 && (parameters.isNotEmpty() || isNotStatic)) {
            var destReg = oldFirstParameterRegister
            var pReg = 0

            // Handle `this`.
            if (isNotStatic) {
                addInstructions(insertIndex++, "move-object/from16 v$destReg, p0")
                addedInstructions++
                destReg += 1
                pReg += 1
            }

            // Handle method parameters.
            for (parameter in parameters) {
                val opcode = when (parameter.type) {
                    "J", "D" -> "move-wide/from16"
                    else -> {
                        if (parameter.type.startsWith('L') || parameter.type.startsWith('[')) {
                            "move-object/from16"
                        } else {
                            "move/from16"
                        }
                    }
                }

                addInstructions(insertIndex++, "$opcode v$destReg, p$pReg")
                addedInstructions++

                val width = if (opcode.startsWith("move-wide")) 2 else 1
                destReg += width
                pReg += width
            }

            if (addedInstructions != numberOfParameterRegistersLogical) {
                throw IllegalStateException(
                    "Added instructions do not match additional registers " +
                            "addedInstructions: $addedInstructions " +
                            "numberOfParameterRegistersLogical: $numberOfParameterRegistersLogical"
                )
            }
        }
    }
}

/**
 * Additional registers effectively take the place of the pX parameters (p0, p1, p2, etc.)
 * and contain the original contents of the method parameters.
 * Added registers always start at index: `originalMethod.implementation!!.registerCount` of the
 * original uncloned method.
 *
 * **Fingerprint match indexes will be increased positively by [numberOfParameterRegistersLogical]**.
 */
fun Method.cloneParameters(mutableClass : MutableClass) : MutableMethod {
    check (!AccessFlags.STATIC.isSet(accessFlags) || parameters.isNotEmpty()) {
        "Static methods have no parameter registers to preserve"
    }

    val clonedMethod = cloneMutable(
        additionalRegisters = numberOfParameterRegisters
    )

    // Replace existing method with cloned with more registers.
    mutableClass.methods.apply {
        remove(this@cloneParameters)
        add(clonedMethod)
    }

    return clonedMethod
}

/**
 * Additional registers effectively take the place of the pX parameters (p0, p1, p2, etc.)
 * and contain the original contents of the method parameters.
 * Added registers always start at index: `originalMethod.implementation!!.registerCount` of the
 * original uncloned method.
 *
 * **Fingerprint match indexes will be increased positively by [numberOfParameterRegistersLogical]**.
 */
context(patchContext: BytecodePatchContext)
fun Method.cloneParameters() = cloneParameters(
    patchContext.mutableClassDefBy(definingClass)
)
