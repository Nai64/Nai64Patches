package patches.universal.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import java.util.logging.Logger

// Shared injection helpers for the Google Services killer patches. Every
// helper matches ALL overloads (parameters intentionally unconstrained on the
// fingerprints) via matchAll with a methodOrNull fallback, skips
// implementation-less and wrong-return-type matches, and isolates failures
// per match so one bad method never breaks the rest.

internal fun BytecodePatchContext.gmsInjectAll(
    logger: Logger,
    fingerprint: Fingerprint,
    label: String,
    allowedReturns: Set<String>,
    smali: String,
): Int {
    var patched = 0
    fun handle(method: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod): Boolean {
        return try {
            if (method.implementation == null) false
            else if (method.returnType !in allowedReturns) false
            else {
                method.addInstructions(0, smali)
                true
            }
        } catch (_: Exception) {
            false
        }
    }
    try {
        val matches: List<Match> = try {
            with(this@gmsInjectAll) { fingerprint.matchAll() }
        } catch (_: Exception) {
            emptyList()
        }
        for (m in matches) {
            try {
                if (handle(m.method)) patched++
            } catch (_: Exception) {}
        }
        if (patched > 0) {
            logger.info("$label: patched $patched method(s)")
            return patched
        }
    } catch (_: Exception) {}
    try {
        val single = fingerprint.methodOrNull
        if (single != null && handle(single)) {
            logger.info("$label: patched 1 method (exact)")
            return 1
        }
    } catch (_: Exception) {}
    return 0
}

internal fun BytecodePatchContext.gmsNoopAll(logger: Logger, fingerprint: Fingerprint, label: String): Int =
    gmsInjectAll(logger, fingerprint, label, setOf("V"), "return-void")

internal fun BytecodePatchContext.gmsZeroAll(logger: Logger, fingerprint: Fingerprint, label: String): Int =
    gmsInjectAll(logger, fingerprint, label, setOf("I", "Z", "B", "S"), "const/4 v0, 0x0\nreturn v0")

internal fun BytecodePatchContext.gmsOneAll(logger: Logger, fingerprint: Fingerprint, label: String): Int =
    gmsInjectAll(logger, fingerprint, label, setOf("I", "Z", "B", "S"), "const/4 v0, 0x1\nreturn v0")

internal fun BytecodePatchContext.gmsNullAll(logger: Logger, fingerprint: Fingerprint, label: String): Int {
    var patched = 0
    fun handle(method: app.morphe.patcher.util.proxy.mutableTypes.MutableMethod): Boolean {
        return try {
            if (method.implementation == null) false
            else if (method.returnType.length < 2 || method.returnType[0] != 'L' && method.returnType[0] != '[') false
            else {
                method.addInstructions(0, "const/4 v0, 0x0\nreturn-object v0")
                true
            }
        } catch (_: Exception) {
            false
        }
    }
    try {
        val matches: List<Match> = try {
            with(this@gmsNullAll) { fingerprint.matchAll() }
        } catch (_: Exception) {
            emptyList()
        }
        for (m in matches) {
            try {
                if (handle(m.method)) patched++
            } catch (_: Exception) {}
        }
        if (patched > 0) {
            logger.info("$label: nulled $patched method(s)")
            return patched
        }
    } catch (_: Exception) {}
    return 0
}

internal fun BytecodePatchContext.gmsEmptyStringAll(logger: Logger, fingerprint: Fingerprint, label: String): Int =
    gmsInjectAll(logger, fingerprint, label, setOf("Ljava/lang/String;"), "const-string v0, \"\"\nreturn-object v0")

internal fun BytecodePatchContext.gmsVersionAll(logger: Logger, fingerprint: Fingerprint, label: String): Int =
    gmsInjectAll(logger, fingerprint, label, setOf("I"), "const v0, 0x17D7840\nreturn v0")

private const val TASK_FOR_RESULT_NULL =
    "const/4 v0, 0x0\ninvoke-static {v0}, Lcom/google/android/gms/tasks/Tasks;->forResult(Ljava/lang/Object;)Lcom/google/android/gms/tasks/Task;\nmove-result-object v0\nreturn-object v0"

internal fun BytecodePatchContext.gmsTaskNullAll(logger: Logger, fingerprint: Fingerprint, label: String): Int =
    gmsInjectAll(logger, fingerprint, label, setOf("Lcom/google/android/gms/tasks/Task;"), TASK_FOR_RESULT_NULL)

internal fun BytecodePatchContext.gmsTaskBoolAll(logger: Logger, fingerprint: Fingerprint, label: String, value: Boolean): Int {
    val bit = if (value) "0x1" else "0x0"
    val smali = "const/4 v0, $bit\ninvoke-static {v0}, Ljava/lang/Boolean;->valueOf(Z)Ljava/lang/Boolean;\nmove-result-object v0\n" + TASK_FOR_RESULT_NULL.replaceFirst("const/4 v0, 0x0\n", "")
    return gmsInjectAll(logger, fingerprint, label, setOf("Lcom/google/android/gms/tasks/Task;"), smali)
}
