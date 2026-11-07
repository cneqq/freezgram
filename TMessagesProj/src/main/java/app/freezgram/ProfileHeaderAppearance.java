package app.freezgram;

import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableRenderNode;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProviderThemed;

public final class ProfileHeaderAppearance {
    public static final int CLASSIC = 0;
    public static final int GLASS = 1;
    public static final int FROSTED = 3;

    private ProfileHeaderAppearance() {}

    public static int style() {
        int style = AppearanceConfig.profileHeaderStyle();
        return style != CLASSIC && !LiquidGlassStyle.canDrawGlass() ? CLASSIC : style;
    }

    public static BlurredBackgroundDrawableViewFactory frostedFactory(BlurredBackgroundDrawableViewFactory factory) {
        return factory.withSource(SmallElementsGlass.createFrostedSource(factory.getSource()));
    }

    public static BlurredBackgroundDrawable surface(BlurredBackgroundDrawableViewFactory factory, View owner,
                                                    Theme.ResourcesProvider provider, int style, boolean control) {
        if (style != FROSTED) {
            if (control) return LiquidGlassStyle.createSurface(factory, owner, provider);
            BlurredBackgroundDrawable drawable = factory.create(owner,
                    LiquidGlassStyle.colors(provider, LiquidGlassStyle::canDrawGlass));
            LiquidGlassStyle.configure(drawable);
            if (Build.VERSION.SDK_INT >= 33 && drawable instanceof BlurredBackgroundDrawableRenderNode renderNode) {
                renderNode.setLiquidGlassEffectAllowed(true);
            }
            return drawable;
        }
        BlurredBackgroundDrawable drawable = factory.create(owner, new BlurredBackgroundColorProviderThemed(
                provider, Theme.key_windowBackgroundWhite, control ? .34f : .18f));
        if (Build.VERSION.SDK_INT >= 33 && drawable instanceof BlurredBackgroundDrawableRenderNode renderNode) {
            renderNode.setLiquidGlassEffectAllowed(false);
        }
        return drawable;
    }

    public static Drawable control(BlurredBackgroundDrawableViewFactory factory, View owner,
                                   Theme.ResourcesProvider provider) {
        BlurredBackgroundDrawable drawable = surface(factory, owner, provider, style(), true);
        drawable.setPadding(AndroidUtilities.dp(6));
        drawable.setRadius(AndroidUtilities.dp(20));
        return drawable;
    }
}
