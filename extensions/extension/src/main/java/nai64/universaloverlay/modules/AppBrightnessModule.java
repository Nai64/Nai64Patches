package nai64.universaloverlay.modules;

import android.app.Activity;
import android.view.WindowManager;

/** Applies a temporary per-Activity brightness override. */
public final class AppBrightnessModule extends UniversalOverlayActivityModule {
    private float originalBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
    @Override public String key() { return "appBrightness"; }
    @Override public String label() { return "App brightness"; }
    @Override public String description() { return "Adjust this Activity only; the maximum is normal app brightness."; }
    @Override protected boolean readEnabled(Activity activity, int flags, int systemUi) {
        originalBrightness = activity.getWindow().getAttributes().screenBrightness;
        return false;
    }
    @Override protected void applyEnabled(Activity activity, int flags, int systemUi) { }
    @Override protected void restoreOriginal(Activity activity, int flags, int systemUi) { restore(activity); }
    public float current(Activity activity) {
        float value = activity.getWindow().getAttributes().screenBrightness;
        return value < 0f ? 1f : Math.max(0f, Math.min(1f, value));
    }
    public void apply(Activity activity, float brightness) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = Math.max(0f, Math.min(1f, brightness));
        activity.getWindow().setAttributes(params);
    }
    public void restore(Activity activity) {
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = originalBrightness;
        activity.getWindow().setAttributes(params);
    }
}
