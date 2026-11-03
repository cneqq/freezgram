package app.freezgram;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.ViewGroup;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.chat.ViewPositionWatcher;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;

/** Shared renderer for actual messages and settings previews. Captures wallpaper only. */
public final class MessageSurfaceRenderer {
    private final ViewGroup root;
    private final BlurredBackgroundSource wallpaper;
    private final Theme.ResourcesProvider provider;
    private final RectF cellBounds = new RectF();
    private final BlurredBackgroundSourceRenderNode[] messageSources = new BlurredBackgroundSourceRenderNode[4];
    private final long[] messageFrames = {Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE, Long.MIN_VALUE};
    public MessageSurfaceRenderer(ViewGroup root, BlurredBackgroundSource wallpaper, Theme.ResourcesProvider provider) {
        this.root = root; this.wallpaper = wallpaper; this.provider = provider;
    }
    public boolean draw(ChatMessageCell cell, Canvas canvas, RectF bounds, int profile,
            MessageSurfaceStyle.Surface surface, float alpha) {
        return drawInternal(cell, canvas, bounds, profile, surface, alpha, 0);
    }
    public org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable decoration(ChatMessageCell cell, Canvas canvas, RectF bounds,
            float radius, org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable cached) {
        MessageSurfaceStyle.Surface surface = new MessageSurfaceStyle.Surface();
        surface.glass = cached; surface.glassProfile = 3;
        return drawInternal(cell, canvas, bounds, 3, surface, 1f, radius) ? surface.glass : null;
    }
    private boolean drawInternal(ChatMessageCell cell, Canvas canvas, RectF bounds, int profile,
            MessageSurfaceStyle.Surface surface, float alpha, float radius) {
        if (!(profile == 3 ? SmallElementsGlass.areMessageDecorationsEnabled() : MessageSurfaceStyle.glassEnabled(profile)) || !canvas.isHardwareAccelerated() || root == null) return false;
        if (!ViewPositionWatcher.computeRectInParent(cell, root, cellBounds)) return false;
        int width = root.getWidth(), height = root.getHeight();
        if (width <= 0 || height <= 0) return false;
        var source = messageSources[profile];
        if (source == null) source = messageSources[profile] = new BlurredBackgroundSourceRenderNode(wallpaper);
        long frame = root.getDrawingTime();
        if (messageFrames[profile] != frame || source.needUpdateDisplayList(width, height)) {
            messageFrames[profile] = frame;
            Canvas capture = source.beginRecording(width, height);
            wallpaper.draw(capture, 0, 0, width, height); source.endRecording();
            source.setBlur(AndroidUtilities.dpf2((profile == 3 ? LiquidGlassStyle.blurRadiusDp() : MessageSurfaceStyle.blur(profile))));
            source.invalidateDisplayListForDrawables();
        }
        if (surface.glass == null || surface.glassProfile != profile) {
            if (surface.glass != null) surface.glass.setCallback(null);
            surface.glassProfile = profile;
            surface.glass = source.createDrawable(); surface.glass.setCallback(cell);
            surface.glass.setColorProvider(profile == 3 ? LiquidGlassStyle.surfaceColors(provider, SmallElementsGlass::areMessageDecorationsEnabled) : MessageSurfaceStyle.colors(provider, profile));
        }
        var glass = surface.glass;
        glass.setSourceOffset(cellBounds.left, cellBounds.top);
        bounds.roundOut(AndroidUtilities.rectTmp2); glass.setBounds(AndroidUtilities.rectTmp2);
        glass.setRadius(radius); glass.setAlpha((int) (255 * alpha)); glass.setThickness(AndroidUtilities.dp(12));
        glass.setIntensity((profile == 3 ? app.exteraless.appearance.AppearanceConfig.navigationGlassIntensity() : MessageSurfaceStyle.intensity(profile)) / 100f);
        ((org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableRenderNode) glass).setLiquidGlassEffectAllowed(true);
        glass.updateColors(); glass.draw(canvas); return true;
    }

}
