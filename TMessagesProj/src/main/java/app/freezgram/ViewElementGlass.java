package app.freezgram;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.Rect;
import android.view.View;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/** A separate material per view; hides its owner only during guarded background capture. */
public final class ViewElementGlass {
    private final SmallElementsGlass.NotificationBackdrop backdrop;
    private final Rect bounds = new Rect();
    private final RectF original = new RectF();
    public ViewElementGlass(java.util.function.BooleanSupplier enabled) { backdrop = new SmallElementsGlass.NotificationBackdrop(enabled, true); }
    public boolean draw(Canvas canvas, View owner, RectF rect, Theme.ResourcesProvider provider, float radiusPx) {
        if (AndroidUtilities.makingGlobalBlurBitmap) return false;
        original.set(rect); rect.roundOut(bounds);
        try { return backdrop.draw(canvas, owner, bounds, provider, Math.round(radiusPx / AndroidUtilities.density)); }
        finally { rect.set(original); }
    }
    public void setDrawStroke(boolean drawStroke) { backdrop.setDrawStroke(drawStroke); }
    public void release() { backdrop.release(); }
}
