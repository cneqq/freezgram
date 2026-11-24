package app.freezgram;

import java.util.function.BooleanSupplier;
import android.os.Build;
import android.view.View;
import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.utils.RenderNodeEffects;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProvider;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProviderBuilder;
import org.telegram.utils.glass.source.GlassSourceRenderNode;

/** Shared optical parameters, retaining the existing preference keys. */
public final class LiquidGlassStyle {
    private LiquidGlassStyle() {}
    public static float overlayAlpha(boolean dark) {
        return overlayAlpha(dark, AppearanceConfig.iosMode());
    }

    public static float overlayAlpha(boolean dark, boolean ios) {
        int transparency = ios ? AppearanceConfig.iosGlassTransparency() : AppearanceConfig.navigationGlassTransparency();
        if (ios && transparency < 0) return dark ? .17f : .24f;
        return transparency < 0 ? (dark ? .10f : .07f) : (100 - transparency) / 100f;
    }

    public static int blurRadiusDp() {
        return AppearanceConfig.iosMode() ? AppearanceConfig.iosGlassBlur() : AppearanceConfig.navigationGlassBlur();
    }
    public static int refraction() {
        return AppearanceConfig.iosMode() ? AppearanceConfig.iosGlassIntensity() : AppearanceConfig.navigationGlassIntensity();
    }
    public static boolean enhanceColors() {
        return AppearanceConfig.iosMode() ? AppearanceConfig.iosGlassEnhanceColors() : AppearanceConfig.glassEnhanceColors();
    }
    public static boolean wallpaperDim() {
        return AppearanceConfig.iosMode() ? AppearanceConfig.iosGlassWallpaperDim() : AppearanceConfig.glassWallpaperDim();
    }

    @androidx.annotation.RequiresApi(31)
    public static void applyBackdropBlur(GlassSourceRenderNode source) {
        source.setBlur(AndroidUtilities.dpf2(blurRadiusDp()),
                enhanceColors() ? RenderNodeEffects.getSaturationX3RenderEffect() : null);
    }
    public static int revision() {
        boolean ios = AppearanceConfig.iosMode();
        int transparency = ios ? AppearanceConfig.iosGlassTransparency() : AppearanceConfig.navigationGlassTransparency();
        int thickness = ios ? AppearanceConfig.iosGlassEdge() : 12;
        int optics = ((blurRadiusDp() * 101 + refraction()) * 102 + transparency + 1) * 21 + thickness;
        int flags = (ios ? 1 : 0) | (enhanceColors() ? 2 : 0) | (wallpaperDim() ? 4 : 0)
                | (!ios && AppearanceConfig.chatHeaderGlassDim() ? 8 : 0);
        return optics * 16 + flags;
    }
    public static void configure(BlurredBackgroundDrawable drawable) {
        drawable.setFreezgramGlass(!AppearanceConfig.iosMode());
        drawable.setThickness(AndroidUtilities.dp(AppearanceConfig.iosMode() ? AppearanceConfig.iosGlassEdge() : 12));
        drawable.setIntensity(refraction() / 100f);
    }

    public static boolean canDrawGlass() {
        return Build.VERSION.SDK_INT >= 33 && SharedConfig.chatBlurEnabled() && !LiteMode.isGlassPowerSaverApplied();
    }

    public static BlurredBackgroundDrawable createSurface(BlurredBackgroundDrawableViewFactory factory,
                                                           View owner, Theme.ResourcesProvider provider) {
        BlurredBackgroundDrawable drawable = factory.create(owner,
                surfaceColors(provider, LiquidGlassStyle::canDrawGlass, false));
        configure(drawable);
        if (Build.VERSION.SDK_INT >= 33) {
            drawable.setLiquidGlassEffectAllowed(true);
        }
        return drawable;
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
        if (AppearanceConfig.iosMode()) {
            return new BlurredBackgroundProviderBuilder(resourcesProvider)
                    .setBackgroundColor((r, dark) -> Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhite, r),
                            enabled.getAsBoolean() ? Math.max(surface ? .12f : 0f, overlayAlpha(dark, true)) : 1f))
                    .setStrokeColorTop(drawStroke ? 0x33ffffff : 0, drawStroke ? 0x66ffffff : 0)
                    .setStrokeColorBottom(drawStroke ? 0x33000000 : 0, drawStroke ? 0x22000000 : 0)
                    .setStrokeWidth(AndroidUtilities.dpf2(.8f), AndroidUtilities.dpf2(.6f))
                    .setShadowColor(drawStroke ? 0x17000000 : 0, drawStroke ? 0x22000000 : 0)
                    .setShadowLayer(AndroidUtilities.dpf2(5), 0, AndroidUtilities.dpf2(2)).build();
        }
        return new BlurredBackgroundProviderBuilder(resourcesProvider)
                .setBackgroundColor((r, dark) -> header && AppearanceConfig.chatHeaderGlassDim() && enabled.getAsBoolean()
                        ? Theme.multAlpha(0xff000000, Math.max(.40f, overlayAlpha(dark, false))) : Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhite, r),
                        enabled.getAsBoolean() ? (surface ? Math.max(overlayAlpha(dark, false), dark ? .24f : .18f) : overlayAlpha(dark, false))
                                : org.telegram.messenger.LiteMode.isGlassPowerSaverApplied() ? 1f : .82f))
                .setStrokeColorTop(!drawStroke ? 0 : surface ? 0x99ffffff : 0x66ffffff, !drawStroke ? 0 : surface ? 0x99ffffff : 0x4dffffff)
                .setStrokeColorBottom(!drawStroke ? 0 : surface ? 0x40000000 : 0x24000000, !drawStroke ? 0 : surface ? 0x55ffffff : 0x26ffffff)
                .setStrokeWidth(AndroidUtilities.dpf2(surface ? 1.2f : .9f), AndroidUtilities.dpf2(surface ? .8f : .5f))
                .setShadowColor(drawStroke ? 0x24000000 : 0, drawStroke ? 0x33000000 : 0)
                .setShadowLayer(AndroidUtilities.dpf2(6), 0, AndroidUtilities.dpf2(2)).build();
    }
}
