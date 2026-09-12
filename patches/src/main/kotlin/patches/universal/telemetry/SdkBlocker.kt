package patches.universal.telemetry

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import com.android.tools.smali.dexlib2.AccessFlags
import patches.universal.ads.util.findMutableMethodOf

/**
 * Early-returns (`return-void` at index 0) every `void` method whose name is in
 * [staticEntries] (for `static` methods) or [instanceEntries] (for instance
 * methods) on classes whose type starts with one of [prefixes].
 *
 * Only `void` methods are touched, so no value flow can break: the call simply
 * does nothing. Constructors are never matched by name sets on purpose.
 *
 * @return per-SDK count of neutralized methods, keyed by SDK label.
 */
internal fun BytecodePatchContext.blockSdkMethods(
    prefixes: Map<String, String>,
    staticEntries: Set<String>,
    instanceEntries: Set<String> = emptySet(),
): Map<String, Int> {
    val hit = mutableMapOf<String, Int>()
    classDefForEach { classDef ->
        val sdkName = prefixes.entries.firstOrNull { classDef.type.startsWith(it.key) }?.value
            ?: return@classDefForEach

        val mutableClass by lazy { mutableClassDefBy(classDef) }
        for (method in classDef.methods) {
            if (method.returnType != "V") continue
            val isStatic = AccessFlags.STATIC.isSet(method.accessFlags)
            if (isStatic && method.name !in staticEntries) continue
            if (!isStatic && method.name !in instanceEntries) continue

            mutableClass.findMutableMethodOf(method).addInstruction(0, "return-void")
            hit[sdkName] = (hit[sdkName] ?: 0) + 1
        }
    }
    return hit
}
