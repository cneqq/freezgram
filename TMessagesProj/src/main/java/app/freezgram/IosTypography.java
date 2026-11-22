package app.freezgram;

import android.graphics.Typeface;
import android.os.Build;

/** System font weights for the compact labels in iOS mode. */
public final class IosTypography {
    public static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    public static final Typeface MEDIUM = weight(500);
    public static final Typeface SEMIBOLD = weight(600);

    private IosTypography() {}

    private static Typeface weight(int weight) {
        return Build.VERSION.SDK_INT >= 28 ? Typeface.create(REGULAR, weight, false)
                : Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }
}
