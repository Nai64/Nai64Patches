package nai64.runtime;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Runtime implementation for the Nai64 overlay.
 *
 * This class is compiled into the extension DEX. The patch generator only injects a bridge call;
 * changes to this file therefore do not alter host Activity fields, listener interfaces, or large
 * generated Smali methods. The implementation deliberately uses platform Views only so it can run
 * in ordinary Android apps, Unity/Godot hosts, and game Activities without AppCompat coupling.
 */
public final class RuntimeOverlayRuntime {
    private static final Map<Activity, Controller> CONTROLLERS = new WeakHashMap<>();
    private static boolean callbacksRegistered;
    private static Config configuration;

    private RuntimeOverlayRuntime() { }

    /** Primary entry point, called once from Application.onCreate(). */
    public static synchronized void install(Application application, String encodedConfig) {
        if (application == null) return;
        configuration = Config.decode(encodedConfig);
        if (!callbacksRegistered) {
            application.registerActivityLifecycleCallbacks(new LifecycleCallbacks());
            callbacksRegistered = true;
        }
    }

    /** Compatibility fallback for APKs where Application.onCreate cannot be resolved. */
    public static synchronized void installActivity(Activity activity, String encodedConfig) {
        if (activity == null) return;
        configuration = Config.decode(encodedConfig);
        show(activity);
    }

