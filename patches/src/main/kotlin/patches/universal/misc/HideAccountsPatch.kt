package patches.universal.misc

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val hideAccountsPatch = bytecodePatch(
    name = "Hide Accounts",
    description = "Redirects AccountManager.getAccounts/getAccountsByType(...) to an empty Account[] so the app sees no linked accounts.",
    default = false,
) {
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
