package app.freezgram;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.os.Build;
import android.view.View;
import android.view.ViewTreeObserver;
import java.lang.ref.WeakReference;
import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableRenderNode;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceBitmap;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;
import org.telegram.ui.Components.blur3.utils.Blur3Utils;

/** Shared opt-in for small app surfaces; system notifications are not affected. */
public final class SmallElementsGlass {
    private SmallElementsGlass() {}
    public static boolean isEnabled() {
        return AppearanceConfig.smallElementsGlass() && Build.VERSION.SDK_INT >= 33
                && SharedConfig.chatBlurEnabled() && !LiteMode.isGlassPowerSaverApplied();
    }

    public static boolean areUnreadCountersEnabled() {
        return AppearanceConfig.unreadCountersGlass() && Build.VERSION.SDK_INT >= 33 && SharedConfig.chatBlurEnabled() && !LiteMode.isGlassPowerSaverApplied();
    }
    public static boolean areMessageDecorationsEnabled() {
        return AppearanceConfig.messageDecorationsGlass() && Build.VERSION.SDK_INT >= 33 && SharedConfig.chatBlurEnabled() && !LiteMode.isGlassPowerSaverApplied();
    }

    public static boolean areServiceBubblesEnabled() {
        return isEnabled() || MessageSurfaceStyle.glassEnabled(0);
    }

    public static boolean areGiftCardsEnabled() {
        return GiftGlassStyle.style() == GiftGlassStyle.GLASS && Build.VERSION.SDK_INT >= 33
                && SharedConfig.chatBlurEnabled() && !LiteMode.isGlassPowerSaverApplied();
    }

    /** Bridge existing bitmap captures into the same AGSL renderer used by the panels. */
    public static BlurredBackgroundSource createSource(BlurredBackgroundSource source) {
        return source instanceof SnapshotSource ? source : new SnapshotSource(source);
    }

    public static BlurredBackgroundSource createFrostedSource(BlurredBackgroundSource source) {
        return new SnapshotSource(source, false, 18);
    }

    /** Gift optics are independent; unwrap an existing bridge to avoid blurring twice. */
    public static BlurredBackgroundSource createGiftSource(BlurredBackgroundSource source) {
        return new SnapshotSource(source instanceof SnapshotSource ? ((SnapshotSource) source).input : source, true);
    }

    private static final class SnapshotSource extends BlurredBackgroundSourceRenderNode {
        private final BlurredBackgroundSource input;
        private final boolean gift;
        private final int minimumBlur;
        private Bitmap lastBitmap;
        private BlurredBackgroundSourceRenderNode recording;
        private int lastRevision = Integer.MIN_VALUE;
        private int lastLeft = Integer.MIN_VALUE, lastTop = Integer.MIN_VALUE;
        SnapshotSource(BlurredBackgroundSource source) { this(source, false); }
        SnapshotSource(BlurredBackgroundSource source, boolean gift) { this(source, gift, 0); }
        SnapshotSource(BlurredBackgroundSource source, boolean gift, int minimumBlur) {
            super(source);
            input = source;
            this.gift = gift;
            this.minimumBlur = minimumBlur;
        }
        @Override public BlurredBackgroundDrawable createDrawable() {
            // Each shape owns its recording. Reaction bubbles must not resize the main panel's source.
            return new BlurredBackgroundDrawableRenderNode(new SnapshotSource(input, gift, minimumBlur));
        }
        @Override public void draw(Canvas canvas, float left, float top, float right, float bottom) {
            if (!canvas.isHardwareAccelerated()) { input.draw(canvas, left, top, right, bottom); return; }
            int padding = AndroidUtilities.dp(64);
            int originX = (int) Math.floor(left) - padding, originY = (int) Math.floor(top) - padding;
            int width = Math.max(1, (int) Math.ceil(right) - originX + padding);
            int height = Math.max(1, (int) Math.ceil(bottom) - originY + padding);
            Bitmap bitmap = input instanceof BlurredBackgroundSourceBitmap ? ((BlurredBackgroundSourceBitmap) input).getBitmap() : null;
            int revision = gift ? GiftGlassStyle.revision() : LiquidGlassStyle.revision();
            boolean geometryChanged = recording == null || originX != lastLeft || originY != lastTop
                    || recording.needUpdateDisplayList(width, height);
            if (bitmap == null || bitmap != lastBitmap || revision != lastRevision || geometryChanged) {
                // A shared wallpaper under-source can be recorded into several glass shapes.
                // Keep previous nodes alive with their original geometry: the canvas retains
                // references to them, not copies of their contents.
                if (geometryChanged) recording = new BlurredBackgroundSourceRenderNode(input);
                lastBitmap = bitmap; lastRevision = revision; lastLeft = originX; lastTop = originY;
                Canvas capture = recording.beginRecording(width, height);
                capture.translate(-originX, -originY);
                input.draw(capture, originX, originY, originX + width, originY + height);
                recording.endRecording();
                recording.setBlur(AndroidUtilities.dpf2(Math.max(minimumBlur, gift ? GiftGlassStyle.blurRadiusDp() : LiquidGlassStyle.blurRadiusDp())), !gift && LiquidGlassStyle.enhanceColors()
                        ? org.telegram.messenger.utils.RenderNodeEffects.getSaturationX3RenderEffect() : null);
            }
            canvas.save();
            canvas.translate(originX, originY);
            recording.draw(canvas, left - originX, top - originY, right - originX, bottom - originY);
            canvas.restore();
        }
    }

