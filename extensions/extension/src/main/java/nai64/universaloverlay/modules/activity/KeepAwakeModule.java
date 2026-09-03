package nai64.universaloverlay.modules.activity;

import android.app.Activity;
import android.view.WindowManager;
import nai64.universaloverlay.modules.UniversalOverlayActivityModule;

public final class KeepAwakeModule extends UniversalOverlayActivityModule {
    @Override public String key() { return "keepAwake"; }
    private static final int FLAG_KEEP_SCREEN_ON = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
    @Override public String label() { return "Keep screen awake"; }
    @Override public String description() { return "Prevent the screen from turning off while this Activity is visible."; }
    @Override protected boolean readEnabled(Activity activity, int flags, int systemUi) { return (flags & FLAG_KEEP_SCREEN_ON) != 0; }
    @Override protected void applyEnabled(Activity activity, int flags, int systemUi) {
        activity.getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
    }
    @Override protected void restoreOriginal(Activity activity, int flags, int systemUi) {
        if ((flags & FLAG_KEEP_SCREEN_ON) != 0) activity.getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        else activity.getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
    }
}
