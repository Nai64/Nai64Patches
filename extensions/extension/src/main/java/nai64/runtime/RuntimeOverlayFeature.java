package nai64.runtime;

import android.app.Activity;

/** Independent runtime control contract; adding a feature must not change the host bridge. */
interface RuntimeOverlayFeature {
    String key();
    String label();
    String description();
    boolean initiallyEnabled(Activity activity, int originalWindowFlags, int originalSystemUi);
    void setEnabled(Activity activity, boolean enabled, int originalWindowFlags, int originalSystemUi);
    void restore(Activity activity, int originalWindowFlags, int originalSystemUi);
}
