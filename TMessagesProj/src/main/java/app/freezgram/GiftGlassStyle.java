package app.freezgram;

import app.exteraless.appearance.AppearanceConfig;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import androidx.core.graphics.ColorUtils;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProvider;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProviderBuilder;

/** Gift-only optics, shared by received cards and the sending preview. */
public final class GiftGlassStyle {
    private GiftGlassStyle() {}
    public static final int ORDINARY = 0, GLASS = 1, SOFT = 2;
    public static int style() {
        if (AppearanceConfig.iosMode()) {
            return ORDINARY;
        }
        // Preserve the former gift-glass opt-out when upgrading or restoring backups.
        return AppearanceConfig.giftCardsGlass() ? AppearanceConfig.giftCardStyle() : ORDINARY;
    }

    public static float cornerRadius(float nativeRadius) {
        if (AppearanceConfig.iosMode()) {
            return nativeRadius;
        }
        int corners = AppearanceConfig.giftCardCorners();
        return corners < 0 ? nativeRadius : AndroidUtilities.dpf2(corners);
    }
    public static boolean customOrdinary() {
        return !AppearanceConfig.iosMode()
                && (AppearanceConfig.giftOrdinaryOpacity() != 100 || AppearanceConfig.giftCardCorners() >= 0);
    }
    public static void drawOrdinary(Canvas canvas, RectF bounds, float radius, Paint fill, Paint darken) {
        canvas.saveLayerAlpha(bounds, (int) (255 * AppearanceConfig.giftOrdinaryOpacity() / 100f));
        canvas.drawRoundRect(bounds, cornerRadius(radius), cornerRadius(radius), fill);
        if (darken != null) canvas.drawRoundRect(bounds, cornerRadius(radius), cornerRadius(radius), darken);
        canvas.restore();
    }

    /** Theme-colored satin surface. Each card owns its shader and geometry. */
    public static final class SoftCard {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF edge = new RectF();
        private float left, top, right, bottom;
        private int lastAccent, lastBase;
        private boolean lastOverlay;
        private int lastStrength, lastAngle;
        private LinearGradient gradient;
        public void draw(Canvas canvas, RectF bounds, float radius, Theme.ResourcesProvider provider, float alpha, boolean overlay) {
            int accent = Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, provider);
            int base = Theme.getColor(Theme.key_windowBackgroundWhite, provider) | 0xff000000;
            boolean lightText = ColorUtils.calculateLuminance(Theme.getColor(Theme.key_chat_serviceText, provider)) > .5;
            if (lightText == (ColorUtils.calculateLuminance(base) > .5)) base = lightText ? 0xff202631 : 0xffeef2f7;
            int strength = AppearanceConfig.giftGradientStrength(), angle = AppearanceConfig.giftGradientAngle();
            if (gradient == null || lastStrength != strength || lastAngle != angle || left != bounds.left || top != bounds.top || right != bounds.right || bottom != bounds.bottom
                    || accent != lastAccent || base != lastBase || overlay != lastOverlay) {
                left = bounds.left; top = bounds.top; right = bounds.right; bottom = bounds.bottom;
                lastAccent = accent; lastBase = base; lastOverlay = overlay;
                lastStrength = strength; lastAngle = angle;
                double radians = Math.toRadians(angle);
                float cx = (left + right) / 2f, cy = (top + bottom) / 2f;
                float length = (float) (Math.abs((right - left) * Math.cos(radians)) + Math.abs((bottom - top) * Math.sin(radians))) / 2f;
                float dx = (float) Math.cos(radians) * length, dy = (float) Math.sin(radians) * length;
                gradient = new LinearGradient(cx - dx, cy - dy, cx + dx, cy + dy,
                        overlay ? ((int) (strength * 2.55f) << 24) | 0x00ffffff : ColorUtils.blendARGB(base, accent, strength / 100f) | 0xff000000,
                        overlay ? 0x06000000 : ColorUtils.blendARGB(base, accent, strength / 100f * .375f) | 0xff000000, Shader.TileMode.CLAMP);
            }
            paint.setShader(gradient); paint.setStyle(Paint.Style.FILL);
            float opacity = AppearanceConfig.giftGradientOpacity() / 100f;
            radius = cornerRadius(radius);
            paint.setAlpha((int) (255 * alpha * opacity)); canvas.drawRoundRect(bounds, radius, radius, paint);
            paint.setShader(null); paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(AndroidUtilities.dpf2(1));
            paint.setColor(0x30ffffff); paint.setAlpha((int) (255 * AppearanceConfig.giftGradientOutline() / 100f * alpha * opacity));
            edge.set(bounds); edge.inset(AndroidUtilities.dpf2(.5f), AndroidUtilities.dpf2(.5f));
            canvas.drawRoundRect(edge, radius, radius, paint);
        }
    }

    public static int blurRadiusDp() {
        return AppearanceConfig.giftGlassBlur();
    }
    public static int revision() {
        return (blurRadiusDp() * 101 + AppearanceConfig.giftGlassIntensity()) * 101 + AppearanceConfig.giftGlassTransparency();
    }
    public static float opacity() {
        return (100 - AppearanceConfig.giftGlassTransparency()) / 100f;
    }
    public static void configure(BlurredBackgroundDrawable drawable) {
        drawable.setFreezgramGlass(true);
        drawable.setThickness(AndroidUtilities.dp(12));
        drawable.setIntensity(AppearanceConfig.giftGlassIntensity() / 100f);
    }
    public static BlurredBackgroundProvider colors(Theme.ResourcesProvider provider) {
        return new BlurredBackgroundProviderBuilder(provider)
                .setBackgroundColor((r, dark) -> Theme.multAlpha(Theme.getColor(Theme.key_windowBackgroundWhite, r), opacity()))
                .setStrokeColorTop(0x99ffffff, 0x99ffffff)
                .setStrokeColorBottom(0x40000000, 0x55ffffff)
                .setStrokeWidth(AndroidUtilities.dpf2(1.2f), AndroidUtilities.dpf2(.8f))
                .setShadowColor(0x24000000, 0x33000000)
                .setShadowLayer(AndroidUtilities.dpf2(6), 0, AndroidUtilities.dpf2(2)).build();
    }
}
