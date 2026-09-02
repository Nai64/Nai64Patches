package nai64.runtime;

import android.app.Activity;

final class FullscreenFeature implements RuntimeOverlayFeature {
    private static final int FULLSCREEN_FLAGS = 0x1706;
    @Override public String label() { return "Fullscreen"; }
    @Override public boolean initiallyEnabled(Activity activity, int flags, int systemUi) { return (systemUi & 0x4) != 0; }
    @Override public void setEnabled(Activity activity, boolean enabled, int flags, int systemUi) {
        activity.getWindow().getDecorView().setSystemUiVisibility(enabled ? systemUi | FULLSCREEN_FLAGS : systemUi);
    }
}
