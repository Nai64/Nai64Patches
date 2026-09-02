package nai64.universaloverlay.modules;

import android.app.Activity;

/** Safe base for Activity modules; subclasses provide only Activity-specific operations. */
public abstract class UniversalOverlayActivityModule implements UniversalOverlayModule {
    protected UniversalOverlayActivityModule() { }

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
