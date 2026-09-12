package patches.universal.gms

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

internal object RemoteConfigFetchFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/remoteconfig/FirebaseRemoteConfig;",
    name = "fetch",
    returnType = "Lcom/google/android/gms/tasks/Task;",
)

internal object RemoteConfigFetchAndActivateFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/remoteconfig/FirebaseRemoteConfig;",
    name = "fetchAndActivate",
    returnType = "Lcom/google/android/gms/tasks/Task;",
)

internal object RemoteConfigActivateFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/remoteconfig/FirebaseRemoteConfig;",
    name = "activate",
    returnType = "Z",
)

@Suppress("unused")
val disableRemoteConfigPatch = bytecodePatch(
    name = "Disable Remote Config Fetch",
    description = "Remote Config fetches complete instantly (fetch resolves empty, activate reports success) so apps run on bundled defaults and server-side kill switches never land.",
    default = false,
) {
    category("Google Services")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var total = 0
        total += gmsTaskNullAll(logger, RemoteConfigFetchFingerprint, "Disable Remote Config Fetch: fetch")
        total += gmsTaskBoolAll(logger, RemoteConfigFetchAndActivateFingerprint, "Disable Remote Config Fetch: fetchAndActivate", true)
        total += gmsOneAll(logger, RemoteConfigActivateFingerprint, "Disable Remote Config Fetch: activate")
        if (total == 0) logger.warning("Disable Remote Config Fetch: no Remote Config methods found. No changes applied.")
        else logger.info("Disable Remote Config Fetch: disabled $total method(s) in total")
    }
}
