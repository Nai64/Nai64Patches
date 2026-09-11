package patches.universal.privacy

import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger
import patches.universal.misc.replaceGetterWithStaticCall

@Suppress("unused")
val hideAccessibilityServicesPatch = bytecodePatch(
    name = "Hide Accessibility Services",
    description = "Makes AccessibilityManager return empty lists so apps cannot detect TalkBack or automation services",
    default = false,
) {
    // Guarded: morphe-patcher < 1.13.0 has no category() and the bundle
    // must still load there (ungrouped) instead of dying on linkage.
    try { category("Privacy") } catch (_: NoSuchMethodError) {}
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        patched += replaceGetterWithStaticCall(
            definingClass = "Landroid/view/accessibility/AccessibilityManager;",
            methodNames = setOf("getEnabledAccessibilityServiceList", "getInstalledAccessibilityServiceList"),
            returnType = "Ljava/util/List;",
            replacementInvoke = "invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;",
        )
        if (patched > 0) logger.info("Hid accessibility services at $patched call site(s)")
        else logger.warning("No accessibility service calls found. No changes applied.")
    }
}
