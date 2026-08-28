package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.foldObjectGetterToNull
import patches.universal.misc.replaceGetterWithStaticCall

@Suppress("unused")
val hideQueryIntentsPatch = bytecodePatch(
    name = "Hide Query Intents",
    description = "Makes intent queries appear empty so apps cannot discover other installed apps via intents.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = replaceGetterWithStaticCall(
            "Landroid/content/pm/PackageManager;",
            setOf("queryIntentActivities", "queryIntentServices", "queryBroadcastReceivers"),
            "Ljava/util/List;",
            "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        patched += foldObjectGetterToNull(
            "Landroid/content/pm/PackageManager;",
            setOf("resolveActivity", "resolveService", "resolveContentProvider"),
            "Landroid/content/pm/ResolveInfo;",
        )
        if (patched > 0) logger.info("Hid query intents at $patched call site(s)")
        else logger.warning("No query intent call sites found. No changes applied.")
    }
}
