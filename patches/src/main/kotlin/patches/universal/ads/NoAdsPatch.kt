package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val noAdsPatch = bytecodePatch(
    name = "No Ads",
    description = "Blocks all non-rewarded ads: interstitial, banner, app open, MREC. " +
            "Supports MAX Unity and native MAX.",
    default = false,
) {
    execute {
        var patched = 0

        // ── MAX Unity wrapper ──
        ShowInterstitialFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void"); patched++ }
        ShowAppOpenAdFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void"); patched++ }
        ShowBannerFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void"); patched++ }
        ShowMRecFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void"); patched++ }
        StartBannerAutoRefreshFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void"); patched++ }
        StartMRecAutoRefreshFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void"); patched++ }

        // ── Native MAX (non-Unity) ──
        MaxInterstitialAdShowAdFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void"); patched++ }
        MaxAppOpenAdShowAdFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void"); patched++ }
        MaxAdViewStartAutoRefreshFingerprint.methodOrNull?.let { it.addInstruction(0, "return-void"); patched++ }
    }
}
