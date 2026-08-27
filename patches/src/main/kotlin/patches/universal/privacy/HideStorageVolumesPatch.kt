package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.replaceArrayGetterWithEmpty
import patches.universal.misc.replaceGetterWithStaticCall

@Suppress("unused")
val hideStorageVolumesPatch = bytecodePatch(
    name = "Hide Storage Volumes",
    description = "Makes StorageManager return empty lists so volume enumeration for fingerprint sees nothing",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += replaceGetterWithStaticCall(
            definingClass = "Landroid/os/storage/StorageManager;",
            methodNames = setOf("getStorageVolumes"),
            returnType = "Ljava/util/List;",
            replacementInvoke = "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        patched += replaceArrayGetterWithEmpty(
            targetClass = "Landroid/os/storage/StorageManager;",
            methods = setOf("getVolumeList"),
            elementType = "Landroid/os/storage/StorageVolume;",
        )
        if (patched > 0) logger.info("Hid storage volumes at $patched call site(s)")
        else logger.warning("No StorageManager volume calls found. No changes applied.")
    }
}
