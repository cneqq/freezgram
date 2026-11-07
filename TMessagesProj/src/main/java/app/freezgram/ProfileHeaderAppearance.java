package app.freezgram;

import android.graphics.drawable.Drawable;
import android.view.View;

import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;

public final class ProfileHeaderAppearance {
    public static final int CLASSIC = 0;
    public static final int GLASS = 1;

    private ProfileHeaderAppearance() {}

    public static int style() {
        int style = AppearanceConfig.profileHeaderStyle();
        return style == GLASS && !LiquidGlassStyle.canDrawGlass() ? CLASSIC : style;
    }

    public static Drawable control(BlurredBackgroundDrawableViewFactory factory, View owner,
                                   Theme.ResourcesProvider provider) {
        BlurredBackgroundDrawable drawable = LiquidGlassStyle.createSurface(factory, owner, provider);
        drawable.setPadding(AndroidUtilities.dp(6));
        drawable.setRadius(AndroidUtilities.dp(20));
        return drawable;
    }
}
