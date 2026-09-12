package patches.universal.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.Match
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object MapsInitSimpleFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/maps/MapsInitializer;",
    name = "initialize",
    returnType = "I",
    parameters = listOf("Landroid/content/Context;"),
)

internal object MapsInitCallbackFingerprint : Fingerprint(
    definingClass = "Lcom/google/android/gms/maps/MapsInitializer;",
    name = "initialize",
    returnType = "I",
    parameters = listOf(
        "Landroid/content/Context;",
        "Lcom/google/android/gms/maps/MapsInitializer\$Renderer;",
        "Lcom/google/android/gms/maps/OnMapsSdkInitializedCallback;",
    ),
)

// Report SUCCESS and, for the callback overload, fire
// onMapsSdkInitialized(LATEST) so map loading continues on MicroG.
private const val MAPS_CALLBACK_SMALI =
    "sget-object v0, Lcom/google/android/gms/maps/MapsInitializer" + "$" + "Renderer;->LATEST:Lcom/google/android/gms/maps/MapsInitializer" + "$" + "Renderer;\n" +
        "invoke-interface {p2, v0}, Lcom/google/android/gms/maps/OnMapsSdkInitializedCallback;->onMapsSdkInitialized(Lcom/google/android/gms/maps/MapsInitializer" + "$" + "Renderer;)V\n" +
        "const/4 v0, 0x0\nreturn v0"

private fun BytecodePatchContext.patchMapsInit(logger: Logger): Int {
    var patched = 0
    try {
        val simple: List<Match> = try {
            with(this@patchMapsInit) { MapsInitSimpleFingerprint.matchAll() }
        } catch (_: Exception) {
            emptyList()
        }
        for (m in simple) {
            try {
                val method = m.method
                if (method.implementation == null || method.returnType != "I") continue
                method.addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                patched++
            } catch (_: Exception) {}
        }
        val chained: List<Match> = try {
            with(this@patchMapsInit) { MapsInitCallbackFingerprint.matchAll() }
        } catch (_: Exception) {
            emptyList()
        }
        for (m in chained) {
            try {
                val method = m.method
                if (method.implementation == null || method.returnType != "I") continue
                method.addInstructions(0, MAPS_CALLBACK_SMALI)
                patched++
            } catch (_: Exception) {}
        }
    } catch (_: Exception) {}
    return patched
}

@Suppress("unused")
val fixMapsInitPatch = bytecodePatch(
    name = "Fix Maps on MicroG",
    description = "Reports Maps init SUCCESS and fires the initialized callback with the LATEST renderer so maps keep loading without real Play Services.",
    default = false,
) {
    category("Google Services")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val total = patchMapsInit(logger)
        if (total == 0) logger.warning("Fix Maps on MicroG: no MapsInitializer found. No changes applied.")
        else logger.info("Fix Maps on MicroG: fixed $total method(s)")
    }
}
