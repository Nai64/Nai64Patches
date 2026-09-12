package patches.universal.graphics

import app.morphe.patcher.patch.bytecodePatch
import patches.universal.misc.foldNoArgIntGetter
import patches.universal.misc.forceIntValue
import java.util.logging.Logger

@Suppress("unused")
val forceDarkModePatch = bytecodePatch(
    name = "Force Dark Mode",
    description = "Makes night-mode checks report dark theme on so apps render their dark theme.",
    default = false,
) {
    category("Graphics")
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += foldNoArgIntGetter(
            definingClass = "Landroid/app/UiModeManager;",
            methodNames = setOf("getNightMode"),
            value = 2,
        )
        patched += foldNoArgIntGetter(
            definingClass = "Landroidx/appcompat/app/AppCompatDelegate;",
            methodNames = setOf("getDefaultNightMode"),
            value = 2,
        )
        patched += forceIntValue(
            targetClass = "Landroidx/appcompat/app/AppCompatDelegate;",
            setters = setOf("setDefaultNightMode"),
            value = 2,
        )
        if (patched > 0) logger.info("Forced $patched night-mode check(s) to dark")
        else logger.warning("No night-mode checks found. No changes applied.")
    }
}