    /** Scroll-aware in-memory backdrop. Released when its notice detaches. */
    public static final class NotificationBackdrop {
        private static View excludedOwner;
        public static boolean isExcluded(View view) { return excludedOwner == view; }
        private final java.util.function.BooleanSupplier enabled;
        private final boolean hideOwner;
        private final LiveGlassBackdrop liveBackdrop = new LiveGlassBackdrop();
        public NotificationBackdrop() { this(SmallElementsGlass::isEnabled, false); }
        public NotificationBackdrop(java.util.function.BooleanSupplier enabled, boolean hideOwner) { this.enabled = enabled; this.hideOwner = hideOwner; }
        private final BlurredBackgroundSourceBitmap bitmapSource = new BlurredBackgroundSourceBitmap();
        private BlurredBackgroundDrawable glass;
        private Bitmap bitmap;
        private View capturedRoot;
        private int rootWidth, rootHeight;
        private boolean captureDirty;
        private boolean refreshOnScroll = true;
        private boolean drawStroke = true;

        public void setDrawStroke(boolean drawStroke) {
            if (this.drawStroke != drawStroke) {
                this.drawStroke = drawStroke;
                release();
            }
        }

        public void freezeOnScroll() {
            refreshOnScroll = false;
            release();
        }
        private ViewTreeObserver scrollObserver;
        private WeakReference<View> capturedOwner;
        private final ViewTreeObserver.OnScrollChangedListener scrollListener = () -> {
            captureDirty = true;
            View owner = capturedOwner == null ? null : capturedOwner.get();
            if (owner != null) owner.invalidate();
        };
        private final int[] ownerLocation = new int[2], rootLocation = new int[2];

        public boolean draw(Canvas canvas, View owner, Rect bounds, Theme.ResourcesProvider provider, int radiusDp) {
            if (!enabled.getAsBoolean()) { release(); return false; }
            if (!canvas.isHardwareAccelerated() || AndroidUtilities.makingGlobalBlurBitmap) return false;
            if (refreshOnScroll && liveBackdrop.attach(owner)) {
                releaseSnapshot();
                return liveBackdrop.draw(canvas, owner, bounds, provider, radiusDp, enabled, hideOwner, drawStroke);
            }
            View root = owner.getRootView();
            if (root.getWidth() <= 0 || root.getHeight() <= 0) return false;
            if (bitmap == null || root != capturedRoot || rootWidth != root.getWidth() || rootHeight != root.getHeight()) {
                release(); capturedRoot = root; rootWidth = root.getWidth(); rootHeight = root.getHeight();
                if (refreshOnScroll) {
                    capturedOwner = new WeakReference<>(owner);
                    scrollObserver = root.getViewTreeObserver();
                    scrollObserver.addOnScrollChangedListener(scrollListener);
                }
                captureDirty = true;
            }
            if (captureDirty) {
                captureDirty = false;
                // Bulletin/Undo draw guards exclude notices from this capture, including this owner.
                float alpha = owner.getAlpha();
                View previousExcluded = excludedOwner;
                try {
                    if (hideOwner) excludedOwner = owner;
                    if (hideOwner && owner != root) owner.setAlpha(0f);
                    AndroidUtilities.makeGlobalBlurBitmap(value -> { bitmap = value; bitmapSource.setBitmap(value); }, 4f, 0, root, null);
                } finally {
                    excludedOwner = previousExcluded;
                    if (hideOwner && owner != root) owner.setAlpha(alpha);
                }
                if (bitmap == null) { captureDirty = true; return false; }
                Blur3Utils.checkBitmapSourceMatrixScale(bitmapSource, root);
                if (glass == null) {
                    glass = createSource(bitmapSource).createDrawable();
                    glass.setColorProvider(hideOwner ? LiquidGlassStyle.surfaceColors(provider, enabled, drawStroke) : LiquidGlassStyle.colors(provider, enabled));
                }
            }
            owner.getLocationOnScreen(ownerLocation); root.getLocationOnScreen(rootLocation);
            glass.setSourceOffset(ownerLocation[0] - rootLocation[0], ownerLocation[1] - rootLocation[1]);
            glass.setBounds(bounds); glass.setRadius(AndroidUtilities.dp(radiusDp));
            LiquidGlassStyle.configure(glass);
            ((BlurredBackgroundDrawableRenderNode) glass).setLiquidGlassEffectAllowed(true);
            glass.updateColors(); glass.draw(canvas);
            return true;
        }

        public void release() {
            liveBackdrop.release();
            releaseSnapshot();
        }

        private void releaseSnapshot() {
            if (scrollObserver != null && scrollObserver.isAlive()) scrollObserver.removeOnScrollChangedListener(scrollListener);
            scrollObserver = null; capturedOwner = null; captureDirty = false;
            glass = null; bitmapSource.setBitmap(null);
            // RenderNodes can still reference the old image until the frame completes; let GC release it.
            bitmap = null; capturedRoot = null;
        }
    }
}
