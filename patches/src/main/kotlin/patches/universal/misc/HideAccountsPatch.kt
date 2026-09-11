package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideAccountsPatch = bytecodePatch(
    name = "Hide Accounts",
    description = "Redirects AccountManager.getAccounts/getAccountsByType(...) to an empty Account[] so the app sees no linked accounts.",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Hide") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val patched = replaceArrayGetterWithEmpty(
            "Landroid/accounts/AccountManager;",
            setOf("getAccounts", "getAccountsByType", "getAccountsByTypeAndFeatures"),
            "Landroid/accounts/Account;",
        )
        if (patched > 0) logger.info("Redirected $patched account-list call(s) to empty array")
        else logger.warning("No account-list calls found. No changes applied.")
    }
}
