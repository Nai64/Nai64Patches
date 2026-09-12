package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldObjectGetterToNull
import patches.universal.misc.noOpVoidInvoke

@Suppress("unused")
val blockInstallReferrerPatch = bytecodePatch(
    name = "Block Install Referrer",
    description = "Blocks the Play Install Referrer API so apps cannot read which ad or link installed them",
    default = false,
) {
    category("Privacy")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += noOpVoidInvoke(
            "Lcom/android/installreferrer/api/InstallReferrerClient;",
            setOf("startConnection"),
        )
        patched += foldObjectGetterToNull(
            "Lcom/android/installreferrer/api/InstallReferrerClient;",
            setOf("getInstallReferrer"),
            "Lcom/android/installreferrer/api/ReferrerDetails;",
        )
        if (patched > 0) logger.info("Blocked install referrer at $patched call site(s)")
        else logger.warning("No install referrer calls found. No changes applied.")
    }
}
