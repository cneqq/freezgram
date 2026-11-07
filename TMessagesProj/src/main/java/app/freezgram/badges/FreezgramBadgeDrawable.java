package app.freezgram.badges;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import org.telegram.messenger.AndroidUtilities;

/** Full-color community plane, with no background or animation. */
public final class FreezgramBadgeDrawable extends Drawable {
    private final Drawable plane;

    public FreezgramBadgeDrawable(Drawable plane) { this.plane = plane; }

    @Override
    public void draw(Canvas canvas) {
        Rect bounds = getBounds();
        if (bounds.isEmpty()) return;
        int save = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        canvas.scale(bounds.width() / 32f, bounds.height() / 28f);
        // Optical centering accounts for the asymmetric plane and transparent margins.
        canvas.translate(1.518f, .558f);
        int size = AndroidUtilities.dp(26);
        canvas.scale(26f / size, 26f / size);
        plane.setBounds(0, 0, size, size);
        plane.draw(canvas);
        canvas.restoreToCount(save);
    }

    @Override public int getIntrinsicWidth() { return AndroidUtilities.dp(32); }
    @Override public int getIntrinsicHeight() { return AndroidUtilities.dp(28); }
    @Override public void setAlpha(int alpha) { plane.setAlpha(alpha); invalidateSelf(); }
    @Override public void setColorFilter(ColorFilter filter) { plane.setColorFilter(filter); invalidateSelf(); }
    @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
}
