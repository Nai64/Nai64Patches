package patches.universal.privacy

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.stringOption
import java.util.logging.Logger
import patches.universal.misc.foldIntGetterConst
import patches.universal.misc.foldStringGetterConst

@Suppress("unused")
val spoofTrackingIdsPatch = bytecodePatch(
    name = "Spoof Tracking IDs",
    description = "Spoofs the App Set ID and Firebase Instance ID so reinstalls cannot be linked to your device",
    default = false,
) {
    category("Privacy")
    val appSetId by stringOption(
        title = "App Set ID",
        default = "00000000-0000-0000-0000-000000000000",
        key = "spoofAppSetId",
        description = "UUID reported as the App Set ID.",
    )
    val firebaseId by stringOption(
        title = "Firebase ID",
        default = "0000000000000000000000",
        key = "spoofFirebaseId",
        description = "ID reported as the Firebase Instance ID.",
    )
    val spoofScope by booleanOption(
        title = "Spoof App Set scope",
        default = true,
        key = "spoofAppSetScope",
        description = "Report App Set scope as app (1) instead of developer (2).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        val appSet = appSetId?.trim().takeUnless { it.isNullOrEmpty() } ?: "00000000-0000-0000-0000-000000000000"
        patched += foldStringGetterConst(
            "Lcom/google/android/gms/appset/AppSetIdInfo;",
            setOf("getId"),
            appSet,
        )
        if (spoofScope == true) {
            patched += foldIntGetterConst(
                "Lcom/google/android/gms/appset/AppSetIdInfo;",
                setOf("getScope"),
                1,
            )
        }
        val fid = firebaseId?.trim().takeUnless { it.isNullOrEmpty() } ?: "0000000000000000000000"
        patched += foldStringGetterConst(
            "Lcom/google/firebase/iid/FirebaseInstanceId;",
            setOf("getId", "getToken"),
            fid,
        )
        if (patched > 0) logger.info("Spoofed tracking IDs at $patched call site(s)")
        else logger.warning("No App Set or Firebase ID calls found. No changes applied.")
    }
}
