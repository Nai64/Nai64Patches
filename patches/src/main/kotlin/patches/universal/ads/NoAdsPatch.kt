package patches.universal.ads

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.patch.bytecodePatch

@Suppress("unused")
val noAdsPatch = bytecodePatch(
    name = "No Ads",
    description = "Blocks all non-rewarded ads: interstitial, banner, app open, MREC banners. " +
            "Prevents ad display without affecting rewarded ad rewards.",
    default = false,
) {
    execute {
        ShowInterstitialFingerprint.method.addInstruction(0, "return-void")
        ShowAppOpenAdFingerprint.method.addInstruction(0, "return-void")
        ShowBannerFingerprint.method.addInstruction(0, "return-void")
        ShowMRecFingerprint.method.addInstruction(0, "return-void")
        StartBannerAutoRefreshFingerprint.method.addInstruction(0, "return-void")
        StartMRecAutoRefreshFingerprint.method.addInstruction(0, "return-void")
    }
}
