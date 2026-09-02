package nai64.universaloverlay.modules;

import android.app.Activity;

/**
 * Base for optional runtime hooks that modify behavior associated with an Activity.
 *
 * Hook modules have their own guarded Activity-aware lifecycle. They are separate from activity
 * controls because hooks modify host behavior rather than exposing an Activity setting.
 */
public abstract class UniversalOverlayHookModule implements UniversalOverlayModule {
    protected UniversalOverlayHookModule() { }

    public final boolean initiallyEnabled(Activity activity, int originalWindowFlags, int originalSystemUi) {
        try { return readEnabled(activity, originalWindowFlags, originalSystemUi); }
        catch (RuntimeException ignored) { return false; }
    }

    public final boolean setEnabled(Activity activity, boolean enabled, int originalWindowFlags, int originalSystemUi) {
        try {
            if (enabled) applyEnabled(activity, originalWindowFlags, originalSystemUi);
            else restoreOriginal(activity, originalWindowFlags, originalSystemUi);
            return true;
        } catch (RuntimeException ignored) {
            if (enabled) {
                try { restoreOriginal(activity, originalWindowFlags, originalSystemUi); }
                catch (RuntimeException ignoredRestore) { }
            }
            return false;
        }
    }

    public final boolean restore(Activity activity, int originalWindowFlags, int originalSystemUi) {
        try {
            restoreOriginal(activity, originalWindowFlags, originalSystemUi);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    protected abstract boolean readEnabled(Activity activity, int originalWindowFlags, int originalSystemUi);
    protected abstract void applyEnabled(Activity activity, int originalWindowFlags, int originalSystemUi);
    protected abstract void restoreOriginal(Activity activity, int originalWindowFlags, int originalSystemUi);
}
