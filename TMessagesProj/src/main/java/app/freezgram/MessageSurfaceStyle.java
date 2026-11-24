package app.freezgram;

import android.os.Build;
import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProvider;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProviderBuilder;

/** Incoming settings also serve comments; optional linking applies them to outgoing messages. */
public final class MessageSurfaceStyle {
    private MessageSurfaceStyle() {}
    public static int settingsProfile(int profile) {
        return profile == 1 && !AppearanceConfig.sameMessageSurfaces() ? 1 : 0;
    }
    public static int style(int profile) {
        if (AppearanceConfig.iosMode()) {
            return 0;
        }
        return settingsProfile(profile) == 1 ? AppearanceConfig.outgoingSurfaceStyle() : AppearanceConfig.incomingSurfaceStyle();
    }
    public static int opacity(int profile) {
        return settingsProfile(profile) == 1 ? AppearanceConfig.outgoingSurfaceOpacity() : AppearanceConfig.incomingSurfaceOpacity();
    }
    public static int blur(int profile) {
        return settingsProfile(profile) == 1 ? AppearanceConfig.outgoingSurfaceBlur() : AppearanceConfig.incomingSurfaceBlur();
    }
    public static int intensity(int profile) {
        return settingsProfile(profile) == 1 ? AppearanceConfig.outgoingSurfaceIntensity() : AppearanceConfig.incomingSurfaceIntensity();
    }
    public static boolean glassEnabled(int profile) {
        return style(profile) == 1 && Build.VERSION.SDK_INT >= 33 && SharedConfig.chatBlurEnabled() && !LiteMode.isGlassPowerSaverApplied();
    }
    public static int baseColor(Theme.ResourcesProvider provider, int profile) {
        return Theme.getColor(settingsProfile(profile) == 1 ? Theme.key_chat_outBubble : Theme.key_chat_inBubble, provider) | 0xff000000;
    }
    public static BlurredBackgroundProvider colors(Theme.ResourcesProvider provider, int profile) {
        return new BlurredBackgroundProviderBuilder(provider)
                .setBackgroundColor((r, dark) -> Theme.multAlpha(baseColor(r, profile), opacity(profile) / 100f))
                .setStrokeColorTop(0x66ffffff, 0x66ffffff).setStrokeColorBottom(0x22000000, 0x33000000)
                .setStrokeWidth(0, 0).build();
    }
    public static final class Surface {
        public BlurredBackgroundDrawable glass;
        public int glassProfile = -1;
    }
}
