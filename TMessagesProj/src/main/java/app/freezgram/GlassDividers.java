package app.freezgram;

import android.graphics.Canvas;
import android.graphics.Paint;
import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/** Shared line geometry for popup rows, chat top panels and their preview. */
public final class GlassDividers {
    private static final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private GlassDividers() {}
    public static float popupVisibility(int materialAlpha, float previousAlpha, float currentAlpha) {
        return Math.max(0f, Math.min(materialAlpha / 255f, Math.min(previousAlpha, currentAlpha)));
    }
    public static void draw(Canvas canvas, float left, float right, float y, float visibility, int color) {
        if (!AppearanceConfig.glassMenuDividers()) return;
        float inset = Math.min(AndroidUtilities.dp(AppearanceConfig.glassDividerInset()), Math.max(0, (right - left) / 2));
        float thickness = Math.max(1f, AndroidUtilities.dpf2(AppearanceConfig.glassDividerWeight() * .5f));
        paint.setColor(Theme.multAlpha(color, AppearanceConfig.glassDividerOpacity() / 100f * visibility));
        canvas.drawRect(left + inset, y, right - inset, y + thickness, paint);
    }
}
