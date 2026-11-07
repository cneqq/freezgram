package app.freezgram;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.view.Gravity;
import android.view.MotionEvent;
import app.exteraless.appearance.AppearanceConfig;
import app.exteraless.appearance.ChatHeaderUiHelper;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ChatAvatarContainer;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableSource;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;
import org.telegram.ui.Components.chat.ViewPositionWatcher;
import org.telegram.ui.Stories.recorder.PreviewView;

/** The real ActionBar and ChatAvatarContainer over the current wallpaper. No chat actions. */
final class ChatHeaderPreview extends SizeNotifierFrameLayout {
    private final BaseFragment owner;
    private final Theme.ResourcesProvider provider;
    private final int account;
    private final boolean profile;
    private ChatAvatarContainer avatar;
    private ActionBar bar;
    private BlurredBackgroundSourceRenderNode source;
    private final BlurredBackgroundDrawableViewFactory factory;
    ChatHeaderPreview(Context context, BaseFragment owner, int account, Theme.ResourcesProvider provider) {
        this(context, owner, account, provider, false);
    }

    ChatHeaderPreview(Context context, BaseFragment owner, int account, Theme.ResourcesProvider provider, boolean profile) {
        super(context);
        this.owner = owner; this.account = account; this.provider = provider; this.profile = profile;
        setBackgroundImage(PreviewView.getBackgroundDrawable(null, account, UserConfig.getInstance(account).getClientUserId(), Theme.isCurrentThemeDark()), false);
        BlurredBackgroundSource wallpaper = new BlurredBackgroundSource() {
            @Override public BlurredBackgroundDrawable createDrawable() { return new BlurredBackgroundDrawableSource(this); }
            @Override public void draw(Canvas canvas, float l, float t, float r, float b) { drawWallpaperForGlass(canvas); }
        };
        if (Build.VERSION.SDK_INT >= 31) source = new BlurredBackgroundSourceRenderNode(wallpaper);
        factory = new BlurredBackgroundDrawableViewFactory(new ViewPositionWatcher(this), this, source != null ? source : wallpaper);
        refresh();
    }
    void refresh() {
        if (avatar != null) avatar.onDestroy();
        avatar = null;
        // BackgroundView belongs to SizeNotifierFrameLayout and must survive style changes.
        if (bar != null) removeView(bar);
        bar = new ActionBar(getContext(), provider);
        bar.setOccupyStatusBar(false);
        if (profile) {
            setupProfile();
            return;
        }
        bar.setBackButtonImage(R.drawable.ic_ab_back);
        bar.setItemsColor(Theme.getColor(Theme.key_chat_topPanelTitle, provider), false);
        avatar = new ChatAvatarContainer(getContext(), owner, false, provider) {
            @Override protected boolean isCentered() { return AppearanceConfig.iosChatHeader(); }
        };
        avatar.setOccupyStatusBar(false);
        ChatHeaderUiHelper.setupGlassAvatarContainer(avatar);
        var user = UserConfig.getInstance(account).getCurrentUser();
        avatar.setTitle(user == null ? "Freezgram" : UserObject.getUserName(user));
        avatar.setSubtitle(LocaleController.getString(R.string.Online));
        if (user != null) avatar.setUserAvatar(user, true);
        bar.addView(avatar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, Gravity.TOP | Gravity.LEFT, 52, 0, AppearanceConfig.iosChatHeader() ? 0 : 52, 0));
        factory.setLiquidGlassEffectAllowed(IosLiquidGlassChatHeader.isEnabled());
        bar.setupGlass(factory, AppearanceConfig.iosChatHeader() ? IosLiquidGlassChatHeader.colors(provider) : BlurredBackgroundProviderImpl.topPanelChatActivity(provider));
        bar.setIosChatHeaderGlass(AppearanceConfig.iosChatHeader());
        ChatHeaderUiHelper.applyChatHeaderGlassStyle(bar);
        if (AppearanceConfig.iosChatHeader()) {
            bar.setChatAvatarContainer(avatar); avatar.setActionBar(bar); bar.setForcedMenuWidth(AndroidUtilities.dp(46));
        } else {
            bar.createMenu().addItem(1, R.drawable.call);
            bar.createMenu().addItem(0, R.drawable.ic_ab_other);
        }
        addView(bar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 20, 0, 0));
        invalidate();
    }

    private void setupProfile() {
        int style = ProfileHeaderAppearance.style();
        boolean frosted = style == ProfileHeaderAppearance.FROSTED;
        if (frosted) bar.centerTitle();
        var user = UserConfig.getInstance(account).getCurrentUser();
        bar.setTitle(user == null ? "FreezGram" : UserObject.getUserName(user));
        bar.setSubtitle(LocaleController.getString(R.string.MyProfile));
        int titleColor = Theme.getColor(Theme.key_profile_title, provider);
        bar.setTitleColor(titleColor);
        bar.setSubtitleColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, provider));
        bar.setItemsColor(titleColor, false);
        if (style != ProfileHeaderAppearance.CLASSIC) {
            var backgroundFactory = frosted ? ProfileHeaderAppearance.frostedFactory(factory) : factory;
            bar.setBackground(ProfileHeaderAppearance.surface(backgroundFactory, bar, provider, style, false));
            if (frosted) {
                bar.setBackButtonImage(R.drawable.ic_ab_back);
                bar.getBackButton().setBackground(ProfileHeaderAppearance.control(backgroundFactory, bar.getBackButton(), provider));
            }
        } else {
            bar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite, provider));
        }
        if (style == ProfileHeaderAppearance.CLASSIC) {
            bar.setBackButtonImage(R.drawable.outline_header_qr_24);
        }
        var menu = bar.createMenu().addItem(0, frosted ? R.drawable.msg_list
                : style == ProfileHeaderAppearance.GLASS ? R.drawable.msg_settings : R.drawable.ic_ab_other);
        if (style != ProfileHeaderAppearance.CLASSIC) {
            menu.setBackground(ProfileHeaderAppearance.control(
                    frosted ? ProfileHeaderAppearance.frostedFactory(factory) : factory, menu, provider));
        }
        addView(bar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 20, 0, 0));
        invalidate();
    }
    void release() { if (avatar != null) avatar.onDestroy(); }
    @Override protected void dispatchDraw(Canvas canvas) {
        if (source != null && canvas.isHardwareAccelerated() && getWidth() > 0 && getHeight() > 0) {
            Canvas recording = source.beginRecording(getWidth(), getHeight());
            drawWallpaperForGlass(recording); source.endRecording();
            source.setBlur(AndroidUtilities.dpf2(LiquidGlassStyle.blurRadiusDp()), LiquidGlassStyle.enhanceColors() ? org.telegram.messenger.utils.RenderNodeEffects.getSaturationX3RenderEffect() : null);
        }
        super.dispatchDraw(canvas);
    }
    @Override public boolean dispatchTouchEvent(MotionEvent event) { return true; }
    @Override protected boolean isActionBarVisible() { return false; }
    @Override protected boolean isStatusBarVisible() { return false; }
    @Override protected boolean useRootView() { return false; }
}
