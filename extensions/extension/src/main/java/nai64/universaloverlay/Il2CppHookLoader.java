package nai64.universaloverlay;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Installs the IL2CPP hook library into a patched app.
 *
 * Unity keeps its native libraries in an ABI split rather than the base APK,
 * so a library dropped into the base's {@code lib/} directory is never loaded.
 * The library therefore travels as an asset and is copied into the app's
 * private files directory, which {@link System#load} accepts by path.
 *
 * The hook targets are data rather than compiled-in code: the patcher writes
 * them next to the library as {@code il2cpp-hooks.txt} and they are handed to
 * the native side here. Adding a target is therefore a patch change, not a
 * native rebuild, and one library serves every IL2CPP game.
 *
 * Called from an activity's {@code onCreate}, so every failure is swallowed: a
 * missing asset or a rejected load must never stop the game from starting.
 */
public final class Il2CppHookLoader {
    private static final String TAG = "morphe-il2cpp";
    private static final String LIB_ASSET = "morphe/libmorpheil2cpp.so";
    private static final String RULES_DIR = "morphe/il2cpp-hooks";
    private static final String LIB_NAME = "morphe-il2cpp.so";
    private static final int COPY_BUFFER = 16 * 1024;

    private static volatile boolean done;

    private Il2CppHookLoader() {
    }

    /** Loads the library and applies the bundled rules. Safe to call repeatedly. */
    public static void install(Context context) {
        if (done || context == null) return;
        synchronized (Il2CppHookLoader.class) {
            if (done) return;
            Context app = context.getApplicationContext();
            try {
                File lib = extract(app, LIB_ASSET, LIB_NAME);
                if (lib == null) return;
                System.load(lib.getAbsolutePath());
                String[] rules = readRules(app);
                if (rules.length == 0) {
                    Log.w(TAG, "no hook rules, library loaded but idle");
                } else {
                    nativeSetRules(rules);
                }
                done = true;
                Log.i(TAG, "hook library loaded with " + rules.length + " rule(s)");
            } catch (Throwable t) {
                Log.w(TAG, "hook library not loaded: " + t);
            }
        }
    }

    /** Defined in libmorpheil2cpp.so; stores the rules for the background hook. */
    private static native void nativeSetRules(String[] rules);

    private static File extract(Context context, String asset, String name) {
        File dir = new File(context.getFilesDir(), "morphe");
        if (!dir.isDirectory() && !dir.mkdirs()) return null;
        File out = new File(dir, name);

        long expected = assetSize(context, asset);
        // Skip the copy when the file is already the expected size, so a
        // restart does not re-extract on every activity launch.
        if (out.isFile() && out.length() == expected && expected > 0) return out;

        try (InputStream in = context.getAssets().open(asset);
             OutputStream sink = new FileOutputStream(out)) {
            byte[] buffer = new byte[COPY_BUFFER];
            int read;
            while ((read = in.read(buffer)) > 0) {
                sink.write(buffer, 0, read);
            }
            sink.flush();
        } catch (IOException e) {
            Log.w(TAG, "could not extract " + asset + ": " + e);
            return null;
        }
        return out;
    }

    private static long assetSize(Context context, String asset) {
        try (InputStream in = context.getAssets().open(asset)) {
            return in.available();
        } catch (IOException e) {
            return 0L;
        }
    }

    /**
     * Reads every rule file in the rules directory.
     *
     * Each patch that wants a hook writes its own file rather than one shared
     * list, so two patches can be enabled together without the second
     * overwriting the first.
     */
    private static String[] readRules(Context context) {
        List<String> out = new ArrayList<>();
        String[] names;
        try {
            names = context.getAssets().list(RULES_DIR);
        } catch (IOException e) {
            Log.w(TAG, "no rule directory: " + e);
            return new String[0];
        }
        if (names == null || names.length == 0) {
            Log.w(TAG, "rule directory is empty");
            return new String[0];
        }
        // Sorted so the applied order is stable between runs.
        java.util.Arrays.sort(names);
        for (String name : names) {
            if (name.endsWith(".txt")) {
                readRuleFile(context, RULES_DIR + "/" + name, out);
            }
        }
        return out.toArray(new String[0]);
    }

    private static void readRuleFile(Context context, String asset, List<String> out) {
        try (InputStream in = context.getAssets().open(asset);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    out.add(trimmed);
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "could not read " + asset + ": " + e);
        }
    }
}