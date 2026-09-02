package nai64.universaloverlay.modules;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import java.util.Map;
import java.util.WeakHashMap;

/** Best-effort runtime suppression for currently attached Android views. */
public final class DisableHapticsModule extends UniversalOverlayHookModule {
    private final Map<View, Boolean> originalStates = new WeakHashMap<>();
    @Override public String key() { return "disableHaptics"; }
    @Override public String label() { return "Disable haptic feedback / vibrations"; }
    @Override public String description() { return "Disable haptic feedback on currently attached app views."; }
    @Override protected boolean readEnabled(Activity activity, int flags, int systemUi) { return false; }
    @Override protected void applyEnabled(Activity activity, int flags, int systemUi) {
        originalStates.clear();
        visit(activity.getWindow().getDecorView());
    }
    @Override protected void restoreOriginal(Activity activity, int flags, int systemUi) {
        restore(activity.getWindow().getDecorView());
        originalStates.clear();
    }
    private void visit(View view) {
        originalStates.put(view, view.isHapticFeedbackEnabled());
        view.setHapticFeedbackEnabled(false);
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) visit(((ViewGroup) view).getChildAt(i));
    }
    private void restore(View view) {
        Boolean original = originalStates.get(view);
        if (original != null) view.setHapticFeedbackEnabled(original);
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) restore(((ViewGroup) view).getChildAt(i));
    }
}
