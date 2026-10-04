# The patcher merges these classes into the target APK's dex and the patch
# addresses them by name, so R8 must not rename or strip anything here.
-keep class nai64.universaloverlay.** { *; }
-keep interface nai64.universaloverlay.** { *; }
-keep class dev.nai64.** { *; }

-dontwarn nai64.universaloverlay.**
-dontwarn dev.nai64.**
