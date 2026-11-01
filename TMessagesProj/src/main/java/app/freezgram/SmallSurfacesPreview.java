package app.freezgram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.ActionBarMenuSubItem;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;
import org.telegram.ui.Components.chat.ViewPositionWatcher;
import org.telegram.ui.Components.chat.buttons.ChatActivityBlurredRoundPageDownButton;
import org.telegram.ui.Components.glass.GlassTabView;
import org.telegram.ui.Stories.recorder.PreviewView;

/** Native popup rows, side control and the same attachment tabs as ChatAttachAlert. */
final class SmallSurfacesPreview extends SizeNotifierFrameLayout {
    private final Theme.ResourcesProvider provider;
    private final FrameLayout foreground;
    private final BlurredBackgroundDrawableViewFactory factory;
    private BlurredBackgroundSourceRenderNode source;
    private ActionBarPopupWindow.ActionBarPopupWindowLayout menu;
    private Drawable ordinaryMenu;
    private BlurredBackgroundDrawable menuGlass, attachmentGlass;
    SmallSurfacesPreview(Context context, int account, Theme.ResourcesProvider provider) {
        super(context);
        this.provider = provider;
        setBackgroundImage(PreviewView.getBackgroundDrawable(null, account, UserConfig.getInstance(account).getClientUserId(), Theme.isCurrentThemeDark()), false);
        BlurredBackgroundSource wallpaper = new BlurredBackgroundSource() {
            @Override public BlurredBackgroundDrawable createDrawable() { return new BlurredBackgroundDrawableSource(this); }
            @Override public void draw(Canvas canvas, float l, float t, float r, float b) { drawWallpaperForGlass(canvas); }
        };
        if (Build.VERSION.SDK_INT >= 33) source = new BlurredBackgroundSourceRenderNode(wallpaper);
        factory = new BlurredBackgroundDrawableViewFactory(new ViewPositionWatcher(this), this, source != null ? source : wallpaper);
        foreground = new FrameLayout(context);
        addView(foreground, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));
        refresh();
    }
    void refresh() {
        foreground.removeAllViews();
        factory.setLiquidGlassEffectAllowed(SmallElementsGlass.isEnabled());
        menu = new ActionBarPopupWindow.ActionBarPopupWindowLayout(getContext(), provider);
        ordinaryMenu = menu.getBackgroundDrawable();
        foreground.addView(menu, LayoutHelper.createFrame(220, LayoutHelper.WRAP_CONTENT, Gravity.TOP | Gravity.LEFT, 12, 12, 0, 0));
        int[] titles = {R.string.Reply, R.string.Copy, R.string.Forward};
        int[] icons = {R.drawable.msg_reply_small, R.drawable.msg_copy, R.drawable.msg_forward};
        for (int i = 0; i < titles.length; i++) {
            ActionBarMenuSubItem row = new ActionBarMenuSubItem(getContext(), i == 0, i == titles.length - 1, provider);
            row.setTextAndIcon(LocaleController.getString(titles[i]), icons[i]);
            menu.addView(row, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 48));
        }
        menuGlass = factory.create(menu, LiquidGlassStyle.surfaceColors(provider, SmallElementsGlass::isEnabled));
        menuGlass.setRadius(AndroidUtilities.dp(12)); menuGlass.setPadding(AndroidUtilities.dp(8)); menuGlass.setHasPadding(true);
        menu.setBackgroundDrawable(SmallElementsGlass.isEnabled() ? menuGlass : ordinaryMenu);
        menu.setGlassDividers(SmallElementsGlass::isEnabled, Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, provider));
        var button = ChatActivityBlurredRoundPageDownButton.create(getContext(), 46, 24, provider, factory,
                SmallElementsGlass.isEnabled() ? LiquidGlassStyle.surfaceColors(provider, SmallElementsGlass::isEnabled)
                        : org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl.topPanel(provider), R.drawable.pagedown);
        button.setCount(3, false);
        foreground.addView(button, LayoutHelper.createFrame(46, 64, Gravity.TOP | Gravity.RIGHT, 0, 92, 14, 0));
        LinearLayout attachments = new LinearLayout(getContext()) {
            @Override protected void dispatchDraw(Canvas canvas) {
                if (AppearanceConfig.attachmentPanelsGlass() && attachmentGlass != null && canvas.isHardwareAccelerated()) {
                    attachmentGlass.setBounds(0, 0, getWidth(), getHeight());
                    LiquidGlassStyle.configure(attachmentGlass);
                    ((org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableRenderNode) attachmentGlass).setLiquidGlassEffectAllowed(attachmentsEnabled());
                    attachmentGlass.updateColors(); attachmentGlass.draw(canvas);
                } else canvas.drawColor(Theme.getColor(Theme.key_windowBackgroundWhite, provider));
                super.dispatchDraw(canvas);
            }
        };
        attachments.setOrientation(LinearLayout.HORIZONTAL);
        attachments.setPadding(AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8), 0);
        foreground.addView(attachments, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 84, Gravity.BOTTOM, 12, 0, 12, 12));
        if (source != null) {
            attachmentGlass = factory.create(attachments, LiquidGlassStyle.surfaceColors(provider, this::attachmentsEnabled));
            attachmentGlass.setRadius(AndroidUtilities.dp(18));
        }
        int[] labels = {R.string.ChatGallery, R.string.ChatDocument, R.string.ChatLocation};
        GlassTabView.TabAnimation[] animations = {GlassTabView.TabAnimation.GALLERY, GlassTabView.TabAnimation.FILES, GlassTabView.TabAnimation.LOCATION};
        for (int i = 0; i < labels.length; i++) {
            GlassTabView tab = GlassTabView.createAttachTab(getContext(), provider);
            tab.setText(LocaleController.getString(labels[i])); tab.setTabAnimation(animations[i]); tab.setSelected(i == 0, false);
            attachments.addView(tab, new LinearLayout.LayoutParams(0, LayoutHelper.MATCH_PARENT, 1f));
        }
        updateMenuWidth();
        invalidate();
    }
    private boolean attachmentsEnabled() { return AppearanceConfig.attachmentPanelsGlass() && Build.VERSION.SDK_INT >= 33 && SharedConfig.chatBlurEnabled() && !LiteMode.isGlassPowerSaverApplied(); }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateMenuWidth();
    }
    private void updateMenuWidth() {
        if (menu != null && getWidth() > 0) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) menu.getLayoutParams();
            lp.width = Math.min(AndroidUtilities.dp(220), Math.max(AndroidUtilities.dp(120), getWidth() - AndroidUtilities.dp(88)));
            menu.setLayoutParams(lp);
        }
    }
    @Override protected void dispatchDraw(Canvas canvas) {
        if (source != null && canvas.isHardwareAccelerated() && getWidth() > 0 && getHeight() > 0) {
            Canvas recording = source.beginRecording(getWidth(), getHeight());
            drawWallpaperForGlass(recording); source.endRecording();
            source.setBlur(AndroidUtilities.dpf2(LiquidGlassStyle.blurRadiusDp()), LiquidGlassStyle.enhanceColors() ? org.telegram.messenger.utils.RenderNodeEffects.getSaturationX3RenderEffect() : null);
            LiquidGlassStyle.configure(menuGlass); menuGlass.updateColors();
        }
        super.dispatchDraw(canvas);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) { return true; }
    @Override protected boolean isActionBarVisible() { return false; }
    @Override protected boolean isStatusBarVisible() { return false; }
    @Override protected boolean useRootView() { return false; }
}
