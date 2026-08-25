package patches.universal.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch
import java.util.logging.Logger

@Suppress("unused")
val bypassHostnameVerificationPatch = bytecodePatch(
    name = "Bypass Hostname Verification",
    description = "Makes any HostnameVerifier.verify(String, SSLSession) always return true, accepting any certificate hostname without errors.",
    default = false,
) {
    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var patched = 0
        classDefForEach { classDef ->
            if (classDef.interfaces.none { it == "Ljavax/net/ssl/HostnameVerifier;" }) return@classDefForEach
            val mutableClass = mutableClassDefBy(classDef)
            for (method in mutableClass.methods) {
                val impl = method.implementation ?: continue
                if (method.returnType != "Z") continue
                if (method.name != "verify") continue
                if (method.parameterTypes != listOf("Ljava/lang/String;", "Ljavax/net/ssl/SSLSession;")) continue
                method.addInstruction(0, "const/4 v0, 0x1")
                method.addInstruction(1, "return v0")
                patched++
            }
        }
        if (patched > 0) {
            logger.info("Forced $patched HostnameVerifier.verify() call(s) to return true")
        } else {
            logger.warning("No HostnameVerifier implementations found. No changes applied.")
        }
    }
}
