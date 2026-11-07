package app.freezgram;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.view.View;

import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;

public final class ProfileHeaderAppearance {
    public static final int CLASSIC = 0;
    public static final int GLASS = 1;
    public static final int CONTRAST = 2;

    private ProfileHeaderAppearance() {}

    public static int style() {
        int style = AppearanceConfig.profileHeaderStyle();
        return style == GLASS && !LiquidGlassStyle.canDrawGlass() ? CLASSIC : style;
    }

    public static Drawable control(BlurredBackgroundDrawableViewFactory factory, View owner,
                                   Theme.ResourcesProvider provider, int style) {
        if (style == GLASS) {
            BlurredBackgroundDrawable drawable = LiquidGlassStyle.createSurface(factory, owner, provider);
            drawable.setPadding(AndroidUtilities.dp(6));
            drawable.setRadius(AndroidUtilities.dp(20));
            return drawable;
        }
        int fill = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, provider);
        return new InsetDrawable(Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(40),
                Theme.multAlpha(fill, .18f), Theme.getColor(Theme.key_listSelector, provider)),
                AndroidUtilities.dp(5), AndroidUtilities.dp(6), AndroidUtilities.dp(5), AndroidUtilities.dp(6));
    }
}
