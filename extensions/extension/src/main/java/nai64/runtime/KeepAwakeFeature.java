package nai64.runtime;

import android.app.Activity;

final class KeepAwakeFeature implements RuntimeOverlayFeature {
    private static final int FLAG_KEEP_SCREEN_ON = 0x80;
    @Override public String label() { return "Keep screen awake"; }
    @Override public boolean initiallyEnabled(Activity activity, int flags, int systemUi) { return (flags & FLAG_KEEP_SCREEN_ON) != 0; }
    @Override public void setEnabled(Activity activity, boolean enabled, int flags, int systemUi) {
        if (enabled) activity.getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        else if ((flags & FLAG_KEEP_SCREEN_ON) != 0) activity.getWindow().addFlags(FLAG_KEEP_SCREEN_ON);
        else activity.getWindow().clearFlags(FLAG_KEEP_SCREEN_ON);
    }
}
