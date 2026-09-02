package nai64.runtime;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
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
    private static RuntimeOverlayConfig configuration;

    private RuntimeOverlayRuntime() { }

    /** Primary entry point, called once from Application.onCreate(). */
    public static synchronized void install(Application application, String encodedConfig) {
        if (application == null) return;
        configuration = RuntimeOverlayConfig.decode(encodedConfig);
        if (!callbacksRegistered) {
            application.registerActivityLifecycleCallbacks(new RuntimeOverlayLifecycle());
            callbacksRegistered = true;
        }
    }

    /** Compatibility fallback for APKs where Application.onCreate cannot be resolved. */
    public static synchronized void installActivity(Activity activity, String encodedConfig) {
        if (activity == null) return;
        try {
            Application application = activity.getApplication();
            if (application != null) {
                install(application, encodedConfig);
            } else {
                configuration = RuntimeOverlayConfig.decode(encodedConfig);
            }
        } catch (RuntimeException ignored) {
            configuration = RuntimeOverlayConfig.decode(encodedConfig);
        }
        showActivity(activity);
        try {
            // The current Activity may already be inside onCreate when callbacks are registered,
            // so it will not receive the first onActivityResumed event. Retry after the framework
            // has attached the Activity window and issued its first layout pass.
            activity.getWindow().getDecorView().post(() -> promoteActivity(activity));
        } catch (RuntimeException ignored) {
            // The normal lifecycle callback remains the fallback promotion path.
        }
    }

    static synchronized void showActivity(Activity activity) {
        if (configuration == null) return;
        if (activity.isFinishing() || (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) return;
        Controller existing = CONTROLLERS.get(activity);
        if (existing != null) {
            existing.promoteToWindowLayer();
            return;
        }
        Controller controller = null;
        try {
            controller = new Controller(activity, configuration);
            CONTROLLERS.put(activity, controller);
            controller.attach();
        } catch (RuntimeException ignored) {
            if (controller != null) controller.detach();
            // Never let overlay setup failure crash the host application.
            CONTROLLERS.remove(activity);
        }
    }

    private static synchronized void promoteActivity(Activity activity) {
        Controller controller = CONTROLLERS.get(activity);
        if (controller != null) controller.promoteToWindowLayer();
    }

    static synchronized void removeActivity(Activity activity) {
        Controller controller = CONTROLLERS.remove(activity);
        if (controller != null) controller.detach();
    }

    /** Owns all views and state for exactly one Activity. */
    private static final class Controller {
        private final Activity activity;
        private final RuntimeOverlayConfig config;
        private final FrameLayout root;
        private final TextView floatingButton;
        private final FrameLayout menuLayer;
        private final View menuScrim;
        private final LinearLayout panel;
        private final FrameLayout confirmationLayer;
        private final List<RuntimeOverlayFeature> features = new ArrayList<>();
        private final int originalWindowFlags;
        private final int originalSystemUi;
        private boolean menuVisible;
        private boolean fullyClosed;
        private boolean attached;
        private boolean detached;
        private boolean windowAttached;
        private WindowManager windowManager;
        private int windowX;
        private int windowY;
        private boolean windowPositionInitialized;
        private float downX;
        private float downY;
        private float startX;
        private float startY;
        private boolean dragged;

        Controller(Activity activity, RuntimeOverlayConfig config) {
            this.activity = activity;
            this.config = config;
            Window window = activity.getWindow();
            originalWindowFlags = window.getAttributes().flags;
            originalSystemUi = window.getDecorView().getSystemUiVisibility();
            root = new FrameLayout(activity);
            root.setClipChildren(false);
            floatingButton = createFloatingButton();
            menuLayer = new FrameLayout(activity);
            menuScrim = createMenuScrim();
            panel = createMenuPanel();
            confirmationLayer = createConfirmationLayer();
        }

        void attach() {
            if (attached || detached) return;
            try {
                root.addView(menuLayer, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                menuLayer.addView(menuScrim, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                menuLayer.addView(panel, panel.getLayoutParams());
                root.addView(floatingButton, buttonParams());
                root.addView(confirmationLayer, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                menuLayer.setVisibility(View.GONE);
                confirmationLayer.setVisibility(View.GONE);
                if (!tryAttachWindowLayer()) {
                    activity.addContentView(root, contentLayoutParams());
                }
                attached = true;
            } catch (RuntimeException failure) {
                removeRoot();
                throw failure;
            }
        }

        void detach() {
            if (detached) return;
            detached = true;
            restoreFeatures();
            removeRoot();
        }

        private void removeRoot() {
            try {
                if (windowAttached && windowManager != null) {
                    windowManager.removeViewImmediate(root);
                } else if (root.getParent() instanceof ViewGroup) {
                    ((ViewGroup) root.getParent()).removeView(root);
                }
            } catch (RuntimeException ignored) {
                // Cleanup must not propagate a host-specific view hierarchy failure.
            }
            windowAttached = false;
            windowManager = null;
            attached = false;
        }

        private FrameLayout.LayoutParams contentLayoutParams() {
            return new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        }

        private boolean tryAttachWindowLayer() {
            try {
                Window window = activity.getWindow();
                IBinder token = window.getAttributes().token;
                if (token == null) token = window.getDecorView().getWindowToken();
                WindowManager manager = (WindowManager) activity.getSystemService(Activity.WINDOW_SERVICE);
                if (token == null || manager == null) return false;
                initializeWindowPosition();
                floatingButton.setLayoutParams(windowButtonChildParams());
                WindowManager.LayoutParams params = windowLayoutParams(
                        menuVisible ? ViewGroup.LayoutParams.MATCH_PARENT : dp(config.buttonSize),
                        menuVisible ? ViewGroup.LayoutParams.MATCH_PARENT : dp(config.buttonSize));
                params.token = token;
                manager.addView(root, params);
                windowManager = manager;
                windowAttached = true;
                return true;
            } catch (RuntimeException ignored) {
                floatingButton.setLayoutParams(buttonParams());
                return false;
            }
        }

        private WindowManager.LayoutParams windowLayoutParams(int width, int height) {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                        width,
                        height,
                        WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT);
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.x = width == ViewGroup.LayoutParams.MATCH_PARENT ? 0 : windowX;
            params.y = height == ViewGroup.LayoutParams.MATCH_PARENT ? 0 : windowY;
            params.setTitle("Nai64 Runtime Overlay");
            return params;
        }

        private void initializeWindowPosition() {
            if (windowPositionInitialized) return;
            int width = activity.getResources().getDisplayMetrics().widthPixels;
            int height = activity.getResources().getDisplayMetrics().heightPixels;
            int size = dp(config.buttonSize);
            int margin = dp(16);
            int horizontal = config.gravity & Gravity.HORIZONTAL_GRAVITY_MASK;
            int vertical = config.gravity & Gravity.VERTICAL_GRAVITY_MASK;
            windowX = horizontal == Gravity.RIGHT ? Math.max(0, width - size - margin)
                    : horizontal == Gravity.CENTER_HORIZONTAL ? Math.max(0, (width - size) / 2) : margin;
            windowY = vertical == Gravity.BOTTOM ? Math.max(0, height - size - margin)
                    : vertical == Gravity.CENTER_VERTICAL ? Math.max(0, (height - size) / 2) : margin;
            windowPositionInitialized = true;
        }

        private FrameLayout.LayoutParams windowButtonChildParams() {
            return new FrameLayout.LayoutParams(dp(config.buttonSize), dp(config.buttonSize));
        }

        private void updateWindowLayer(boolean menuOpen) {
            if (!windowAttached || windowManager == null) return;
            try {
                if (menuOpen) {
                    floatingButton.setLayoutParams(buttonParams());
                    windowManager.updateViewLayout(root, windowLayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                } else {
                    int[] location = new int[2];
                    floatingButton.getLocationOnScreen(location);
                    windowX = Math.max(0, location[0]);
                    windowY = Math.max(0, location[1]);
                    floatingButton.setLayoutParams(windowButtonChildParams());
                    windowManager.updateViewLayout(root, windowLayoutParams(dp(config.buttonSize), dp(config.buttonSize)));
                }
            } catch (RuntimeException ignored) {
                moveToContentFallback();
            }
        }

        private void moveToContentFallback() {
            if (!windowAttached || windowManager == null || detached) return;
            try {
                windowManager.removeViewImmediate(root);
            } catch (RuntimeException ignored) {
                // Continue with the content fallback attempt.
            }
            windowAttached = false;
            windowManager = null;
            try {
                floatingButton.setLayoutParams(buttonParams());
                activity.addContentView(root, contentLayoutParams());
            } catch (RuntimeException ignored) {
                attached = false;
            }
        }

        void promoteToWindowLayer() {
            if (!attached || detached || windowAttached) return;
            ViewParent parent = root.getParent();
            if (!(parent instanceof ViewGroup)) return;
            ViewGroup contentParent = (ViewGroup) parent;
            int[] location = new int[2];
            root.getLocationOnScreen(location);
            windowX = Math.max(0, location[0] + (int) floatingButton.getX());
            windowY = Math.max(0, location[1] + (int) floatingButton.getY());
            windowPositionInitialized = true;
            contentParent.removeView(root);
            floatingButton.setLayoutParams(windowButtonChildParams());
            if (!tryAttachWindowLayer()) {
                try {
                    floatingButton.setLayoutParams(buttonParams());
                    contentParent.addView(root, contentLayoutParams());
                } catch (RuntimeException ignored) {
                    // The content fallback was already removed; lifecycle cleanup will finish.
                    attached = false;
                }
            }
        }

        private void restoreFeatures() {
            for (RuntimeOverlayFeature feature : features) {
                try {
                    feature.restore(activity, originalWindowFlags, originalSystemUi);
                } catch (RuntimeException ignored) {
                    // A single incompatible window must not prevent other features or host cleanup.
                }
            }
            features.clear();
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
            button.setBackground(RuntimeOverlayViews.background(config.buttonBackground, config.outline, config.shape == 1));
            button.setOnClickListener(v -> toggleMenu());
            button.setOnTouchListener(this::onButtonTouch);
            return button;
        }

        private FrameLayout createMenuLayer() {
            FrameLayout layer = new FrameLayout(activity);
            // This full-screen container remains touchable while the menu is open. Its children
            // consume all background touches so Unity/host content cannot receive game input.
            layer.setClickable(true);
            layer.setFocusable(true);
            return layer;
        }

        private View createMenuScrim() {
            View scrim = new View(activity);
            scrim.setBackgroundColor(0x55000000);
            scrim.setClickable(true);
            scrim.setFocusable(true);
            scrim.setOnClickListener(v -> closeMenu());
            // The clickable View consumes the gesture and still delivers its click callback;
            // returning true here would bypass View.onTouchEvent and prevent dismissal.
            scrim.setOnTouchListener((v, event) -> false);
            return scrim;
        }

        private LinearLayout createMenuPanel() {
            LinearLayout menu = new LinearLayout(activity);
            menu.setOrientation(LinearLayout.VERTICAL);
            menu.setClickable(true);
            menu.setFocusable(true);
            // Consume unused panel area without preventing its child controls from receiving taps.
            menu.setOnTouchListener((v, event) -> true);
            menu.setPadding(dp(20), dp(18), dp(20), dp(12));
            menu.setBackground(RuntimeOverlayViews.background(config.background, config.outline, false));
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

            int maxControlHeight = Math.min(dp(280), (int) (activity.getResources().getDisplayMetrics().heightPixels * .45f));
            ScrollView scroll = new BoundedScrollView(activity, maxControlHeight);
            scroll.setFillViewport(true);
            LinearLayout controls = new LinearLayout(activity);
            controls.setOrientation(LinearLayout.VERTICAL);
            addControls(controls);
            scroll.addView(controls, new ScrollView.LayoutParams(-1, -2));
            LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, -2);
            scrollParams.topMargin = dp(12);
            menu.addView(scroll, scrollParams);

            LinearLayout actions = new LinearLayout(activity);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER);
            menu.addView(actions, new LinearLayout.LayoutParams(-1, -2));
            addAction(actions, config.repositoryText, v -> openRepository());
            addAction(actions, "Close menu", v -> closeMenu());
            addAction(actions, "Fully close", v -> showCloseConfirmation());
            return menu;
        }

        private FrameLayout createConfirmationLayer() {
            FrameLayout layer = new FrameLayout(activity);
            layer.setBackgroundColor(0xB3000000);
            layer.setClickable(true);
            layer.setFocusable(true);
            layer.setOnClickListener(v -> hideCloseConfirmation());

            LinearLayout card = new LinearLayout(activity);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(20), dp(18), dp(20), dp(12));
            card.setBackground(RuntimeOverlayViews.background(config.background, config.outline, false));
            card.setClickable(true);
            card.setOnClickListener(v -> { });

            TextView title = text("Close overlay?", 20, config.outline);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(title, new LinearLayout.LayoutParams(-1, -2));

            TextView message = text("The overlay will be removed for this Activity.", 14, config.outline);
            LinearLayout.LayoutParams messageParams = new LinearLayout.LayoutParams(-1, -2);
            messageParams.topMargin = dp(8);
            card.addView(message, messageParams);

            LinearLayout actions = new LinearLayout(activity);
            actions.setOrientation(LinearLayout.HORIZONTAL);
            actions.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(-1, -2);
            actionsParams.topMargin = dp(8);
            card.addView(actions, actionsParams);
            addAction(actions, "Cancel", v -> hideCloseConfirmation());
            addAction(actions, "Fully close", v -> fullyClose());

            FrameLayout.LayoutParams cardParams = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                    Gravity.CENTER);
            cardParams.setMargins(dp(20), dp(20), dp(20), dp(20));
            layer.addView(card, cardParams);
            return layer;
        }

        private void addControls(LinearLayout controls) {
            if (config.keepAwake) addFeature(controls, new KeepAwakeFeature());
            if (config.fullscreen) addFeature(controls, new FullscreenFeature());
            if (config.screenshots) addFeature(controls, new ScreenshotsFeature());
        }

        private void addFeature(LinearLayout controls, RuntimeOverlayFeature feature) {
            final boolean initial;
            try {
                initial = feature.initiallyEnabled(activity, originalWindowFlags, originalSystemUi);
            } catch (RuntimeException ignored) {
                return;
            }
            features.add(feature);
            addControlRow(controls, feature, initial, checked -> {
                try {
                    feature.setEnabled(activity, checked, originalWindowFlags, originalSystemUi);
                } catch (RuntimeException ignored) {
                    // Feature controls are independent; a failure here must not crash the host.
                }
            });
        }

        private void addControlRow(LinearLayout parent, RuntimeOverlayFeature feature, boolean initial, final Toggle toggle) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(6), 0, dp(6));
            row.setMinimumHeight(dp(64));

            LinearLayout copy = new LinearLayout(activity);
            copy.setOrientation(LinearLayout.VERTICAL);
            TextView title = text(feature.label(), 16, config.outline);
            title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            copy.addView(title, new LinearLayout.LayoutParams(-1, -2));
            TextView description = text(feature.description(), 13, config.outline);
            description.setAlpha(.82f);
            LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(-1, -2);
            descriptionParams.topMargin = dp(2);
            copy.addView(description, descriptionParams);
            row.addView(copy, new LinearLayout.LayoutParams(0, -2, 1f));

            CheckBox control = new CheckBox(activity);
            control.setChecked(initial);
            control.setContentDescription(feature.label());
            control.setOnCheckedChangeListener((button, checked) -> {
                toggle.changed(checked);
                Toast.makeText(activity, feature.label() + " is " + (checked ? "enabled" : "disabled"), Toast.LENGTH_SHORT).show();
            });
            row.addView(control, new LinearLayout.LayoutParams(-2, -2));
            row.setClickable(true);
            row.setOnClickListener(v -> control.setChecked(!control.isChecked()));
            parent.addView(row, new LinearLayout.LayoutParams(-1, -2));
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
            action.setBackground(RuntimeOverlayViews.selectableBackground(activity));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(48), 1f);
            row.addView(action, params);
        }

        private void toggleMenu() {
            if (fullyClosed) return;
            menuVisible = !menuVisible;
            if (menuVisible) {
                updateWindowLayer(true);
                menuLayer.setVisibility(View.VISIBLE);
                menuScrim.animate().cancel();
                menuScrim.setAlpha(0f);
                menuScrim.animate().alpha(1f).setDuration(180).start();
            } else {
                hideMenuLayer();
            }
            floatingButton.animate().alpha(menuVisible ? 0f : config.opacity).setDuration(180).start();
        }

        private void closeMenu() {
            menuVisible = false;
            hideMenuLayer();
            floatingButton.animate().alpha(config.opacity).setDuration(180).start();
        }

        private void hideMenuLayer() {
            menuScrim.animate().cancel();
            menuScrim.animate().alpha(0f).setDuration(180).withEndAction(() -> {
                if (!menuVisible) {
                    menuLayer.setVisibility(View.GONE);
                    updateWindowLayer(false);
                }
            }).start();
        }

        private void showCloseConfirmation() {
            confirmationLayer.setAlpha(0f);
            confirmationLayer.setVisibility(View.VISIBLE);
            confirmationLayer.animate().alpha(1f).setDuration(180).start();
        }

        private void hideCloseConfirmation() {
            confirmationLayer.animate().alpha(0f).setDuration(160).withEndAction(() ->
                    confirmationLayer.setVisibility(View.GONE)).start();
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
                    if (windowAttached && !menuVisible) {
                        startX = windowX;
                        startY = windowY;
                    }
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float dx = event.getRawX() - downX;
                    float dy = event.getRawY() - downY;
                    if (Math.abs(dx) > dp(5) || Math.abs(dy) > dp(5)) dragged = true;
                    if (dragged) {
                        if (windowAttached && !menuVisible && windowManager != null) {
                            int maxX = Math.max(0, activity.getResources().getDisplayMetrics().widthPixels - view.getWidth());
                            int maxY = Math.max(0, activity.getResources().getDisplayMetrics().heightPixels - view.getHeight());
                            windowX = (int) clamp(startX + dx, 0, maxX);
                            windowY = (int) clamp(startY + dy, 0, maxY);
                            try {
                                windowManager.updateViewLayout(root, windowLayoutParams(view.getWidth(), view.getHeight()));
                            } catch (RuntimeException ignored) {
                                moveToContentFallback();
                            }
                        } else {
                            view.setX(clamp(startX + dx, 0, root.getWidth() - view.getWidth()));
                            view.setY(clamp(startY + dy, 0, root.getHeight() - view.getHeight()));
                        }
                        // The idle state is intentionally translucent, but dragging must make
                        // the control fully visible so its position remains easy to track.
                        view.setAlpha(1f);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if (!dragged) view.performClick();
                    else view.animate().alpha(config.opacity).setDuration(180).start();
                    return true;
                default: return true;
            }
        }

        private int dp(int value) { return (int) (value * activity.getResources().getDisplayMetrics().density + .5f); }
    }

    private interface Toggle { void changed(boolean checked); }

    private static final class BoundedScrollView extends ScrollView {
        private final int maxHeight;

        BoundedScrollView(android.content.Context context, int maxHeight) {
            super(context);
            this.maxHeight = maxHeight;
        }

        @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            setMeasuredDimension(getMeasuredWidth(), Math.min(getMeasuredHeight(), maxHeight));
        }
    }

    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(value, max)); }

}
