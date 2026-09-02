package nai64.universaloverlay;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

/** Process lifecycle adapter that delegates Activity ownership to the runtime entry point. */
final class UniversalOverlayLifecycle implements Application.ActivityLifecycleCallbacks {
    @Override public void onActivityCreated(Activity activity, Bundle state) { }
    @Override public void onActivityStarted(Activity activity) { }
    @Override public void onActivityResumed(Activity activity) { UniversalOverlayRuntime.showActivity(activity); }
    @Override public void onActivityPaused(Activity activity) { }
    @Override public void onActivityStopped(Activity activity) { }
    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) { }
    @Override public void onActivityDestroyed(Activity activity) { UniversalOverlayRuntime.removeActivity(activity); }
}
