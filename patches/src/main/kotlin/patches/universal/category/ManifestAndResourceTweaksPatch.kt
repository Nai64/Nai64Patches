package patches.universal.category

import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.resourcePatch
import patches.universal.manifest.*
import patches.universal.telemetry.*
import patches.universal.ui.*
import java.util.logging.Logger

@Suppress("unused")
val manifestAndResourceTweaksPatch = resourcePatch(
    name = "Manifest & Resource Tweaks",
    description = "Grouped settings for small standalone patches. Every option is disabled by default.",
    default = false,
) {
    val allowClearingAppDataEnabled by booleanOption(
        key = "allowClearingAppDataEnabled",
        default = false,
        title = "Allow Clearing App Data",
        description = "Apply the Allow Clearing App Data patch.",
    )
    val allowCleartextTrafficEnabled by booleanOption(
        key = "allowCleartextTrafficEnabled",
        default = false,
        title = "Allow Cleartext Traffic",
        description = "Apply the Allow Cleartext Traffic patch.",
    )
    val classicBackGestureEnabled by booleanOption(
        key = "classicBackGestureEnabled",
        default = false,
        title = "Classic Back Gesture",
        description = "Apply the Classic Back Gesture patch.",
    )
    val clearingSplitMetadataEnabled by booleanOption(
        key = "clearingSplitMetadataEnabled",
        default = false,
        title = "Clearing Split Metadata",
        description = "Apply the Clearing Split Metadata patch.",
    )
    val disableAppBackupEnabled by booleanOption(
        key = "disableAppBackupEnabled",
        default = false,
        title = "Disable App Backup",
        description = "Apply the Disable App Backup patch.",
    )
    val disableAppLinksVerificationEnabled by booleanOption(
        key = "disableAppLinksVerificationEnabled",
        default = false,
        title = "Disable App Links Verification",
        description = "Apply the Disable App Links Verification patch.",
    )
    val disableBatteryOptimizationPromptEnabled by booleanOption(
        key = "disableBatteryOptimizationPromptEnabled",
        default = false,
        title = "Disable Battery Optimization Prompt",
        description = "Apply the Disable Battery Optimization Prompt patch.",
    )
    val disableBootAutoStartEnabled by booleanOption(
        key = "disableBootAutoStartEnabled",
        default = false,
        title = "Disable Boot Auto-Start",
        description = "Apply the Disable Boot Auto-Start patch.",
    )
    val disableHardwareAccelerationEnabled by booleanOption(
        key = "disableHardwareAccelerationEnabled",
        default = false,
        title = "Disable Hardware Acceleration",
        description = "Apply the Disable Hardware Acceleration patch.",
    )
    val disableHeapPointerTaggingEnabled by booleanOption(
        key = "disableHeapPointerTaggingEnabled",
        default = false,
        title = "Disable Heap Pointer Tagging",
        description = "Apply the Disable Heap Pointer Tagging patch.",
    )
    val disablePermissionAutoRevokeEnabled by booleanOption(
        key = "disablePermissionAutoRevokeEnabled",
        default = false,
        title = "Disable Permission Auto-Revoke",
        description = "Apply the Disable Permission Auto-Revoke patch.",
    )
    val enableLargeHeapEnabled by booleanOption(
        key = "enableLargeHeapEnabled",
        default = false,
        title = "Enable Large Heap",
        description = "Apply the Enable Large Heap patch.",
    )
    val ensureInternetPermissionEnabled by booleanOption(
        key = "ensureInternetPermissionEnabled",
        default = false,
        title = "Ensure Internet Permission",
        description = "Apply the Ensure Internet Permission patch.",
    )
    val excludeFromRecentsEnabled by booleanOption(
        key = "excludeFromRecentsEnabled",
        default = false,
        title = "Exclude From Recents",
        description = "Apply the Exclude From Recents patch.",
    )
    val exportAllActivitiesEnabled by booleanOption(
        key = "exportAllActivitiesEnabled",
        default = false,
        title = "Export All Activities",
        description = "Apply the Export All Activities patch.",
    )
    val forceExtractNativeLibsEnabled by booleanOption(
        key = "forceExtractNativeLibsEnabled",
        default = false,
        title = "Force Extract Native Libs",
        description = "Apply the Force Extract Native Libs patch.",
    )
    val forceHardwareAccelerationEnabled by booleanOption(
        key = "forceHardwareAccelerationEnabled",
        default = false,
        title = "Force Hardware Acceleration",
        description = "Apply the Force Hardware Acceleration patch.",
    )
    val forceLeftToRightLayoutEnabled by booleanOption(
        key = "forceLeftToRightLayoutEnabled",
        default = false,
        title = "Force Left-to-Right Layout",
        description = "Apply the Force Left-to-Right Layout patch.",
    )
    val forcePictureInPictureEnabled by booleanOption(
        key = "forcePictureInPictureEnabled",
        default = false,
        title = "Force Picture-in-Picture",
        description = "Apply the Force Picture-in-Picture patch.",
    )
    val forceResizableActivityEnabled by booleanOption(
        key = "forceResizableActivityEnabled",
        default = false,
        title = "Force Resizable Activity",
        description = "Apply the Force Resizable Activity patch.",
    )
    val hideDisplayCutoutEnabled by booleanOption(
        key = "hideDisplayCutoutEnabled",
        default = false,
        title = "Hide Display Cutout",
        description = "Apply the Hide Display Cutout patch.",
    )
    val keepDataOnUninstallEnabled by booleanOption(
        key = "keepDataOnUninstallEnabled",
        default = false,
        title = "Keep Data on Uninstall",
        description = "Apply the Keep Data on Uninstall patch.",
    )
    val keepScreenOnEnabled by booleanOption(
        key = "keepScreenOnEnabled",
        default = false,
        title = "Keep Screen On",
        description = "Apply the Keep Screen On patch.",
    )
    val legacyExternalStorageEnabled by booleanOption(
        key = "legacyExternalStorageEnabled",
        default = false,
        title = "Legacy External Storage",
        description = "Apply the Legacy External Storage patch.",
    )
    val makeAppDebuggableEnabled by booleanOption(
        key = "makeAppDebuggableEnabled",
        default = false,
        title = "Make App Debuggable",
        description = "Apply the Make App Debuggable patch.",
    )
    val optimizeAsGameEnabled by booleanOption(
        key = "optimizeAsGameEnabled",
        default = false,
        title = "Optimize as Game",
        description = "Apply the Optimize as Game patch.",
    )
    val relaxHardwareFeaturesEnabled by booleanOption(
        key = "relaxHardwareFeaturesEnabled",
        default = false,
        title = "Relax Hardware Features",
        description = "Apply the Relax Hardware Features patch.",
    )
    val relaxRequiredLibrariesEnabled by booleanOption(
        key = "relaxRequiredLibrariesEnabled",
        default = false,
        title = "Relax Required Libraries",
        description = "Apply the Relax Required Libraries patch.",
    )
    val relaxSharedLibrariesEnabled by booleanOption(
        key = "relaxSharedLibrariesEnabled",
        default = false,
        title = "Relax Shared Libraries",
        description = "Apply the Relax Shared Libraries patch.",
    )
    val removeAdServicesEntriesEnabled by booleanOption(
        key = "removeAdServicesEntriesEnabled",
        default = false,
        title = "Remove Ad Services Entries",
        description = "Apply the Remove Ad Services Entries patch.",
    )
    val removeAppIconEnabled by booleanOption(
        key = "removeAppIconEnabled",
        default = false,
        title = "Remove App Icon",
        description = "Apply the Remove App Icon patch.",
    )
    val removeBackupRestrictionsEnabled by booleanOption(
        key = "removeBackupRestrictionsEnabled",
        default = false,
        title = "Remove Backup Restrictions",
        description = "Apply the Remove Backup Restrictions patch.",
    )
    val removeCompatibleScreensEnabled by booleanOption(
        key = "removeCompatibleScreensEnabled",
        default = false,
        title = "Remove Compatible Screens",
        description = "Apply the Remove Compatible Screens patch.",
    )
    val removeNetworkSecurityConfigEnabled by booleanOption(
        key = "removeNetworkSecurityConfigEnabled",
        default = false,
        title = "Remove Network Security Config",
        description = "Apply the Remove Network Security Config patch.",
    )
    val setProfileableEnabled by booleanOption(
        key = "setProfileableEnabled",
        default = false,
        title = "Set Profileable",
        description = "Apply the Set Profileable patch.",
    )
    val stripTranslationsEnabled by booleanOption(
        key = "stripTranslationsEnabled",
        default = false,
        title = "Strip Translations",
        description = "Apply the Strip Translations patch.",
    )
    val supportAllScreensEnabled by booleanOption(
        key = "supportAllScreensEnabled",
        default = false,
        title = "Support All Screens",
        description = "Apply the Support All Screens patch.",
    )
    val trustUserCasConfigEnabled by booleanOption(
        key = "trustUserCasConfigEnabled",
        default = false,
        title = "Trust User CAs (Config)",
        description = "Apply the Trust User CAs (Config) patch.",
    )
    val unlockMaxAspectRatioEnabled by booleanOption(
        key = "unlockMaxAspectRatioEnabled",
        default = false,
        title = "Unlock Max Aspect Ratio",
        description = "Apply the Unlock Max Aspect Ratio patch.",
    )
    val unlockRotationEnabled by booleanOption(
        key = "unlockRotationEnabled",
        default = false,
        title = "Unlock Rotation",
        description = "Apply the Unlock Rotation patch.",
    )
    val unmarkTestOnlyEnabled by booleanOption(
        key = "unmarkTestOnlyEnabled",
        default = false,
        title = "Unmark Test Only",
        description = "Apply the Unmark Test Only patch.",
    )
    val vmSafeModeEnabled by booleanOption(
        key = "vmSafeModeEnabled",
        default = false,
        title = "VM Safe Mode",
        description = "Apply the VM Safe Mode patch.",
    )
    val disableFacebookSdkAutoInitEnabled by booleanOption(
        key = "disableFacebookSdkAutoInitEnabled",
        default = false,
        title = "Disable Facebook SDK Auto-Init",
        description = "Apply the Disable Facebook SDK Auto-Init patch.",
    )
    val disableFirebaseAutoInitEnabled by booleanOption(
        key = "disableFirebaseAutoInitEnabled",
        default = false,
        title = "Disable Firebase Auto-Init",
        description = "Apply the Disable Firebase Auto-Init patch.",
    )
    val immersiveFullscreenEnabled by booleanOption(
        key = "immersiveFullscreenEnabled",
        default = false,
        title = "Immersive Fullscreen",
        description = "Apply the Immersive Fullscreen patch.",
    )
    val transparentSystemBarsEnabled by booleanOption(
        key = "transparentSystemBarsEnabled",
        default = false,
        title = "Transparent System Bars",
        description = "Apply the Transparent System Bars patch.",
    )

    execute {
        val logger = Logger.getLogger(this::class.java.name)
        var selected = 0
        if (allowClearingAppDataEnabled == true) {
            allowClearingAppDataPatch.execute(this)
            selected++
        }
        if (allowCleartextTrafficEnabled == true) {
            allowCleartextTrafficPatch.execute(this)
            selected++
        }
        if (classicBackGestureEnabled == true) {
            classicBackGesturePatch.execute(this)
            selected++
        }
        if (clearingSplitMetadataEnabled == true) {
            clearSplitMetadataPatch.execute(this)
            selected++
        }
        if (disableAppBackupEnabled == true) {
            disableAppBackupPatch.execute(this)
            selected++
        }
        if (disableAppLinksVerificationEnabled == true) {
            disableAppLinksVerificationPatch.execute(this)
            selected++
        }
        if (disableBatteryOptimizationPromptEnabled == true) {
            disableBatteryOptimizationPromptPatch.execute(this)
            selected++
        }
        if (disableBootAutoStartEnabled == true) {
            disableBootAutoStartPatch.execute(this)
            selected++
        }
        if (disableHardwareAccelerationEnabled == true) {
            disableHardwareAccelerationPatch.execute(this)
            selected++
        }
        if (disableHeapPointerTaggingEnabled == true) {
            disableHeapPointerTaggingPatch.execute(this)
            selected++
        }
        if (disablePermissionAutoRevokeEnabled == true) {
            disablePermissionAutoRevokePatch.execute(this)
            selected++
        }
        if (enableLargeHeapEnabled == true) {
            enableLargeHeapPatch.execute(this)
            selected++
        }
        if (ensureInternetPermissionEnabled == true) {
            ensureInternetPermissionPatch.execute(this)
            selected++
        }
        if (excludeFromRecentsEnabled == true) {
            excludeFromRecentsPatch.execute(this)
            selected++
        }
        if (exportAllActivitiesEnabled == true) {
            exportAllActivitiesPatch.execute(this)
            selected++
        }
        if (forceExtractNativeLibsEnabled == true) {
            extractNativeLibsPatch.execute(this)
            selected++
        }
        if (forceHardwareAccelerationEnabled == true) {
            forceHardwareAccelerationPatch.execute(this)
            selected++
        }
        if (forceLeftToRightLayoutEnabled == true) {
            forceLeftToRightLayoutPatch.execute(this)
            selected++
        }
        if (forcePictureInPictureEnabled == true) {
            forcePictureInPicturePatch.execute(this)
            selected++
        }
        if (forceResizableActivityEnabled == true) {
            forceResizableActivityPatch.execute(this)
            selected++
        }
        if (hideDisplayCutoutEnabled == true) {
            hideDisplayCutoutPatch.execute(this)
            selected++
        }
        if (keepDataOnUninstallEnabled == true) {
            keepDataOnUninstallPatch.execute(this)
            selected++
        }
        if (keepScreenOnEnabled == true) {
            keepScreenOnPatch.execute(this)
            selected++
        }
        if (legacyExternalStorageEnabled == true) {
            legacyExternalStoragePatch.execute(this)
            selected++
        }
        if (makeAppDebuggableEnabled == true) {
            makeAppDebuggablePatch.execute(this)
            selected++
        }
        if (optimizeAsGameEnabled == true) {
            optimizeAsGamePatch.execute(this)
            selected++
        }
        if (relaxHardwareFeaturesEnabled == true) {
            relaxHardwareFeaturesPatch.execute(this)
            selected++
        }
        if (relaxRequiredLibrariesEnabled == true) {
            relaxRequiredLibrariesPatch.execute(this)
            selected++
        }
        if (relaxSharedLibrariesEnabled == true) {
            relaxSharedLibrariesPatch.execute(this)
            selected++
        }
        if (removeAdServicesEntriesEnabled == true) {
            removeAdServicesEntriesPatch.execute(this)
            selected++
        }
        if (removeAppIconEnabled == true) {
            removeAppIconPatch.execute(this)
            selected++
        }
        if (removeBackupRestrictionsEnabled == true) {
            removeBackupRestrictionsPatch.execute(this)
            selected++
        }
        if (removeCompatibleScreensEnabled == true) {
            removeCompatibleScreensPatch.execute(this)
            selected++
        }
        if (removeNetworkSecurityConfigEnabled == true) {
            removeNetworkSecurityConfigPatch.execute(this)
            selected++
        }
        if (setProfileableEnabled == true) {
            setProfileablePatch.execute(this)
            selected++
        }
        if (stripTranslationsEnabled == true) {
            stripTranslationsPatch.execute(this)
            selected++
        }
        if (supportAllScreensEnabled == true) {
            supportAllScreensPatch.execute(this)
            selected++
        }
        if (trustUserCasConfigEnabled == true) {
            trustUserCasConfigPatch.execute(this)
            selected++
        }
        if (unlockMaxAspectRatioEnabled == true) {
            unlockMaxAspectRatioPatch.execute(this)
            selected++
        }
        if (unlockRotationEnabled == true) {
            unlockRotationPatch.execute(this)
            selected++
        }
        if (unmarkTestOnlyEnabled == true) {
            unmarkTestOnlyPatch.execute(this)
            selected++
        }
        if (vmSafeModeEnabled == true) {
            vmSafeModePatch.execute(this)
            selected++
        }
        if (disableFacebookSdkAutoInitEnabled == true) {
            disableFacebookAutoInitPatch.execute(this)
            selected++
        }
        if (disableFirebaseAutoInitEnabled == true) {
            disableFirebaseAutoInitPatch.execute(this)
            selected++
        }
        if (immersiveFullscreenEnabled == true) {
            immersiveFullscreenPatch.execute(this)
            selected++
        }
        if (transparentSystemBarsEnabled == true) {
            transparentSystemBarsPatch.execute(this)
            selected++
        }
        if (selected == 0) {
            logger.info("Manifest & Resource Tweaks: no options enabled; no changes applied")
        }
    }
}

