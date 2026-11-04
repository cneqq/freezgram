package app.freezgram;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewParent;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableRenderNode;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceColor;

/** Small counters reuse their panel's GPU source. Never capture or redraw the window. */
public final class CounterGlass {
    private final BlurredBackgroundSourceColor flatSource = new BlurredBackgroundSourceColor();
    private BlurredBackgroundSource source;
    private BlurredBackgroundDrawableRenderNode glass;
    private final Rect bounds = new Rect();
    private final int[] ownerLocation = new int[2], panelLocation = new int[2];

    public boolean draw(Canvas canvas, View owner, RectF rect, Theme.ResourcesProvider provider, float radiusPx) {
        if (!SmallElementsGlass.areUnreadCountersEnabled()) { release(); return false; }
        if (!canvas.isHardwareAccelerated() || AndroidUtilities.makingGlobalBlurBitmap) return false;
        // List rows have a uniform background. Tabs can reuse an existing live blurred
        // panel source, including its offset, without recording the list a second time.
        flatSource.setColor(Theme.getColor(Theme.key_windowBackgroundWhite, provider));
        BlurredBackgroundSource next = flatSource;
        float offsetX = 0, offsetY = 0;
        for (ViewParent parent = owner.getParent(); parent instanceof View panel; parent = panel.getParent()) {
            if (panel.getBackground() instanceof BlurredBackgroundDrawable background) {
                next = background.getSource();
                owner.getLocationOnScreen(ownerLocation); panel.getLocationOnScreen(panelLocation);
                offsetX = background.getSourceOffsetX() + ownerLocation[0] - panelLocation[0];
                offsetY = background.getSourceOffsetY() + ownerLocation[1] - panelLocation[1];
                break;
            }
        }
        if (next == null) return false;
        if (glass == null || source != next) {
            release(); source = next;
            glass = new BlurredBackgroundDrawableRenderNode(source);
            glass.setColorProvider(LiquidGlassStyle.surfaceColors(provider, SmallElementsGlass::areUnreadCountersEnabled));
            glass.setCallback(owner);
            glass.setLiquidGlassEffectAllowed(true);
        }
        rect.roundOut(bounds); glass.setBounds(bounds); glass.setRadius(radiusPx);
        glass.setSourceOffset(offsetX, offsetY);
        LiquidGlassStyle.configure(glass); glass.updateColors(); glass.draw(canvas);
        return true;
    }

    public void release() {
        if (glass != null) glass.setCallback(null);
        glass = null; source = null;
    }
}
