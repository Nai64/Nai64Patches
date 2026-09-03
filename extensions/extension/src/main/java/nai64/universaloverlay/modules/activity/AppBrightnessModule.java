package nai64.universaloverlay.modules.activity;

import android.app.Activity;
import android.view.View;
import nai64.universaloverlay.modules.UniversalOverlayActivityModule;

/** Applies a temporary app-content dimming layer without changing system brightness. */
public final class AppBrightnessModule extends UniversalOverlayActivityModule {
    private View dimLayer;
    @Override public String key() { return "appBrightness"; }
    @Override public String label() { return "App brightness"; }
    @Override public String description() { return "Adjust this Activity only; the maximum is normal app brightness."; }
    @Override protected boolean readEnabled(Activity activity, int flags, int systemUi) { return false; }
    @Override protected void applyEnabled(Activity activity, int flags, int systemUi) { }
    @Override protected void restoreOriginal(Activity activity, int flags, int systemUi) { restore(activity); }
    public float current(Activity activity) { return 1f; }
    public void bindDimLayer(View dimLayer) { this.dimLayer = dimLayer; }
    public void apply(Activity activity, float brightness) {
        if (dimLayer == null) return;
        dimLayer.setAlpha(1f - Math.max(0f, Math.min(1f, brightness)));
    }
    public void restore(Activity activity) {
        if (dimLayer != null) dimLayer.setAlpha(0f);
    }
}
