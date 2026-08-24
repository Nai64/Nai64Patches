package patches.bandcamp

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldObjectGetterToNull

@Suppress("unused")
val disableSupportBannersPatch = bytecodePatch(
    name = "Disable Support Banners",
    description = "Bandcamp: hides server-pushed banner dialogs such as update prompts, rate-me requests and announcements.",
    default = false,
) {
    compatibleWith("com.bandcamp.android")

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = foldObjectGetterToNull(
            "Le9/a;",
            setOf("h"),
            "Lcom/bandcamp/android/support/model/Banner;",
        )
        if (patched > 0) logger.info("Neutralized $patched banner getter call(s)")
        else logger.warning("No banner getter call sites found. No changes applied.")
    }
}
