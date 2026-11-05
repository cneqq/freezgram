package app.freezgram;

import android.graphics.Canvas;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.utils.RenderNodeEffects;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;

/** One reduced wallpaper texture per frame, shared by all inline controls. */
public final class DownsampledGlassSource extends BlurredBackgroundSourceRenderNode {
    private final BlurredBackgroundSource wallpaper;
    private long lastFrame = Long.MIN_VALUE;
    private int lastRevision = Integer.MIN_VALUE, lastWidth, lastHeight;
    private int scale = 1;

    public DownsampledGlassSource(BlurredBackgroundSource wallpaper) {
        super(wallpaper); this.wallpaper = wallpaper;
    }

    public void update(long frame, int revision, int width, int height, int blurDp, boolean enhanceColors) {
        int nextScale = blurDp >= 4 ? 4 : 1;
        int textureWidth = Math.max(1, (width + nextScale - 1) / nextScale);
        int textureHeight = Math.max(1, (height + nextScale - 1) / nextScale);
        if (frame == lastFrame && revision == lastRevision && width == lastWidth && height == lastHeight
                && scale == nextScale && !needUpdateDisplayList(textureWidth, textureHeight)) return;
        scale = nextScale; lastFrame = frame; lastRevision = revision; lastWidth = width; lastHeight = height;
        Canvas capture = beginRecording(textureWidth, textureHeight);
        capture.scale(1f / scale, 1f / scale);
        wallpaper.draw(capture, 0, 0, width, height);
        endRecording();
        setBlur(AndroidUtilities.dpf2(blurDp) / scale,
                enhanceColors ? RenderNodeEffects.getSaturationX3RenderEffect() : null);
        invalidateDisplayListForDrawables();
    }

    @Override public void draw(Canvas canvas, float left, float top, float right, float bottom) {
        if (!canvas.isHardwareAccelerated()) { wallpaper.draw(canvas, left, top, right, bottom); return; }
        canvas.save(); canvas.scale(scale, scale);
        super.draw(canvas, left / scale, top / scale, right / scale, bottom / scale);
        canvas.restore();
    }
}
