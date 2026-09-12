package patches.universal.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object LvlCheckAccessFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/vending/licensing/LicenseChecker;",
    name = "checkAccess",
    returnType = "V",
    parameters = listOf("Lcom/google/android/vending/licensing/ILicenseResultListener;"),
)

// Classic LVL bypass: answer the license check directly with LICENSED (0)
// on the app's own result listener, skipping the Play license server.
private const val LVL_ALLOW_SMALI =
    "const/4 v0, 0x0\nmove-object/from16 v1, p1\ninvoke-interface {v1, v0}, Lcom/google/android/vending/licensing/ILicenseResultListener;->allow(I)V\nreturn-void"

private fun BytecodePatchContext.lvlAllow(logger: Logger): Int {
    var patched = 0
    try {
        val matches: List<Match> = try {
            with(this@lvlAllow) { LvlCheckAccessFingerprint.matchAll() }
        } catch (_: Exception) {
            emptyList()
        }
        for (m in matches) {
            try {
                val method = m.method
                if (method.implementation == null) continue
                if (method.returnType != "V") continue
                method.addInstructions(0, LVL_ALLOW_SMALI)
                patched++
            } catch (_: Exception) {}
        }
    } catch (_: Exception) {}
    if (patched == 0) {
        try {
            val single = LvlCheckAccessFingerprint.methodOrNull
            if (single?.implementation != null && single.returnType == "V") {
                single.addInstructions(0, LVL_ALLOW_SMALI)
                patched = 1
            }
        } catch (_: Exception) {}
    }
    return patched
}

@Suppress("unused")
val lvlBypassPatch = bytecodePatch(
    name = "Bypass Play License Check",
    description = "Answers the Play license check (LVL) with LICENSED without contacting Google. Only affects apps using the legacy licensing library.",
    default = false,
) {
    category("Google Services")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val total = lvlAllow(logger)
        if (total == 0) logger.warning("Bypass Play License Check: no LVL checkAccess found. No changes applied.")
        else logger.info("Bypass Play License Check: bypassed $total method(s)")
    }
}
