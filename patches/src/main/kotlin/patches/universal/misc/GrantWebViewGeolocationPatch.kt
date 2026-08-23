package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val grantWebViewGeolocationPatch = bytecodePatch(
    name = "Grant WebView Geolocation",
    description = "Auto-grants WebView geolocation requests inside onGeolocationPermissionsShowPrompt so location prompts never block the page.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        classDefForEach { classDef ->
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                if (method.returnType != "V") continue
                if (method.name != "onGeolocationPermissionsShowPrompt") continue
                if (method.parameterTypes != listOf(
                        "Ljava/lang/String;",
                        "Landroid/webkit/GeolocationPermissions\$Callback;",
                    )
                ) continue
                method.addInstruction(0, "const/4 v0, 0x1")
                method.addInstruction(1, "const/4 v1, 0x0")
                method.addInstruction(
                    2,
                    "invoke-virtual {p2, p1, v0, v1}, " +
                        "Landroid/webkit/GeolocationPermissions\$Callback;->invoke(Ljava/lang/String;ZZ)V",
                )
                method.addInstruction(3, "return-void")
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
