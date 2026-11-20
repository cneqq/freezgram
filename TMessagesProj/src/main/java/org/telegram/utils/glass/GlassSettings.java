package org.telegram.utils.glass;

import java.util.Objects;

public class GlassSettings {

    public final boolean isBlurEnabled;
    public final boolean isLiquidGlassEnabled;
    public final boolean isFreezgramGlassEnabled;

    public GlassSettings(boolean isBlurEnabled, boolean isLiquidGlassEnabled, boolean isFreezgramGlassEnabled) {
        this.isBlurEnabled = isBlurEnabled;
        this.isLiquidGlassEnabled = isLiquidGlassEnabled;
        this.isFreezgramGlassEnabled = isFreezgramGlassEnabled;
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
                && isFreezgramGlassEnabled == settings.isFreezgramGlassEnabled;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isBlurEnabled, isLiquidGlassEnabled, isFreezgramGlassEnabled);
    }
}
