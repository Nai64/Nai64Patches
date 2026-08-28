package patches.universal.misc

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val stripExifLocationPatch = bytecodePatch(
    name = "Strip Exif Location",
    description = "Makes ExifInterface.getLatLong return null so apps cannot read GPS from photos.",
    default = false,
) {
    val strip by booleanOption(
        title = "Strip location",
        default = true,
        key = "stripExifLocation",
        description = "If true, location is stripped; if false, no change.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        if (strip != true) {
            logger.info("Strip disabled, skipping.")
            return@execute
        }
        // Both androidx and framework ExifInterface
        var patched = foldObjectGetterToNull(
            "Landroidx/exifinterface/media/ExifInterface;",
            setOf("getLatLong"),
            "[D",
        )
        patched += foldObjectGetterToNull(
            "Landroid/media/ExifInterface;",
            setOf("getLatLong"),
            "[D",
        )
        // Also cover getGps* methods
        patched += foldObjectGetterToNull(
            "Landroidx/exifinterface/media/ExifInterface;",
            setOf("getGpsLatitude", "getGpsLongitude"),
            "Ljava/lang/String;",
        )
        if (patched > 0) logger.info("Stripped Exif location at $patched call site(s)")
        else logger.warning("No Exif getLatLong call sites found. No changes applied.")
    }
}
