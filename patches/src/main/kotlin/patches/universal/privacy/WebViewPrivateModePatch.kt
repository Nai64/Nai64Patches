package patches.universal.privacy

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.util.findMutableMethodOf
import patches.universal.misc.forceBooleanValue
import java.util.logging.Logger

@Suppress("unused")
val webViewPrivateModePatch = bytecodePatch(
    name = "WebView Private Mode",
    description = "Hardens WebViews: denies geolocation prompts and disables cookies, DOM storage and saved form data",
    default = false,
) {
    category("Privacy")
    val denyGeolocation by booleanOption(
        title = "Deny geolocation",
        default = true,
        key = "denyWebViewGeolocation",
        description = "Disable the geolocation API flag and auto-deny location prompts.",
    )
    val blockCookies by booleanOption(
        title = "Block cookies",
        default = true,
        key = "blockWebViewCookies",
        description = "Reject WebView cookies including file scheme cookies.",
    )
    val disableStorage by booleanOption(
        title = "Disable storage",
        default = true,
        key = "disableWebViewStorage",
        description = "Disable DOM storage, databases and saved form or password data.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        if (denyGeolocation == true) {
            patched += forceBooleanValue(
                "Landroid/webkit/WebSettings;",
                setOf("setGeolocationEnabled"),
                false,
            )
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
                    mutableMethod.addInstruction(0, "const/4 v0, 0x0")
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
        }
        if (blockCookies == true) {
            patched += forceBooleanValue(
                "Landroid/webkit/CookieManager;",
                setOf("setAcceptCookie", "setAcceptFileSchemeCookies"),
                false,
            )
        }
        if (disableStorage == true) {
            patched += forceBooleanValue(
                "Landroid/webkit/WebSettings;",
                setOf("setDomStorageEnabled", "setDatabaseEnabled", "setSaveFormData", "setSavePassword"),
                false,
            )
        }
        if (patched > 0) logger.info("Hardened WebView at $patched call site(s)")
        else logger.warning("No WebView calls found. No changes applied.")
    }
}