    private static synchronized void show(Activity activity) {
        if (activity.isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
        if (CONTROLLERS.containsKey(activity)) return;
        try {
            Controller controller = new Controller(activity, configuration);
            CONTROLLERS.put(activity, controller);
            controller.attach();
        } catch (RuntimeException ignored) {
            // TODO(runtime-overlay): replace this broad runtime guard with narrow diagnostics after
            // the first APK compatibility matrix is available. Never let overlay failure crash host apps.
            CONTROLLERS.remove(activity);
        }
    }

    private static synchronized void remove(Activity activity) {
        Controller controller = CONTROLLERS.remove(activity);
        if (controller != null) controller.detach();
    }

    private static final class LifecycleCallbacks implements Application.ActivityLifecycleCallbacks {
        @Override public void onActivityCreated(Activity a, Bundle state) { }
        @Override public void onActivityStarted(Activity a) { }
        @Override public void onActivityResumed(Activity a) { show(a); }
        @Override public void onActivityPaused(Activity a) { }
        @Override public void onActivityStopped(Activity a) { }
        @Override public void onActivitySaveInstanceState(Activity a, Bundle state) { }
        @Override public void onActivityDestroyed(Activity a) { remove(a); }
    }

    /** Owns all views and state for exactly one Activity. */
    private static final class Controller {
        private final Activity activity;
        private final Config config;
        private final FrameLayout root;
        private final TextView floatingButton;
        private final FrameLayout menuLayer;
        private final LinearLayout panel;
        private final int originalWindowFlags;
        private final int originalSystemUi;
        private boolean menuVisible;
        private boolean fullyClosed;
        private float downX;
        private float downY;
        private float startX;
        private float startY;
        private boolean dragged;

        Controller(Activity activity, Config config) {
            this.activity = activity;
            this.config = config;
            Window window = activity.getWindow();
            originalWindowFlags = window.getAttributes().flags;
            originalSystemUi = window.getDecorView().getSystemUiVisibility();
            root = new FrameLayout(activity);
            root.setClipChildren(false);
            floatingButton = createFloatingButton();
            menuLayer = new FrameLayout(activity);
            panel = createMenuPanel();
        }

        void attach() {
            // TODO(runtime-overlay): verify this attachment strategy against SurfaceView and
            // immersive-mode hosts before adding more rendering-specific behavior.
            FrameLayout.LayoutParams rootParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            activity.addContentView(root, rootParams);
            root.addView(menuLayer, new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            menuLayer.addView(panel, panel.getLayoutParams());
            root.addView(floatingButton, buttonParams());
            menuLayer.setVisibility(View.GONE);
        }

        void detach() {
            if (root.getParent() instanceof ViewGroup) ((ViewGroup) root.getParent()).removeView(root);
        }

        private FrameLayout.LayoutParams buttonParams() {
            int size = dp(config.buttonSize);
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(size, size);
            params.gravity = config.gravity;
            int margin = dp(16);
            params.setMargins(margin, margin, margin, margin);
            return params;
        }

        private TextView createFloatingButton() {
            TextView button = new TextView(activity);
            button.setText(config.buttonText);
            button.setTextColor(config.buttonTextColor);
            button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            button.setGravity(Gravity.CENTER);
            button.setAlpha(config.opacity);
            button.setContentDescription(config.buttonText);
            button.setBackground(background(config.buttonBackground, config.outline, config.shape == 1));
            button.setOnClickListener(v -> toggleMenu());
            button.setOnTouchListener(this::onButtonTouch);
            return button;
        }

        private FrameLayout createMenuLayer() {
            FrameLayout layer = new FrameLayout(activity);
            layer.setBackgroundColor(0x66000000);
            layer.setOnClickListener(v -> closeMenu());
            return layer;
        }

        private LinearLayout createMenuPanel() {
            LinearLayout menu = new LinearLayout(activity);
            menu.setOrientation(LinearLayout.VERTICAL);
            menu.setPadding(dp(20), dp(18), dp(20), dp(12));
            menu.setBackground(background(config.background, config.outline, false));
            FrameLayout.LayoutParams panelParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            panelParams.setMargins(dp(20), dp(20), dp(20), dp(20));
            menu.setLayoutParams(panelParams);

            TextView title = text(config.title, 20, config.outline);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            menu.addView(title, new LinearLayout.LayoutParams(-1, -2));

            TextView description = text(config.description, 14, config.outline);
            LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(-1, -2);
            descriptionParams.topMargin = dp(8);
            menu.addView(description, descriptionParams);

            ScrollView scroll = new ScrollView(activity);
            scroll.setFillViewport(true);
            LinearLayout controls = new LinearLayout(activity);
            controls.setOrientation(LinearLayout.VERTICAL);
            addControls(controls);
            scroll.addView(controls, new ScrollView.LayoutParams(-1, -2));
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1f);
            scrollParams.topMargin = dp(12);
            menu.addView(scroll, scrollParams);

            LinearLayout actions = new LinearLayout(activity);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER);
            menu.addView(actions, new LinearLayout.LayoutParams(-1, -2));
            addAction(actions, config.repositoryText, v -> openRepository());
            addAction(actions, "Close menu", v -> closeMenu());
            addAction(actions, "Fully close", v -> fullyClose());
            return menu;
        }

        private void addControls(LinearLayout controls) {
            // TODO(runtime-overlay): add future controls as independent feature classes. The
            // current three controls remain the only optional rows in the scrollable container.
            if (config.keepAwake) addSwitch(controls, "Keep screen awake", (originalWindowFlags & 0x80) != 0,
                    checked -> { if (checked) activity.getWindow().addFlags(0x80); else restoreWindowFlag(0x80); });
            if (config.fullscreen) addSwitch(controls, "Fullscreen", (originalSystemUi & 0x4) != 0,
                    checked -> activity.getWindow().getDecorView().setSystemUiVisibility(checked ? 0x1706 : originalSystemUi));
            if (config.screenshots) addSwitch(controls, "Allow screenshots", (originalWindowFlags & 0x2000) == 0,
                    checked -> { if (checked) activity.getWindow().clearFlags(0x2000); else activity.getWindow().addFlags(0x2000); });
        }

        private void addSwitch(LinearLayout parent, String label, boolean initial, final Toggle toggle) {
            Switch control = new Switch(activity);
            control.setText(label);
            control.setTextSize(16);
            control.setTextColor(config.outline);
            control.setChecked(initial);
            control.setPadding(0, dp(6), 0, dp(6));
            control.setOnCheckedChangeListener((button, checked) -> toggle.changed(checked));
            parent.addView(control, new LinearLayout.LayoutParams(-1, -2));
        }

        private TextView text(String value, float size, int color) {
            TextView view = new TextView(activity);
            view.setText(value);
            view.setTextSize(size);
            view.setTextColor(color);
            return view;
        }

        private void addAction(LinearLayout row, String label, View.OnClickListener listener) {
            TextView action = text(label, 14, config.outline);
            action.setGravity(Gravity.CENTER);
            action.setContentDescription(label);
            action.setOnClickListener(listener);
            action.setBackground(selectableBackground(activity));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
            row.addView(action, params);
        }

        private void toggleMenu() {
            if (fullyClosed) return;
            menuVisible = !menuVisible;
            menuLayer.setVisibility(menuVisible ? View.VISIBLE : View.GONE);
            floatingButton.animate().alpha(menuVisible ? 0f : config.opacity).setDuration(180).start();
        }

        private void closeMenu() {
            menuVisible = false;
            menuLayer.setVisibility(View.GONE);
            floatingButton.animate().alpha(config.opacity).setDuration(180).start();
        }

        private void fullyClose() {
            fullyClosed = true;
            detach();
        }

        private void openRepository() {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(config.repositoryUrl));
                activity.startActivity(intent);
            } catch (RuntimeException ignored) {
                Toast.makeText(activity, "No app is available to open the repository link.", Toast.LENGTH_SHORT).show();
            }
        }

        private boolean onButtonTouch(View view, MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    downX = event.getRawX(); downY = event.getRawY();
                    startX = view.getX(); startY = view.getY(); dragged = false;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;
                    if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) dragged = true;
                    if (dragged) {
                        view.setX(clamp(startX + dx, 0, root.getWidth() - view.getWidth()));
                        view.setY(clamp(startY + dy, 0, root.getHeight() - view.getHeight()));
                        view.setAlpha(0f);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!dragged) view.performClick();
                    else view.animate().alpha(config.opacity).setDuration(180).start();
                    return true;
                default: return true;
            }
        }

        private void restoreWindowFlag(int flag) {
            if ((originalWindowFlags & flag) != 0) activity.getWindow().addFlags(flag);
            else activity.getWindow().clearFlags(flag);
        }

        private int dp(int value) { return (int) (value * activity.getResources().getDisplayMetrics().density + .5f); }
    }

    private interface Toggle { void changed(boolean checked); }

    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(value, max)); }

    private static GradientDrawable background(int color, int stroke, boolean circle) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(circle ? 1000f : 24f);
        drawable.setStroke(1, stroke);
        return drawable;
    }

    private static android.graphics.drawable.Drawable selectableBackground(Context context) {
        android.content.res.TypedArray attributes = context.obtainStyledAttributes(
                new int[] { android.R.attr.selectableItemBackgroundBorderless });
        android.graphics.drawable.Drawable drawable = attributes.getDrawable(0);
        attributes.recycle();
        return drawable;
    }

    private static final class Config {
        String title, description, repositoryText, repositoryUrl, buttonText;
        int background, outline, buttonTextColor, buttonBackground, buttonSize, gravity;
        float opacity;
        int shape;
        boolean keepAwake, fullscreen, screenshots;

        static Config decode(String encoded) {
            Config c = new Config();
            String[] values = encoded == null ? new String[0] : encoded.split("\\|", -1);
            String[] v = new String[14];
            for (int i = 0; i < v.length; i++) v[i] = i < values.length ? decodePart(values[i]) : "";
            c.title = limit(v[0], 80, "Nai64Patches Runtime Controls Overlay");
            c.description = limit(v[1], 500, "Nai64Patches Runtime Controls Overlay");
            c.repositoryText = empty(v[2], "Nai64 repository");
            c.repositoryUrl = empty(v[3], "https://github.com/Nai64/Nai64Patches");
            c.background = color(v[4], 0xCC101820);
            c.outline = color(v[5], 0xFF55D6BE);
            c.buttonText = limit(empty(v[6], "N"), 3, "N");
            c.buttonTextColor = color(v[7], Color.BLACK);
            c.buttonBackground = color(v[8], Color.WHITE);
            c.shape = "square".equals(v[9]) ? 0 : ("squircle".equals(v[9]) ? 2 : 1);
            c.buttonSize = integer(v[10], 56, 32, 128);
            c.opacity = integer(v[11], 35, 10, 100) / 100f;
            c.gravity = gravity(v[12]);
            String controls = v[13];
            c.keepAwake = controls.contains("keep");
            c.fullscreen = controls.contains("fullscreen");
            c.screenshots = controls.contains("screenshots");
            return c;
        }

        private static String decodePart(String value) {
            try { return new String(android.util.Base64.decode(value, android.util.Base64.DEFAULT), java.nio.charset.Charset.forName("UTF-8")); }
            catch (RuntimeException ignored) { return ""; }
        }
        private static String empty(String value, String fallback) { return value == null || value.isEmpty() ? fallback : value; }
        private static String limit(String value, int max, String fallback) { String result = empty(value, fallback); return result.substring(0, Math.min(max, result.length())); }
        private static int integer(String value, int fallback, int min, int max) { try { return Math.max(min, Math.min(max, Integer.parseInt(value))); } catch (RuntimeException ignored) { return fallback; } }
        private static int color(String value, int fallback) { try { String v = value.replace("#", ""); if (v.length() == 6) v = "FF" + v; return (int) Long.parseLong(v, 16); } catch (RuntimeException ignored) { return fallback; } }
        private static int gravity(String value) {
            if ("topLeft".equals(value)) return Gravity.TOP | Gravity.LEFT;
            if ("topMiddle".equals(value)) return Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            if ("centerLeft".equals(value)) return Gravity.CENTER_VERTICAL | Gravity.LEFT;
            if ("centerRight".equals(value)) return Gravity.CENTER_VERTICAL | Gravity.RIGHT;
            if ("bottomLeft".equals(value)) return Gravity.BOTTOM | Gravity.LEFT;
            if ("bottomMiddle".equals(value)) return Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            if ("bottomRight".equals(value)) return Gravity.BOTTOM | Gravity.RIGHT;
            return Gravity.TOP | Gravity.RIGHT;
        }
    }
}
