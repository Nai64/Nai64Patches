package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeCellInfoEmptyPatch = bytecodePatch(
    name = "Fake Cell Info Empty",
    description = "Makes TelephonyManager.getAllCellInfo() return an empty list so apps cannot fingerprint nearby cell towers.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Fake") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = replaceGetterWithStaticCall(
            "Landroid/telephony/TelephonyManager;",
            setOf("getAllCellInfo"),
            "Ljava/util/List;",
            "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        if (patched > 0) {
            logger.info("Faked cell info at $patched call site(s)")
        } else {
            logger.warning("No getAllCellInfo call sites found. No changes applied.")
        }
    }
}
