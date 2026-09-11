package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.util.findMutableMethodOf
import java.util.logging.Logger

@Suppress("unused")
val grantWebViewGeolocationPatch = bytecodePatch(
    name = "Grant WebView Geolocation",
    description = "Auto-grants WebView geolocation requests inside onGeolocationPermissionsShowPrompt so location prompts never block the page.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Permissions") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        classDefForEach { classDef ->
            val mutableClass by lazy { mutableClassDefBy(classDef) }
            for (method in classDef.methods) {
                if (method.returnType != "V") continue
                if (method.name != "onGeolocationPermissionsShowPrompt") continue
                if (method.parameterTypes != listOf(
                        "Ljava/lang/String;",
                        "Landroid/webkit/GeolocationPermissions\$Callback;",
                    )
                ) continue
                val mutableMethod = mutableClass.findMutableMethodOf(method)
                mutableMethod.addInstruction(0, "const/4 v0, 0x1")
                mutableMethod.addInstruction(1, "const/4 v1, 0x0")
                mutableMethod.addInstruction(
                    2,
                    "invoke-virtual {p2, p1, v0, v1}, " +
                        "Landroid/webkit/GeolocationPermissions\$Callback;->invoke(Ljava/lang/String;ZZ)V",
                )
                mutableMethod.addInstruction(3, "return-void")
                patched++
            }
        }
        if (patched > 0) {
            logger.info("Auto-granted geolocation in $patched WebChromeClient(s)")
        } else {
            logger.warning("No onGeolocationPermissionsShowPrompt found. No changes applied.")
        }
    }
}
