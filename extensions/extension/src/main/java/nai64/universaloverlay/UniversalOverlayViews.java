package nai64.universaloverlay;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.RectF;

/** Shared view construction and styling primitives for the overlay controller. */
final class UniversalOverlayViews {
    private UniversalOverlayViews() { }
    static GradientDrawable background(int color, int stroke, boolean circle) {
        return background(color, stroke, circle, 1);
    }
    static GradientDrawable background(int color, int stroke, boolean circle, int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(circle ? 1000f : 24f);
        drawable.setStroke(Math.max(1, strokeWidth), stroke);
        return drawable;
    }

    /** Builds the legacy icon background while keeping its gradient and outline independent. */
    static Drawable gradientBackground(int first, int second, float angle, int stroke, int strokeWidth, boolean circle) {
        return new GradientBackground(first, second, angle, stroke, strokeWidth, circle);
    }

    /** Uses the overlay theme context so host-app selectable colors do not leak into our controls. */
    static Drawable selectableBackground(Context context) {
        android.content.res.TypedArray attributes = context.obtainStyledAttributes(
                new int[] { android.R.attr.selectableItemBackgroundBorderless });
        Drawable drawable = attributes.getDrawable(0);
        attributes.recycle();
        return drawable;
    }

    private static final class GradientBackground extends Drawable {
        private final int first;
        private final int second;
        private final float angle;
        private final int stroke;
        private final int strokeWidth;
        private final boolean circle;
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);

        GradientBackground(int first, int second, float angle, int stroke, int strokeWidth, boolean circle) {
            this.first = first;
            this.second = second;
            this.angle = angle;
            this.stroke = stroke;
            this.strokeWidth = Math.max(1, strokeWidth);
            this.circle = circle;
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(this.strokeWidth);
            border.setColor(stroke);
        }

        @Override public void draw(Canvas canvas) {
            RectF bounds = new RectF(getBounds());
            float radians = (float) Math.toRadians(angle);
            float dx = (float) Math.sin(radians);
            float dy = (float) Math.cos(radians);
            float length = (float) Math.hypot(bounds.width(), bounds.height());
            float cx = bounds.centerX();
            float cy = bounds.centerY();
            fill.setShader(new LinearGradient(
                    cx - dx * length / 2f, cy - dy * length / 2f,
                    cx + dx * length / 2f, cy + dy * length / 2f,
                    first, second, Shader.TileMode.CLAMP));
            float radius = circle ? Math.min(bounds.width(), bounds.height()) / 2f : 24f;
            canvas.drawRoundRect(bounds, radius, radius, fill);
            canvas.drawRoundRect(bounds, radius, radius, border);
        }

        @Override public void setAlpha(int alpha) {
            fill.setAlpha(alpha);
            border.setAlpha(alpha);
            invalidateSelf();
        }
        @Override public void setColorFilter(android.graphics.ColorFilter filter) {
            fill.setColorFilter(filter);
            border.setColorFilter(filter);
            invalidateSelf();
        }
        @Override public int getOpacity() { return android.graphics.PixelFormat.TRANSLUCENT; }
    }
}
