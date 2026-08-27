package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val fakeCellInfoEmptyPatch = bytecodePatch(
    name = "Fake Cell Info Empty",
    description = "Makes TelephonyManager.getAllCellInfo() return an empty list so apps cannot fingerprint nearby cell towers.",
    default = false,
) {
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
