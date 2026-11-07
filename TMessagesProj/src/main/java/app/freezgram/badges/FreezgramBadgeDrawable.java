package app.freezgram.badges;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import org.telegram.messenger.AndroidUtilities;

/** Community plane on the red circular plate from the supplied extera icon. */
public final class FreezgramBadgeDrawable extends Drawable {
    private final Drawable plane;
    private final boolean hasPlate;
    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint planePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint layerPaint = new Paint();

    public FreezgramBadgeDrawable(Drawable plane, boolean hasPlate) {
        this.plane = plane;
        this.hasPlate = hasPlate;
        plane.setColorFilter(new PorterDuffColorFilter(hasPlate ? 0xFFFFFFFF : 0xFFE83030, PorterDuff.Mode.SRC_IN));
        backgroundPaint.setColor(0xFFE83030);
        planePaint.setShader(new LinearGradient(0, 3, 0, 24,
                0xE6FFFFFF, 0x8AFFFFFF, Shader.TileMode.CLAMP));
        planePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
    }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        int save = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(bounds.width() / 32f, bounds.height() / 28f);
        int badgeLayer = canvas.saveLayer(0, 0, 32, 28, layerPaint);
        int planeLayer = -1;
        if (hasPlate) {
            canvas.drawCircle(16, 14, 13, backgroundPaint);
            planeLayer = canvas.saveLayer(0, 0, 32, 28, null);
        }
        int planeSave = canvas.save();
        // Keep the existing plane's optical centering inside the circular plate.
        canvas.translate(16, 14);
        if (hasPlate) {
            canvas.scale(.72f, .72f);
        }
        canvas.translate(1.518f - 16, .558f - 14);
        int size = AndroidUtilities.dp(26);
        canvas.scale(26f / size, 26f / size);
        plane.setBounds(0, 0, size, size);
        plane.draw(canvas);
        canvas.restoreToCount(planeSave);
        if (hasPlate) {
            canvas.drawRect(0, 0, 32, 28, planePaint);
            canvas.restoreToCount(planeLayer);
        }
        canvas.restoreToCount(badgeLayer);
        canvas.restoreToCount(save);
    }

    @Override public int getIntrinsicWidth() { return AndroidUtilities.dp(32); }
    @Override public int getIntrinsicHeight() { return AndroidUtilities.dp(28); }
    @Override public void setAlpha(int alpha) { layerPaint.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { layerPaint.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
