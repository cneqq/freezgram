package app.freezgram;

import java.util.function.BooleanSupplier;
import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProvider;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProviderBuilder;

/** Shared optical parameters, retaining the existing preference keys. */
public final class LiquidGlassStyle {
    private LiquidGlassStyle() {}
    public static float overlayAlpha(boolean dark) {
        int transparency = AppearanceConfig.navigationGlassTransparency();
        return transparency < 0 ? (dark ? .10f : .07f) : (100 - transparency) / 100f;
    }
    public static int blurRadiusDp() { return AppearanceConfig.navigationGlassBlur(); }
    public static boolean enhanceColors() { return AppearanceConfig.glassEnhanceColors(); }
    public static int revision() {
        return ((((blurRadiusDp() * 101 + AppearanceConfig.navigationGlassIntensity()) * 102
                + AppearanceConfig.navigationGlassTransparency() + 1) * 2 + (enhanceColors() ? 1 : 0)) * 2 + (AppearanceConfig.glassWallpaperDim() ? 1 : 0)) * 2 + (AppearanceConfig.chatHeaderGlassDim() ? 1 : 0);
    }
    public static void configure(BlurredBackgroundDrawable drawable) {
        drawable.setThickness(AndroidUtilities.dp(12));
        drawable.setIntensity(AppearanceConfig.navigationGlassIntensity() / 100f);
    }
    public static BlurredBackgroundProvider colors(Theme.ResourcesProvider resourcesProvider, BooleanSupplier enabled) {
        return colors(resourcesProvider, enabled, false);
    }

    /** Popups need a visible surface behind labels even at 100% panel transparency. */
    public static BlurredBackgroundProvider surfaceColors(Theme.ResourcesProvider resourcesProvider, BooleanSupplier enabled) {
        return colors(resourcesProvider, enabled, true);
    }

    public static BlurredBackgroundProvider surfaceColors(Theme.ResourcesProvider resourcesProvider, BooleanSupplier enabled, boolean drawStroke) {
        return colors(resourcesProvider, enabled, true, false, drawStroke);
    }

    public static BlurredBackgroundProvider headerColors(Theme.ResourcesProvider resourcesProvider, BooleanSupplier enabled) {
        return colors(resourcesProvider, enabled, false, true);
    }

    private static BlurredBackgroundProvider colors(Theme.ResourcesProvider resourcesProvider, BooleanSupplier enabled, boolean surface) {
        return colors(resourcesProvider, enabled, surface, false);
    }

    private static BlurredBackgroundProvider colors(Theme.ResourcesProvider resourcesProvider, BooleanSupplier enabled, boolean surface, boolean header) {
        return colors(resourcesProvider, enabled, surface, header, true);
    }

    private static BlurredBackgroundProvider colors(Theme.ResourcesProvider resourcesProvider, BooleanSupplier enabled, boolean surface, boolean header, boolean drawStroke) {
        return new BlurredBackgroundProviderBuilder(resourcesProvider)
                .setBackgroundColor((r, dark) -> header && AppearanceConfig.chatHeaderGlassDim() && enabled.getAsBoolean()
                        ? Theme.multAlpha(0xff000000, Math.max(.40f, overlayAlpha(dark))) : Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhite, r),
                        enabled.getAsBoolean() ? (surface ? Math.max(overlayAlpha(dark), dark ? .24f : .18f) : overlayAlpha(dark))
                                : org.telegram.messenger.LiteMode.isGlassPowerSaverApplied() ? 1f : .82f))
                .setStrokeColorTop(!drawStroke ? 0 : surface ? 0x99ffffff : 0x66ffffff, !drawStroke ? 0 : surface ? 0x99ffffff : 0x4dffffff)
                .setStrokeColorBottom(!drawStroke ? 0 : surface ? 0x40000000 : 0x24000000, !drawStroke ? 0 : surface ? 0x55ffffff : 0x26ffffff)
                .setStrokeWidth(AndroidUtilities.dpf2(surface ? 1.2f : .9f), AndroidUtilities.dpf2(surface ? .8f : .5f))
                .setShadowColor(drawStroke ? 0x24000000 : 0, drawStroke ? 0x33000000 : 0)
                .setShadowLayer(AndroidUtilities.dpf2(6), 0, AndroidUtilities.dpf2(2)).build();
    }
}
