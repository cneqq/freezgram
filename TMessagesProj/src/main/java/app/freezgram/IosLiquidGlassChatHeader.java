package app.freezgram;

import android.os.Build;

import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProvider;

/** Header eligibility; optical parameters come from LiquidGlassStyle. */
public final class IosLiquidGlassChatHeader {
    public static final int PILL_HEIGHT_DP = 44;
    public static final int AVATAR_SIZE_DP = 38;
    public static final int SIDE_INSET_DP = 8;
    public static final int TITLE_PADDING_DP = 14;
    public static final int MIN_TITLE_CONTENT_WIDTH_DP = 150;

    private IosLiquidGlassChatHeader() {}

    public static boolean isRequested() {
        return AppearanceConfig.iosMode() || AppearanceConfig.iosChatHeader();
    }

    public static boolean isEnabled() {
        return isRequested() && Build.VERSION.SDK_INT >= 33
                && SharedConfig.chatBlurEnabled() && !LiteMode.isGlassPowerSaverApplied();
    }

    public static BlurredBackgroundProvider colors(Theme.ResourcesProvider resourcesProvider) {
        return LiquidGlassStyle.headerColors(resourcesProvider, () -> isEnabled());
    }

    public static void refresh(BlurredBackgroundDrawable drawable) {
        if (drawable == null) return;
        LiquidGlassStyle.configure(drawable);
        if (Build.VERSION.SDK_INT >= 33 && drawable != null) {
            ((BlurredBackgroundDrawable) drawable).setLiquidGlassEffectAllowed(isEnabled());
        }
    }
}
