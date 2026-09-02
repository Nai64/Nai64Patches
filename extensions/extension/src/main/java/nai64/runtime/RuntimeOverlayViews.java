package nai64.runtime;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;

/** Shared view construction and styling primitives for the overlay controller. */
final class RuntimeOverlayViews {
    private RuntimeOverlayViews() { }
    static GradientDrawable background(int color, int stroke, boolean circle) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(circle ? 1000f : 24f);
        drawable.setStroke(1, stroke);
        return drawable;
    }
    static Drawable selectableBackground(Context context) {
        android.content.res.TypedArray attributes = context.obtainStyledAttributes(
                new int[] { android.R.attr.selectableItemBackgroundBorderless });
        Drawable drawable = attributes.getDrawable(0);
        attributes.recycle();
        return drawable;
    }
}
