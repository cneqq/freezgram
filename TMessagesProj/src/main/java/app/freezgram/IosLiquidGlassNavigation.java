package app.freezgram;

import android.os.Build;
import app.exteraless.appearance.AppearanceConfig;
import app.exteraless.appearance.MainTabsUiHelper;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProvider;

/** Opt-in iOS navigation glass using the client's existing backdrop and AGSL engine. */
public final class IosLiquidGlassNavigation {
    private IosLiquidGlassNavigation() {}

    public static boolean isRequested() {
        return MainTabsUiHelper.isIosNavigationBar()
                && (AppearanceConfig.iosMode() || AppearanceConfig.iosLiquidGlassNavigation());
    }

    public static boolean isEnabled() {
        return isRequested() && Build.VERSION.SDK_INT >= 33
                && SharedConfig.chatBlurEnabled() && !LiteMode.isGlassPowerSaverApplied();
    }

    public static BlurredBackgroundProvider colors(Theme.ResourcesProvider resourcesProvider) {
        return LiquidGlassStyle.colors(resourcesProvider, () -> isEnabled());
    }

    public static float overlayAlpha(boolean dark) { return LiquidGlassStyle.overlayAlpha(dark); }
    public static int blurRadiusDp() { return LiquidGlassStyle.blurRadiusDp(); }
    public static void configure(BlurredBackgroundDrawable drawable) { LiquidGlassStyle.configure(drawable); }

    public static void refresh(BlurredBackgroundDrawable drawable) {
        if (isRequested()) configure(drawable);
        if (Build.VERSION.SDK_INT >= 33 && isRequested()
                && drawable != null) {
            ((BlurredBackgroundDrawable) drawable).setLiquidGlassEffectAllowed(isEnabled());
        }
    }
}
