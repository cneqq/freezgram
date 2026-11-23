package app.freezgram;

import android.graphics.Typeface;
import android.os.Build;

import org.telegram.messenger.ApplicationLoader;

/** System font weights for the compact labels in iOS mode. */
public final class IosTypography {
    public static final Typeface REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
    public static final Typeface MEDIUM = weight(500);
    public static final Typeface SEMIBOLD = weight(600);

    private IosTypography() {}

    /** Inter for the profile only, including Cyrillic text and its native weight axis. */
    public static final class Profile {
        public static final Typeface REGULAR = make(400, IosTypography.REGULAR);
        public static final Typeface MEDIUM = make(500, IosTypography.MEDIUM);
        public static final Typeface SEMIBOLD = make(600, IosTypography.SEMIBOLD);

        private Profile() {}

        private static Typeface make(int weight, Typeface fallback) {
            if (Build.VERSION.SDK_INT < 26 || ApplicationLoader.applicationContext == null) {
                return fallback;
            }
            final Typeface font = new Typeface.Builder(ApplicationLoader.applicationContext.getAssets(), "fonts/inter-variable.ttf")
                    .setFontVariationSettings("'opsz' 14, 'wght' " + weight)
                    .setFallback("sans-serif")
                    .build();
            return font == null ? fallback : font;
        }
    }

    private static Typeface weight(int weight) {
        return Build.VERSION.SDK_INT >= 28 ? Typeface.create(REGULAR, weight, false)
                : Typeface.create("sans-serif-medium", Typeface.NORMAL);
    }
}
