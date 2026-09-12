package patches.universal.ui

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldNoArgIntGetter
import patches.universal.misc.forceIntValue
import java.util.logging.Logger

@Suppress("unused")
val forceLightModePatch = bytecodePatch(
    name = "Force Light Mode",
    description = "Makes night-mode checks report light theme so apps render their light theme even in dark mode",
    default = false,
) {
    category("Interface")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += foldNoArgIntGetter(
            definingClass = "Landroid/app/UiModeManager;",
            methodNames = setOf("getNightMode"),
            value = 1,
        )
        patched += foldNoArgIntGetter(
            definingClass = "Landroidx/appcompat/app/AppCompatDelegate;",
            methodNames = setOf("getDefaultNightMode"),
            value = 1,
        )
        patched += forceIntValue(
            targetClass = "Landroidx/appcompat/app/AppCompatDelegate;",
            setters = setOf("setDefaultNightMode"),
            value = 1,
        )
        if (patched > 0) logger.info("Forced $patched night-mode check(s) to light")
        else logger.warning("No night-mode checks found. No changes applied.")
    }
}
