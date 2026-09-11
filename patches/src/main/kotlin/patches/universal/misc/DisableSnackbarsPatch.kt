package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableSnackbarsPatch = bytecodePatch(
    name = "Disable Snackbars",
    description = "No-ops com.google.android.material.snackbar.Snackbar.show() so Material Design snackbars no longer appear.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Disable") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = noOpVoidInvoke(
            "Lcom/google/android/material/snackbar/Snackbar;",
            setOf("show"),
        )
        if (patched > 0) logger.info("Neutralized $patched Snackbar.show() call(s)")
        else logger.warning("No Snackbar.show() calls found. No changes applied.")
    }
}
