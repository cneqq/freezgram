package app.freezgram;

import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import org.telegram.ui.ActionBar.Theme;

public final class IosChatListColors {
    private IosChatListColors() {
    }

    public static int background(Theme.ResourcesProvider provider) {
        return isDark(provider) ? Color.BLACK : Theme.getColor(Theme.key_windowBackgroundWhite, provider);
    }

    public static int pinnedBackground(Theme.ResourcesProvider provider) {
        int text = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, provider);
        return ColorUtils.blendARGB(background(provider), text, isDark(provider) ? .11f : .045f);
    }

    public static int separator(Theme.ResourcesProvider provider, boolean pinned) {
        int background = pinned ? pinnedBackground(provider) : background(provider);
        int text = Theme.getColor(Theme.key_windowBackgroundWhiteBlackText, provider);
        return ColorUtils.blendARGB(background, text, .12f);
    }

    private static boolean isDark(Theme.ResourcesProvider provider) {
        return provider != null ? provider.isDark() : Theme.isCurrentThemeDark();
    }
}
