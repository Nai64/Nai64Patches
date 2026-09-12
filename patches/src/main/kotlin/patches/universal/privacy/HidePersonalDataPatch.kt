package patches.universal.privacy

import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import patches.universal.ads.util.findMutableMethodOf
import java.util.logging.Logger

/**
 * Nulls only the `ContentResolver.query()` call sites that read a sensitive
 * provider. A site matches when a `const-string` in the preceding window
 * contains one of [uriKeywords] (string-built URIs such as
 * "content://sms") or an `sget-object` loads a `CONTENT_URI` field owned by
 * one of [contractClasses] (e.g. `MediaStore.Images.Media`).
 */
private fun BytecodePatchContext.nullProviderQueries(
    uriKeywords: Set<String>,
    contractClasses: Set<String>,
): Int {
    var patched = 0
    classDefForEach { classDef ->
        var hasRef = false
        for (m in classDef.methods) {
            val impl = m.implementation ?: continue
            for (insn in impl.instructions) {
                val ref = (insn as? ReferenceInstruction)?.reference as? MethodReference ?: continue
                if (ref.definingClass == "Landroid/content/ContentResolver;" &&
                    ref.name == "query" &&
                    ref.returnType == "Landroid/database/Cursor;"
                ) {
                    hasRef = true
                    break
                }
            }
            if (hasRef) break
        }
        if (!hasRef) return@classDefForEach
        val mutableClass by lazy { mutableClassDefBy(classDef) }
        for (method in classDef.methods) {
            val mutableMethod by lazy { mutableClass.findMutableMethodOf(method) }
            val implementation = method.implementation ?: continue
            // Snapshot; one-for-one replacements keep indices valid.
            val instructions = implementation.instructions.toList()
            for ((index, instruction) in instructions.withIndex()) {
                val reference =
                    (instruction as? ReferenceInstruction)?.reference as? MethodReference
                        ?: continue
                if (reference.definingClass != "Landroid/content/ContentResolver;") continue
                if (reference.name != "query") continue
                if (reference.returnType != "Landroid/database/Cursor;") continue

                val next = instructions.getOrNull(index + 1)
                if (next == null || next.opcode != Opcode.MOVE_RESULT_OBJECT) continue
                val resultRegister = (next as OneRegisterInstruction).registerA

                var matches = false
                var j = index - 1
                var scanned = 0
                while (j >= 0 && scanned < 12) {
                    val prev = instructions[j]
                    if (prev.opcode == Opcode.CONST_STRING) {
                        val value = ((prev as? ReferenceInstruction)?.reference as? StringReference)
                            ?.string.orEmpty().lowercase()
                        if (uriKeywords.any { value.contains(it) }) {
                            matches = true
                            break
                        }
                    } else if (prev.opcode == Opcode.SGET_OBJECT) {
                        val owner = ((prev as? ReferenceInstruction)?.reference as? FieldReference)
                            ?.definingClass.orEmpty()
                        if (contractClasses.any { owner.contains(it) }) {
                            matches = true
                            break
                        }
                    }
                    j--
                    scanned++
                }
                if (!matches) continue

                mutableMethod.replaceInstruction(index, "const/4 v$resultRegister, 0x0")
                mutableMethod.replaceInstruction(index + 1, "nop")
                patched++
            }
        }
    }
    return patched
}

@Suppress("unused")
val hidePersonalDataPatch = bytecodePatch(
    name = "Hide Personal Data",
    description = "Blocks contacts, call log, SMS, calendar and photo reads by emptying sensitive provider queries",
    default = false,
) {
    category("Privacy")
    val hideContacts by booleanOption(
        title = "Hide contacts",
        default = true,
        key = "hideContactsData",
        description = "Empty queries against the contacts provider.",
    )
    val hideCallLog by booleanOption(
        title = "Hide call log",
        default = true,
        key = "hideCallLogData",
        description = "Empty queries against the call log provider.",
    )
    val hideSms by booleanOption(
        title = "Hide SMS",
        default = true,
        key = "hideSmsData",
        description = "Empty queries against the SMS/MMS provider.",
    )
    val hideCalendar by booleanOption(
        title = "Hide calendar",
        default = true,
        key = "hideCalendarData",
        description = "Empty queries against the calendar provider.",
    )
    val hidePhotos by booleanOption(
        title = "Hide photos",
        default = true,
        key = "hidePhotosData",
        description = "Empty queries against the media store provider.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        if (hideContacts == true) {
            patched += nullProviderQueries(
                setOf("contacts", "com.android.contacts"),
                setOf("ContactsContract"),
            )
        }
        if (hideCallLog == true) {
            patched += nullProviderQueries(
                setOf("call_log"),
                setOf("CallLog"),
            )
        }
        if (hideSms == true) {
            patched += nullProviderQueries(
                setOf("sms", "mms", "telephony"),
                setOf("Telephony", "/Sms", "/Mms"),
            )
        }
        if (hideCalendar == true) {
            patched += nullProviderQueries(
                setOf("calendar"),
                setOf("CalendarContract"),
            )
        }
        if (hidePhotos == true) {
            patched += nullProviderQueries(
                setOf("mediastore", "media/external", "dcim"),
                setOf("MediaStore"),
            )
        }
        if (patched > 0) logger.info("Emptied $patched sensitive provider querie(s)")
        else logger.warning("No sensitive provider queries found. No changes applied.")
    }
}
