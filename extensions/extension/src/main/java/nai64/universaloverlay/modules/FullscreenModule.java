package nai64.universaloverlay.modules;

import android.app.Activity;
import android.view.View;

public final class FullscreenModule extends UniversalOverlayActivityModule {
    @Override public String key() { return "fullscreen"; }
    private static final int FULLSCREEN_FLAGS = View.SYSTEM_UI_FLAG_LOW_PROFILE
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
    @Override public String label() { return "Fullscreen"; }
    @Override public String description() { return "Hide system bars while preserving the Activity's original UI state."; }
    @Override protected boolean readEnabled(Activity activity, int flags, int systemUi) {
        return (systemUi & View.SYSTEM_UI_FLAG_FULLSCREEN) != 0;
    }
    @Override protected void applyEnabled(Activity activity, int flags, int systemUi) {
        activity.getWindow().getDecorView().setSystemUiVisibility(systemUi | FULLSCREEN_FLAGS);
    }
    @Override protected void restoreOriginal(Activity activity, int flags, int systemUi) {
        activity.getWindow().getDecorView().setSystemUiVisibility(systemUi);
    }
}
