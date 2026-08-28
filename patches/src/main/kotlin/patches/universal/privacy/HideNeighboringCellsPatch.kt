package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.replaceGetterWithStaticCall
import java.util.logging.Logger

@Suppress("unused")
val hideNeighboringCellsPatch = bytecodePatch(
    name = "Hide Neighboring Cells",
    description = "Makes neighboring cell info appear empty so apps cannot read nearby towers.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = replaceGetterWithStaticCall(
            "Landroid/telephony/TelephonyManager;",
            setOf("getNeighboringCellInfo"),
            "Ljava/util/List;",
            "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        if (patched > 0) logger.info("Hid neighboring cells at $patched call site(s)")
        else logger.warning("No getNeighboringCellInfo call sites found. No changes applied.")
    }
}
