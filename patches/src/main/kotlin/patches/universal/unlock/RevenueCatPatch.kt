package patches.universal.unlock

import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.patch.bytecodePatch
import patches.universal.ads.EntitlementInfoIsActiveFingerprint
import java.util.logging.Logger

@Suppress("unused")
val unlockEntitlementsRevenueCatPatch = bytecodePatch(
    name = "Unlock RevenueCat Entitlements",
    description = "Unlock premium features in apps that use RevenueCat.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0

        EntitlementInfoIsActiveFingerprint.methodOrNull?.let {
            it.addInstructions(0, """
                const/4 v0, 0x1
                return v0
            """.trimIndent())
            logger.info("Patched EntitlementInfo.isActive() to always return true")
            patched++
        }

        // Generic RevenueCat fallback: any isActive/isEntitled/hasActive in com/revenuecat that returns Z -> true
        // Covers CustomerInfo, Offerings, EntitlementInfo variants without hardcoding product IDs
        classDefForEach { classDef ->
            val tl = classDef.type.lowercase()
            if (!tl.contains("revenuecat") && !tl.contains("purchases")) return@classDefForEach
            if (tl.contains("okhttp") || tl.contains("ssl")) return@classDefForEach
            val mutableClass = try { mutableClassDefBy(classDef) } catch (_: Exception) { return@classDefForEach }
            for (method in mutableClass.methods) {
                if (method.returnType != "Z") continue
                val n = method.name.lowercase()
                if (n.contains("provider") || n.contains("product") || n.contains("progress")) continue
                val isEntitlementCheck = n.contains("isactive") || n.contains("isentitled") || n.contains("hasactive") || n.contains("ispremium") || n.contains("haspremium") || n.contains("islocked") || n == "isactive" || n == "isentitled"
                if (!isEntitlementCheck) continue
                try {
                    if (method.implementation == null) continue
                    // avoid double-patching EntitlementInfo.isActive already handled
                    method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                    patched++
                } catch (_: Exception) {}
            }
        }

        // Signature verification mode: force Informational/Disabled to avoid Enforced failure
        classDefForEach { classDef ->
            val tl = classDef.type.lowercase()
            if (!tl.contains("revenuecat") || !tl.contains("verification")) return@classDefForEach
            val mutableClass = try { mutableClassDefBy(classDef) } catch (_: Exception) { return@classDefForEach }
            for (method in mutableClass.methods) {
                if (method.returnType != "Z" && !method.returnType.contains("VerificationMode")) continue
                val n = method.name.lowercase()
                if (n.contains("verify") || n.contains("enforced") || n.contains("informational")) {
                    try {
                        if (method.implementation == null) continue
                        // For Z methods (verify), return true; for VerificationMode, return Informational
                        if (method.returnType == "Z") {
                            method.addInstructions(0, "const/4 v0, 0x1\nreturn v0")
                            patched++
                        }
                    } catch (_: Exception) {}
                }
            }
        }

        if (patched > 0) {
            logger.info("Unlock RevenueCat Entitlements patch succeeded ($patched check(s))")
        } else {
            logger.warning("Could not find RevenueCat EntitlementInfo.isActive() method. Skipping.")
        }
    }
}
