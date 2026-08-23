package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

/**
 * Replaces android.webkit.WebSettings.getUserAgentString() with a desktop
 * browser user agent so embedded webviews load the desktop version of sites.
 */
@Suppress("unused")
val spoofWebViewUserAgentPatch = bytecodePatch(
    name = "Spoof WebView User Agent",
    description = "Replaces WebSettings.getUserAgentString() with a desktop browser user agent so embedded webviews load desktop versions of sites.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldStringGetterConst(
            "Landroid/webkit/WebSettings;",
            setOf("getUserAgentString"),
            DESKTOP_USER_AGENT,
        )
        if (patched > 0) {
            logger.info("Spoofed WebView user agent at $patched call site(s)")
        } else {
            logger.warning("No WebSettings.getUserAgentString call sites found. No changes applied.")
        }
    }
}
