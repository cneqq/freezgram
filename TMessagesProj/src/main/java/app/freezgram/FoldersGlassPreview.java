package app.freezgram;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.view.Gravity;
import app.exteraless.appearance.AppearanceConfig;
import app.exteraless.appearance.FoldersPreviewCell;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;
import org.telegram.ui.Components.chat.ViewPositionWatcher;
import org.telegram.ui.Stories.recorder.PreviewView;

/** Real FilterTabsView, with the same opt-in material as the existing shared preview. */
final class FoldersGlassPreview extends SizeNotifierFrameLayout {
    private final FoldersPreviewCell folders;
    private BlurredBackgroundSourceRenderNode source;
    private BlurredBackgroundDrawable glass;
    FoldersGlassPreview(Context context, int account, Theme.ResourcesProvider provider) {
        super(context);
        setBackgroundImage(PreviewView.getBackgroundDrawable(null, account, UserConfig.getInstance(account).getClientUserId(), Theme.isCurrentThemeDark()), false);
        folders = new FoldersPreviewCell(context, provider);
        addView(folders, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 86, Gravity.TOP, 0, 16, 0, 0));
        if (Build.VERSION.SDK_INT >= 33) {
            source = new BlurredBackgroundSourceRenderNode(null);
            BlurredBackgroundDrawableViewFactory factory = new BlurredBackgroundDrawableViewFactory(new ViewPositionWatcher(this), this, source);
            factory.setLiquidGlassEffectAllowed(true);
            glass = factory.create(folders.getTabsPreviewView(), LiquidGlassStyle.colors(provider, this::effectsEnabled));
            glass.setRadius(AndroidUtilities.dp(18)); glass.setPadding(AndroidUtilities.dp(7));
        }
        refresh();
    }
    void refresh() {
        folders.setGlassPreviewBackground(AppearanceConfig.foldersLiquidGlass() ? glass : null);
        invalidate();
    }
    private boolean effectsEnabled() {
        return Build.VERSION.SDK_INT >= 33 && SharedConfig.chatBlurEnabled() && !LiteMode.isGlassPowerSaverApplied();
    }
    @Override protected void dispatchDraw(Canvas canvas) {
        if (source != null && canvas.isHardwareAccelerated() && getWidth() > 0 && getHeight() > 0 && AppearanceConfig.foldersLiquidGlass()) {
            Canvas recording = source.beginRecording(getWidth(), getHeight());
            drawWallpaperForGlass(recording); source.endRecording();
            source.setBlur(AndroidUtilities.dpf2(LiquidGlassStyle.blurRadiusDp()), LiquidGlassStyle.enhanceColors() ? org.telegram.messenger.utils.RenderNodeEffects.getSaturationX3RenderEffect() : null);
            LiquidGlassStyle.configure(glass);
            ((org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableRenderNode) glass).setLiquidGlassEffectAllowed(effectsEnabled());
            glass.updateColors();
        }
        super.dispatchDraw(canvas);
    }
    @Override protected boolean isActionBarVisible() { return false; }
    @Override protected boolean isStatusBarVisible() { return false; }
    @Override protected boolean useRootView() { return false; }
}
