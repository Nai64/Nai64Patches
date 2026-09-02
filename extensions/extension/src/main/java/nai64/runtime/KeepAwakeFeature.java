package nai64.runtime;

import android.app.Activity;
import android.view.WindowManager;

final class KeepAwakeFeature implements RuntimeOverlayFeature {
    private static final int FLAG_KEEP_SCREEN_ON = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON;
    @Override public String label() { return "Keep screen awake"; }
    @Override public String description() { return "Prevent the screen from turning off while this Activity is visible."; }
    @Override public boolean initiallyEnabled(Activity activity, int flags, int systemUi) { return (flags & FLAG_KEEP_SCREEN_ON) != 0; }
    @Override public void setEnabled(Activity activity, boolean enabled, int flags, int systemUi) {
        if (enabled) activity.getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        else restore(activity, flags, systemUi);
    }
    @Override public void restore(Activity activity, int flags, int systemUi) {
        if ((flags & FLAG_KEEP_SCREEN_ON) != 0) activity.getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        else activity.getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
    }
}
