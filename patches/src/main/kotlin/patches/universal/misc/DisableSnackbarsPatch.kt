package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableSnackbarsPatch = bytecodePatch(
    // Loaded through a category patch; kept as an executable implementation.
    name = null,
    description = "No-ops com.google.android.material.snackbar.Snackbar.show() so Material Design snackbars no longer appear.",
    default = false,
) {
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
