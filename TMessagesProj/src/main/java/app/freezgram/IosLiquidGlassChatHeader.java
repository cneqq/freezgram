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
