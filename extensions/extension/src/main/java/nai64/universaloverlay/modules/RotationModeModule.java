package nai64.universaloverlay.modules;

import android.app.Activity;
import android.content.pm.ActivityInfo;

/** Applies a per-Activity orientation request and restores the original request. */
public final class RotationModeModule extends UniversalOverlayActivityModule {
    private int originalOrientation;
    @Override public String key() { return "rotationMode"; }
    @Override public String label() { return "Rotation mode"; }
    @Override public String description() { return "Choose system rotation, portrait, or landscape for this Activity."; }
    @Override protected boolean readEnabled(Activity activity, int flags, int systemUi) {
        originalOrientation = activity.getRequestedOrientation();
        return false;
    }
    @Override protected void applyEnabled(Activity activity, int flags, int systemUi) { }
    @Override protected void restoreOriginal(Activity activity, int flags, int systemUi) { activity.setRequestedOrientation(originalOrientation); }
    public int current(Activity activity) { return activity.getRequestedOrientation(); }
    public void apply(Activity activity, int orientation) { activity.setRequestedOrientation(orientation); }
}
