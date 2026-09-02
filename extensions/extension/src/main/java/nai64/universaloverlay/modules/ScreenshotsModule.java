package nai64.universaloverlay.modules;

import android.app.Activity;
import android.view.WindowManager;

public final class ScreenshotsModule extends UniversalOverlayActivityModule {
    @Override public String key() { return "screenshots"; }
    private static final int FLAG_SECURE = WindowManager.LayoutParams.FLAG_SECURE;
    @Override public String label() { return "Allow screenshots"; }
    @Override public String description() { return "Allow screenshots when the host Activity blocks them."; }
    @Override protected boolean readEnabled(Activity activity, int flags, int systemUi) { return (flags & FLAG_SECURE) == 0; }
    @Override protected void applyEnabled(Activity activity, int flags, int systemUi) {
        activity.getWindow().clearFlags(FLAG_SECURE);
    }
    @Override protected void restoreOriginal(Activity activity, int flags, int systemUi) {
        if ((flags & FLAG_SECURE) != 0) activity.getWindow().addFlags(FLAG_SECURE);
        else activity.getWindow().clearFlags(FLAG_SECURE);
    }
}
