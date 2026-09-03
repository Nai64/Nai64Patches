package nai64.universaloverlay.modules.activity;

import android.app.Activity;
import android.media.AudioManager;
import nai64.universaloverlay.modules.UniversalOverlayActivityModule;

/** Best-effort temporary mute for the app's music stream. */
public final class AppAudioMuteModule extends UniversalOverlayActivityModule {
    private static int activeInstances;
    private static int sharedOriginalVolume = -1;
    private boolean applied;
    private int stream = AudioManager.STREAM_MUSIC;
    @Override public String key() { return "appAudioMute"; }
    @Override public String label() { return "App audio mute"; }
    @Override public String description() { return "Temporarily mute the app music stream."; }
    @Override protected boolean readEnabled(Activity activity, int flags, int systemUi) { return false; }
    @Override protected void applyEnabled(Activity activity, int flags, int systemUi) {
        AudioManager audio = (AudioManager) activity.getSystemService(Activity.AUDIO_SERVICE);
        if (audio == null) return;
        if (applied) return;
        if (activeInstances == 0) sharedOriginalVolume = audio.getStreamVolume(stream);
        audio.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, 0);
        activeInstances++;
        applied = true;
    }
    @Override protected void restoreOriginal(Activity activity, int flags, int systemUi) {
        AudioManager audio = (AudioManager) activity.getSystemService(Activity.AUDIO_SERVICE);
        if (audio == null) return;
        if (!applied) return;
        activeInstances = Math.max(0, activeInstances - 1);
        if (activeInstances == 0) {
            audio.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0);
            if (sharedOriginalVolume >= 0) audio.setStreamVolume(stream, sharedOriginalVolume, 0);
            sharedOriginalVolume = -1;
        }
        applied = false;
    }
}
