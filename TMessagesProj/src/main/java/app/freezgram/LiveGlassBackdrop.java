package app.freezgram;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import java.util.function.BooleanSupplier;
import java.util.ArrayList;
import java.util.WeakHashMap;
import java.lang.ref.WeakReference;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableRenderNode;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;
import org.telegram.ui.Components.chat.ViewPositionWatcher;

/** Reuses the screen's existing GPU scene; never draws the root or allocates a bitmap. */
public final class LiveGlassBackdrop {
    public static void install(ViewGroup scope, BlurredBackgroundSource source) {
        scope.setTag(R.id.freezgram_live_glass_source, source == null ? null : new Scene(source));
    }
    /** Bulletins live beside the fragment view in the navigation container. */
    public static void bind(View overlay, View screen) {
        overlay.setTag(R.id.freezgram_live_glass_scope, screen instanceof ViewGroup ? new WeakReference<>(screen) : null);
    }
    private static final class Scene {
        final BlurredBackgroundSource input, blurred;
        final WeakHashMap<View, Boolean> owners = new WeakHashMap<>();
        final int[] ownerLocation = new int[2], scopeLocation = new int[2];
        Scene(BlurredBackgroundSource input) { this.input = input; blurred = SmallElementsGlass.createSource(input); }
    }

    /** Add only visible overlay regions to the screen's existing scroll capture. */
    public static int captureBounds(ViewGroup scope, ArrayList<RectF> base, int count, ArrayList<RectF> result) {
        for (int i = 0; i < count; i++) {
            if (i == result.size()) result.add(new RectF());
            result.get(i).set(base.get(i));
        }
        if (scope.getTag(R.id.freezgram_live_glass_source) instanceof Scene scene && LiquidGlassStyle.canDrawGlass()) {
            for (View owner : scene.owners.keySet()) {
                if (owner == null || !owner.isShown() || owner.getWidth() <= 0 || owner.getHeight() <= 0) continue;
                if (count == result.size()) result.add(new RectF());
                RectF bounds = result.get(count);
                if (locate(owner, scope, bounds, scene)) {
                    bounds.inset(-AndroidUtilities.dp(64), -AndroidUtilities.dp(64)); count++;
                }
            }
        }
        return count;
    }

    public static void invalidateOwners(ViewGroup scope) {
        if (SmallElementsGlass.areServiceBubblesEnabled() && scope.getTag(R.id.freezgram_live_glass_source) instanceof Scene scene) {
            for (View owner : scene.owners.keySet()) if (owner != null) owner.invalidate();
        }
    }

    private static boolean locate(View owner, ViewGroup scope, RectF bounds, Scene scene) {
        if (ViewPositionWatcher.computeRectInParent(owner, scope, bounds)) return true;
        if (owner.getRootView() != scope.getRootView()) return false;
        owner.getLocationOnScreen(scene.ownerLocation); scope.getLocationOnScreen(scene.scopeLocation);
        float x = scene.ownerLocation[0] - scene.scopeLocation[0];
        float y = scene.ownerLocation[1] - scene.scopeLocation[1];
        bounds.set(x, y, x + owner.getWidth(), y + owner.getHeight());
        return true;
    }

    private View owner;
    private ViewGroup scope;
    private Scene scene;
    private BlurredBackgroundDrawable glass, panelBackground;
    private final RectF position = new RectF();

    public boolean attach(View owner) {
        if (owner.getTag(R.id.freezgram_live_glass_scope) instanceof WeakReference<?> reference
                && reference.get() instanceof ViewGroup group
                && group.getTag(R.id.freezgram_live_glass_source) instanceof Scene next) {
            attachScene(owner, group, next); return true;
        }
        for (ViewParent ancestor = owner.getParent(); ancestor instanceof ViewGroup group; ancestor = group.getParent()) {
            if (group.getTag(R.id.freezgram_live_glass_source) instanceof Scene next) {
                attachScene(owner, group, next); return true;
            }
        }
        // Sheets can already have a blurred surface even without a screen registration.
        for (ViewParent ancestor = owner.getParent(); ancestor instanceof ViewGroup group; ancestor = group.getParent()) {
            if (group.getBackground() instanceof BlurredBackgroundDrawable background && background.getSource() != null) {
                if (scope != group || panelBackground != background || scene == null || scene.input != background.getSource() || this.owner != owner) {
                    release(); scope = group; scene = new Scene(background.getSource()); panelBackground = background; this.owner = owner;
                }
                return true;
            }
        }
        release(); return false;
    }

    private void attachScene(View owner, ViewGroup group, Scene next) {
        if (scope != group || scene != next || this.owner != owner) {
            release(); scope = group; scene = next; this.owner = owner; scene.owners.put(owner, true);
        }
    }

    public boolean draw(Canvas canvas, View owner, Rect bounds, Theme.ResourcesProvider provider,
            int radiusDp, BooleanSupplier enabled, boolean surface, boolean drawStroke) {
        if (scene == null || !canvas.isHardwareAccelerated() || AndroidUtilities.makingGlobalBlurBitmap || !enabled.getAsBoolean()) return false;
        // An overlay encountered while recording a source must not capture that source recursively.
        if (scene.input instanceof BlurredBackgroundSourceRenderNode node && node.inRecording()) return false;
        if (!locate(owner, scope, position, scene)) return false;
        if (glass == null) {
            glass = scene.blurred.createDrawable(); glass.setCallback(owner);
            ((BlurredBackgroundDrawableRenderNode) glass).setLiquidGlassEffectAllowed(true);
            glass.setColorProvider(surface ? LiquidGlassStyle.surfaceColors(provider, enabled, drawStroke) : LiquidGlassStyle.colors(provider, enabled));
        }
        glass.setSourceOffset(position.left + (panelBackground == null ? 0 : panelBackground.getSourceOffsetX()),
                position.top + (panelBackground == null ? 0 : panelBackground.getSourceOffsetY()));
        glass.setBounds(bounds); glass.setRadius(AndroidUtilities.dp(radiusDp));
        LiquidGlassStyle.configure(glass);
        glass.updateColors(); glass.draw(canvas);
        return true;
    }

    public boolean verifies(android.graphics.drawable.Drawable drawable) { return glass == drawable; }

    public void release() {
        if (glass != null) glass.setCallback(null);
        if (scene != null && owner != null) scene.owners.remove(owner);
        glass = null; scope = null; scene = null; panelBackground = null; owner = null;
    }
}
