package nai64.universaloverlay.modules;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

/** Best-effort runtime cancellation of animations on currently attached Android views. */
public final class DisableAnimationsModule extends UniversalOverlayHookModule {
    @Override public String key() { return "disableAnimations"; }
    @Override public String label() { return "Disable app animations"; }
    @Override public String description() { return "Cancel animations on currently attached app views."; }
    @Override protected boolean readEnabled(Activity activity, int flags, int systemUi) { return false; }
    @Override protected void applyEnabled(Activity activity, int flags, int systemUi) { visit(activity.getWindow().getDecorView(), true); }
    @Override protected void restoreOriginal(Activity activity, int flags, int systemUi) { visit(activity.getWindow().getDecorView(), false); }
    private static void visit(View view, boolean disabled) {
        if (disabled) { view.clearAnimation(); view.animate().cancel(); }
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) visit(((ViewGroup) view).getChildAt(i), disabled);
    }
}
