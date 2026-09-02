package nai64.runtime;

import android.app.Activity;

final class ScreenshotsFeature implements RuntimeOverlayFeature {
    private static final int FLAG_SECURE = 0x2000;
    @Override public String label() { return "Allow screenshots"; }
    @Override public boolean initiallyEnabled(Activity activity, int flags, int systemUi) { return (flags & FLAG_SECURE) == 0; }
    @Override public void setEnabled(Activity activity, boolean enabled, int flags, int systemUi) {
        if (enabled) activity.getWindow().clearFlags(FLAG_SECURE);
        else if ((flags & FLAG_SECURE) != 0) activity.getWindow().addFlags(FLAG_SECURE);
        else activity.getWindow().clearFlags(FLAG_SECURE);
    }
}
