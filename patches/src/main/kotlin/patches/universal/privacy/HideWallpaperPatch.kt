package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldObjectGetterToNull

@Suppress("unused")
val hideWallpaperPatch = bytecodePatch(
    name = "Hide Wallpaper",
    description = "Nulls wallpaper reads so apps cannot grab your home or lock screen image",
    default = false,
) {
    category("Privacy")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldObjectGetterToNull(
            "Landroid/app/WallpaperManager;",
            setOf("getDrawable", "getBuiltInDrawable"),
            "Landroid/graphics/drawable/Drawable;",
        )
        if (patched > 0) logger.info("Hid wallpaper at $patched call site(s)")
        else logger.warning("No wallpaper calls found. No changes applied.")
    }
}
