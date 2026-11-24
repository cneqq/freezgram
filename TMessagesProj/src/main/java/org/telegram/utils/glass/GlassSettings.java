package org.telegram.utils.glass;

import java.util.Objects;

public class GlassSettings {

    public final boolean isBlurEnabled;
    public final boolean isLiquidGlassEnabled;
    public final boolean isFreezgramGlassEnabled;
    public final boolean iosMode;

    public GlassSettings(boolean isBlurEnabled, boolean isLiquidGlassEnabled, boolean isFreezgramGlassEnabled, boolean iosMode) {
        this.isBlurEnabled = isBlurEnabled;
        this.isLiquidGlassEnabled = isLiquidGlassEnabled;
        this.isFreezgramGlassEnabled = isFreezgramGlassEnabled;
        this.iosMode = iosMode;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof GlassSettings)) {
            return false;
        }

        GlassSettings settings = (GlassSettings) object;
        return isBlurEnabled == settings.isBlurEnabled
                && isLiquidGlassEnabled == settings.isLiquidGlassEnabled
                && isFreezgramGlassEnabled == settings.isFreezgramGlassEnabled
                && iosMode == settings.iosMode;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isBlurEnabled, isLiquidGlassEnabled, isFreezgramGlassEnabled, iosMode);
    }
}
