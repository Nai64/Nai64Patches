package nai64.runtime;

import android.app.Activity;
import android.view.View;

final class FullscreenFeature implements RuntimeOverlayFeature {
    private static final int FULLSCREEN_FLAGS = View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
    @Override public String label() { return "Fullscreen"; }
    @Override public String description() { return "Hide system bars while preserving the Activity's original UI state."; }
    @Override public boolean initiallyEnabled(Activity activity, int flags, int systemUi) {
        return (systemUi & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
    }
    @Override public void setEnabled(Activity activity, boolean enabled, int flags, int systemUi) {
        if (enabled) activity.getWindow().getDecorView().setSystemUiVisibility(systemUi | FULLSCREEN_FLAGS);
        else restore(activity, flags, systemUi);
    }
    @Override public void restore(Activity activity, int flags, int systemUi) {
        activity.getWindow().getDecorView().setSystemUiVisibility(systemUi);
    }
}
