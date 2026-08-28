package patches.universal.misc

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val disableAutofillPatch = bytecodePatch(
    name = "Disable Autofill",
    description = "Makes AutofillManager report as disabled so apps stop showing autofill prompts.",
    default = false,
) {
    val disabled by booleanOption(
        title = "Disable",
        default = true,
        key = "disableAutofill",
        description = "Report autofill as disabled (true) or enabled (false).",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        val target = disabled != false
        // isEnabled() -> false, isAutofillSupported() -> false
        var patched = foldBooleanGetterConst(
            "Landroid/view/autofill/AutofillManager;",
            setOf("isEnabled"),
            !target,
        )
        patched += foldBooleanGetterConst(
            "Landroid/view/autofill/AutofillManager;",
            setOf("isAutofillSupported"),
            !target,
        )
        // Also handle hasEnabledAutofillServices()
        patched += foldBooleanGetterConst(
            "Landroid/view/autofill/AutofillManager;",
            setOf("hasEnabledAutofillServices"),
            !target,
        )
        if (patched > 0) logger.info("Disabled autofill at $patched call site(s)")
        else logger.warning("No AutofillManager checks found. No changes applied.")
    }
}
