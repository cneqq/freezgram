package app.freezgram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import app.exteraless.appearance.AppearanceConfig;
import app.exteraless.appearance.AvatarCornersSeekBar;
import app.exteraless.appearance.MainTabsUiHelper;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;
import org.telegram.ui.Components.glass.GlassTabView;

/** Local visual settings with sample background content and the actual navigation tabs. */
public final class NavigationGlassSettingsActivity extends BaseFragment {

    public static final int NAVIGATION = 0, SHARED = 1, COMPOSER = 2;
    private final int mode;
    public NavigationGlassSettingsActivity() { this(NAVIGATION); }
    public NavigationGlassSettingsActivity(int mode) { this.mode = mode; }
    public static boolean previewMaterialRequested(int mode) {
        if (mode == NAVIGATION) return MainTabsUiHelper.isIosNavigationBar() && AppearanceConfig.iosLiquidGlassNavigation();
        if (mode == COMPOSER) return ChatComposerGlass.isRequested();
        return true;
    }
    private GlassPreview preview;
    private AvatarCornersSeekBar blur, intensity, transparency, width, lift, composerLift, composerSide, composerContent;
    private boolean geometryChanged;
    private org.telegram.ui.Cells.TextCheckCell contacts, enhanceColors, wallpaperDim;

    private String text(int id) { return LocaleController.getString(id); }

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(text(mode == SHARED ? R.string.FreezgramStyleGlass : mode == COMPOSER ? R.string.FreezgramComposerSettings : R.string.FreezgramPanelSettings));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ScrollView scroll = StyleEditorUi.scroll(context, resourceProvider);
        LinearLayout content = StyleEditorUi.content(scroll);
        preview = new GlassPreview(context);
        StyleEditorUi.preview(content, preview, mode == SHARED ? 420 : 240, resourceProvider);
        TextView info = new TextView(context);
        info.setText(text(mode == NAVIGATION ? R.string.FreezgramPanelPreviewInfo : mode == SHARED ? R.string.FreezgramStyleGlassInfo : R.string.FreezgramStyleComposerInfo));
        info.setTextSize(14);
        info.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        info.setPadding(dp(20), dp(12), dp(20), dp(12));
        content.addView(info);
        if (mode == 1) {
            LinearLayout optics = StyleEditorUi.section(content, R.string.FreezgramStyleAppearance, resourceProvider);
            LinearLayout switches = StyleEditorUi.section(content, 0, resourceProvider);
            enhanceColors = new org.telegram.ui.Cells.TextCheckCell(context);
            enhanceColors.setTextAndCheck(text(R.string.FreezgramGlassEnhanceColors), LiquidGlassStyle.enhanceColors(), false);
            enhanceColors.setOnClickListener(v -> {
                AppearanceConfig.glassEnhanceColors.setConfigBool(!LiquidGlassStyle.enhanceColors());
                enhanceColors.setChecked(LiquidGlassStyle.enhanceColors()); geometryChanged = true; preview.invalidate();
            });
            switches.addView(enhanceColors);
            wallpaperDim = new org.telegram.ui.Cells.TextCheckCell(context);
            wallpaperDim.setTextAndCheck(text(R.string.FreezgramGlassWallpaperDim), AppearanceConfig.glassWallpaperDim(), false);
            wallpaperDim.setOnClickListener(v -> {
                AppearanceConfig.glassWallpaperDim.setConfigBool(!AppearanceConfig.glassWallpaperDim());
                wallpaperDim.setChecked(AppearanceConfig.glassWallpaperDim()); geometryChanged = true; preview.invalidate();
            });
            switches.addView(wallpaperDim);
            blur = slider(context, optics, R.string.FreezgramPanelBlur, 30,
                    AppearanceConfig.navigationGlassBlur(), value -> {
                        AppearanceConfig.navigationGlassBlur.setConfigInt(value); geometryChanged = true; preview.invalidate();
                    });
            intensity = slider(context, optics, R.string.FreezgramPanelIntensity, 100,
                    AppearanceConfig.navigationGlassIntensity(), value -> {
                        AppearanceConfig.navigationGlassIntensity.setConfigInt(value); geometryChanged = true; preview.invalidate();
                    });
            transparency = slider(context, optics, R.string.FreezgramPanelTransparency, 100,
                    effectiveTransparency(), value -> {
                        AppearanceConfig.navigationGlassTransparency.setConfigInt(value); geometryChanged = true; preview.invalidate();
                    });
        }
        if (mode == NAVIGATION) {
            LinearLayout styleGroup = StyleEditorUi.section(content, R.string.FreezgramStyleAppearance, resourceProvider);
            org.telegram.ui.Cells.TextSettingsCell enabled = new org.telegram.ui.Cells.TextSettingsCell(context, resourceProvider);
            String[] styles = {text(R.string.FreezgramChatComposerDefault), text(R.string.FreezgramStyleMaterial), "iOS"};
            Runnable updateStyle = () -> enabled.setTextAndValue(text(R.string.FreezgramMessageSurfaceStyle),
                    styles[AppearanceConfig.iosNavigationBarStyle() ? 2 : AppearanceConfig.newNavigationBarStyle() ? 1 : 0], false);
            updateStyle.run(); styleGroup.addView(enabled);
            enabled.setBackground(Theme.getSelectorDrawable(false));
            LinearLayout navigationOptions = StyleEditorUi.section(content, R.string.FreezgramPanelSettings, resourceProvider);
            navigationOptions.setVisibility(AppearanceConfig.iosNavigationBarStyle() ? View.VISIBLE : View.GONE);
            enabled.setOnClickListener(v -> showDialog(new org.telegram.ui.ActionBar.AlertDialog.Builder(context)
                    .setTitle(text(R.string.FreezgramPanelSettings)).setItems(styles, (dialog, which) -> {
                        AppearanceConfig.iosNavigationBarStyle.setConfigBool(which == 2);
                        AppearanceConfig.newNavigationBarStyle.setConfigBool(which == 1);
                        updateStyle.run(); navigationOptions.setVisibility(which == 2 ? View.VISIBLE : View.GONE);
                        geometryChanged = true; preview.updateGeometry();
                    }).create()));
            org.telegram.ui.Cells.TextCheckCell glassSwitch = new org.telegram.ui.Cells.TextCheckCell(context);
            glassSwitch.setTextAndCheck("Liquid Glass", AppearanceConfig.iosLiquidGlassNavigation.Bool(), false);
            glassSwitch.setOnClickListener(v -> {
                boolean value = !AppearanceConfig.iosLiquidGlassNavigation.Bool();
                AppearanceConfig.iosLiquidGlassNavigation.setConfigBool(value); glassSwitch.setChecked(value); geometryChanged = true;
                preview.invalidate();
            });
            navigationOptions.addView(glassSwitch);
            width = slider(context, navigationOptions, R.string.FreezgramPanelWidth, 100,
                    AppearanceConfig.navigationPanelWidth(), value -> {
                        AppearanceConfig.navigationPanelWidth.setConfigInt(Math.max(80, value));
                        width.setValue(Math.max(80, value)); geometryChanged = true; preview.updateGeometry();
                    });
            lift = slider(context, navigationOptions, R.string.FreezgramPanelLift, 64,
                    AppearanceConfig.navigationPanelLift(), value -> {
                        AppearanceConfig.navigationPanelLift.setConfigInt(value); geometryChanged = true; preview.updateGeometry();
                    });
            contacts = new org.telegram.ui.Cells.TextCheckCell(context);
            contacts.setTextAndCheck(text(R.string.MainTabsContacts), !xyz.nextalone.nagram.NaConfig.INSTANCE.getMainTabsHideContacts().Bool(), false);
            contacts.setOnClickListener(v -> {
                boolean hidden = !xyz.nextalone.nagram.NaConfig.INSTANCE.getMainTabsHideContacts().Bool();
                xyz.nextalone.nagram.NaConfig.INSTANCE.getMainTabsHideContacts().setConfigBool(hidden);
                contacts.setChecked(!hidden); geometryChanged = true; preview.updateGeometry();
            });
            StyleEditorUi.section(content, 0, resourceProvider).addView(contacts);
        }
        if (mode == COMPOSER) {
            LinearLayout styleGroup = StyleEditorUi.section(content, R.string.FreezgramStyleAppearance, resourceProvider);
            LinearLayout geometry = StyleEditorUi.section(content, R.string.FreezgramComposerSettings, resourceProvider);
            geometry.setVisibility(AppearanceConfig.chatComposerStyle() == 1 ? View.VISIBLE : View.GONE);
            org.telegram.ui.Cells.TextSettingsCell style = new org.telegram.ui.Cells.TextSettingsCell(context);
            String[] styles = {"Android", "iOS"};
            style.setTextAndValue(text(R.string.FreezgramChatComposerStyle), styles[AppearanceConfig.chatComposerStyle() == 1 ? 1 : 0], false);
            style.setOnClickListener(v -> new org.telegram.ui.ActionBar.AlertDialog.Builder(context)
                    .setTitle(text(R.string.FreezgramChatComposerStyle)).setItems(styles, (dialog, which) -> {
                        boolean glass = AppearanceConfig.composerLiquidGlass();
                        AppearanceConfig.chatComposerStyle.setConfigInt(which == 1 ? 1 : 2);
                        AppearanceConfig.composerLiquidGlass.setConfigBool(glass); geometryChanged = true;
                        style.setTextAndValue(text(R.string.FreezgramChatComposerStyle), styles[which], false);
                        geometry.setVisibility(which == 1 ? View.VISIBLE : View.GONE);
                        preview.updateGeometry();
                    }).show());
            styleGroup.addView(style);
            style.setBackground(Theme.getSelectorDrawable(false));
            org.telegram.ui.Cells.TextCheckCell composerGlass = new org.telegram.ui.Cells.TextCheckCell(context, resourceProvider);
            composerGlass.setTextAndCheck("Liquid Glass", AppearanceConfig.composerLiquidGlass(), false);
            composerGlass.setBackground(Theme.getSelectorDrawable(false));
            composerGlass.setOnClickListener(v -> {
                AppearanceConfig.composerLiquidGlass.setConfigBool(!AppearanceConfig.composerLiquidGlass());
                composerGlass.setChecked(AppearanceConfig.composerLiquidGlass()); geometryChanged = true; preview.invalidate();
            });
            styleGroup.addView(composerGlass);
            composerContent = slider(context, geometry, R.string.FreezgramComposerContentInset, 32,
                    AppearanceConfig.composerContentInset(), value -> {
                        AppearanceConfig.composerContentInset.setConfigInt(value); geometryChanged = true; preview.invalidate();
                    });
            composerSide = slider(context, geometry, R.string.FreezgramComposerSideInset, 48,
                    AppearanceConfig.composerSideInset(), value -> {
                        AppearanceConfig.composerSideInset.setConfigInt(value); geometryChanged = true; preview.invalidate();
                    });
            composerLift = slider(context, geometry, R.string.FreezgramPanelLift, 64,
                    AppearanceConfig.composerBottomLift(), value -> {
                        AppearanceConfig.composerBottomLift.setConfigInt(value); geometryChanged = true; preview.invalidate();
                    });
        }
        StyleEditorUi.reset(content, resourceProvider, () -> {
            if (mode == 1) {
                AppearanceConfig.glassWallpaperDim.setConfigBool(true);
                wallpaperDim.setChecked(true);
                AppearanceConfig.glassEnhanceColors.setConfigBool(true);
                enhanceColors.setChecked(true);
                AppearanceConfig.navigationGlassBlur.setConfigInt(6);
                AppearanceConfig.navigationGlassIntensity.setConfigInt(55);
                AppearanceConfig.navigationGlassTransparency.setConfigInt(-1);
                blur.setValue(6); intensity.setValue(55); transparency.setValue(effectiveTransparency());
            } else if (mode == 0) {
                AppearanceConfig.navigationPanelWidth.setConfigInt(100);
                AppearanceConfig.navigationPanelLift.setConfigInt(12);
                width.setValue(100); lift.setValue(12); preview.updateGeometry();
            } else {
                AppearanceConfig.composerBottomLift.setConfigInt(0);
                AppearanceConfig.composerSideInset.setConfigInt(0);
                AppearanceConfig.composerContentInset.setConfigInt(16);
                composerContent.setValue(16); composerLift.setValue(0); composerSide.setValue(0);
            }
            geometryChanged = true;
            preview.invalidate();
        });
        fragmentView = scroll;
        return fragmentView;
    }

    @Override public void onFragmentDestroy() {
        android.app.Activity activity = getParentActivity();
        super.onFragmentDestroy();
        if (geometryChanged && activity instanceof org.telegram.ui.LaunchActivity) {
            AndroidUtilities.runOnUIThread(() -> ((org.telegram.ui.LaunchActivity) activity).rebuildAllFragments(false));
        }
    }

    private int effectiveTransparency() {
        return Math.round(100 * (1 - IosLiquidGlassNavigation.overlayAlpha(Theme.isCurrentThemeDark())));
    }

    private AvatarCornersSeekBar slider(Context context, LinearLayout parent, int title, int max,
                                       int value, AvatarCornersSeekBar.OnDrag onDrag) {
        AvatarCornersSeekBar slider = new AvatarCornersSeekBar(context, onDrag, title == R.string.FreezgramPanelWidth ? 80 : 0, max,
                text(title), title == R.string.FreezgramPanelWidth ? "80" : "0", String.valueOf(max), true);
        slider.setValueSuffix(max == 100 ? "%" : "dp");
        slider.setValue(value);
        parent.addView(slider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 84));
        return slider;
    }

    private static int dp(float value) { return AndroidUtilities.dp(value); }

    private final class GlassPreview extends FrameLayout {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF panel = new RectF();
        private BlurredBackgroundSourceRenderNode source;
        private BlurredBackgroundDrawable glass, headerGlass, fieldGlass, leftGlass, rightGlass, foldersGlass;
        private app.exteraless.appearance.FoldersPreviewCell foldersPreview;
        private Boolean foldersPreviewEnabled;
        private final RectF foldersPosition = new RectF();
        private final android.graphics.Rect field = new android.graphics.Rect(), leftButton = new android.graphics.Rect(), rightButton = new android.graphics.Rect();
        private final android.graphics.drawable.Drawable attachIcon, emojiIcon, micIcon;

        private org.telegram.ui.MainTabsLayout tabs;
        private GlassTabView contactTab;

        GlassPreview(Context context) {
            super(context);
            setWillNotDraw(false);
            attachIcon = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.msg_input_attach2).mutate();
            emojiIcon = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.input_smile).mutate();
            micIcon = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.input_mic).mutate();
            tabs = new org.telegram.ui.MainTabsLayout(context, resourceProvider);
            tabs.setPadding(dp(12), dp(14), dp(12), dp(14));
            GlassTabView[] buttons = {
                    GlassTabView.createMainTab(context, resourceProvider, GlassTabView.TabAnimation.CHATS, R.string.MainTabsChats),
                    GlassTabView.createMainTab(context, resourceProvider, GlassTabView.TabAnimation.SETTINGS, R.string.Settings),
                    GlassTabView.createAvatar(context, resourceProvider, currentAccount, R.string.MainTabsProfile)
            };
            for (int i = 0; i < buttons.length; i++) {
                GlassTabView button = buttons[i];
                button.setSelected(i == 0, false);
                button.setOnClickListener(v -> {
                    for (GlassTabView tab : buttons) tab.setSelected(tab == v, true);
                });
                tabs.addView(button);
                tabs.addTabToIgnoreClick(button); tabs.setViewVisible(button, true, false);
            }
            contactTab = GlassTabView.createMainTab(context, resourceProvider, GlassTabView.TabAnimation.CONTACTS, R.string.MainTabsContacts);
            contactTab.setOnClickListener(v -> {
                for (GlassTabView tab : buttons) tab.setSelected(false, true);
                contactTab.setSelected(true, true);
            });
            tabs.addView(contactTab, 1); tabs.addTabToIgnoreClick(contactTab);
            for (GlassTabView button : buttons) button.setOnClickListener(v -> {
                contactTab.setSelected(false, true);
                for (GlassTabView tab : buttons) tab.setSelected(tab == v, true);
            });
            FrameLayout.LayoutParams tabsLayout = new FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, dp(76));
            tabsLayout.setMargins(dp(12), dp(148), dp(12), 0);
            addView(tabs, tabsLayout);
            updateGeometry();
            if (mode == SHARED) {
                foldersPreview = new app.exteraless.appearance.FoldersPreviewCell(context, resourceProvider);
                FrameLayout.LayoutParams folderParams = new FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, dp(86));
                folderParams.topMargin = dp(68); addView(foldersPreview, folderParams);
            }
            // The actual AGSL renderer; unsupported phones show the same opaque fallback.
            if (Build.VERSION.SDK_INT >= 33) {
                source = new BlurredBackgroundSourceRenderNode(null);
                glass = createGlass(30, 0);
                headerGlass = createGlass(23, 0);
                headerGlass.setColorProvider(LiquidGlassStyle.headerColors(resourceProvider, this::effectsEnabled));
                fieldGlass = createGlass(24, 7);
                leftGlass = createGlass(24, 7);
                rightGlass = createGlass(24, 7);
                foldersGlass = createGlass(18, 7);
            }
        }

        void updateGeometry() {
            float side = dp(12) + Math.max(0, getWidth() - dp(24)) * (100 - AppearanceConfig.navigationPanelWidth()) / 200f;
            float top = dp((mode == 1 ? 356 : 160) - AppearanceConfig.navigationPanelLift());
            panel.set(side, top, getWidth() - side, top + dp(60));
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) tabs.getLayoutParams();
            lp.setMargins(Math.round(side) - dp(8), Math.round(top) - dp(8), Math.round(side) - dp(8), 0);
            tabs.setLayoutParams(lp);
            tabs.setVisibility(mode == 2 ? GONE : VISIBLE);
            tabs.setViewVisible(contactTab, !xyz.nextalone.nagram.NaConfig.INSTANCE.getMainTabsHideContacts().Bool(), false);
            invalidate();
        }

        @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh); updateGeometry();
        }

        @Override protected boolean verifyDrawable(android.graphics.drawable.Drawable who) {
            return who == glass || who == headerGlass || who == fieldGlass || who == leftGlass || who == rightGlass || who == foldersGlass || super.verifyDrawable(who);
        }

        private void background(Canvas canvas) {
            canvas.drawColor(getThemedColor(Theme.key_windowBackgroundWhite));
            for (int i = 0; i < (mode == 1 ? 7 : 4); i++) {
                float y = dp(32 + i * 55);
                paint.setColor(new int[]{0xff459de3, 0xff8871d7, 0xff50b7a1, 0xffd58b56}[i % 4]);
                canvas.drawCircle(dp(37), y, dp(21), paint);
                paint.setColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText));
                paint.setTextSize(dp(15));
                canvas.drawText(i % 2 == 0 ? "Freezgram" : text(R.string.MainTabsChats), dp(72), y, paint);
                paint.setAlpha(100);
                canvas.drawRoundRect(dp(72), y + dp(8), getWidth() - dp(28), y + dp(12), dp(2), dp(2), paint);
                paint.setAlpha(255);
            }
        }

        private boolean effectsEnabled() {
            return previewMaterialRequested(mode) && Build.VERSION.SDK_INT >= 33 && org.telegram.messenger.SharedConfig.chatBlurEnabled()
                    && !org.telegram.messenger.LiteMode.isGlassPowerSaverApplied();
        }

        private BlurredBackgroundDrawable createGlass(int radius, int padding) {
            BlurredBackgroundDrawable drawable = source.createDrawable();
            drawable.setColorProvider(LiquidGlassStyle.colors(resourceProvider, this::effectsEnabled));
            drawable.setRadius(dp(radius)); drawable.setPadding(dp(padding)); drawable.setCallback(this);
            return drawable;
        }

        private void surface(Canvas canvas, BlurredBackgroundDrawable drawable, android.graphics.Rect bounds, int radius, int padding) {
            if (previewMaterialRequested(mode) && drawable != null && canvas.isHardwareAccelerated()) {
                drawable.setBounds(bounds);
                LiquidGlassStyle.configure(drawable);
                var node = (org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableRenderNode) drawable;
                node.setLiquidGlassEffectAllowed(effectsEnabled());
                drawable.updateColors(); node.invalidateDisplayList(); drawable.draw(canvas);
            } else {
                paint.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
                canvas.drawRoundRect(bounds.left + dp(padding), bounds.top + dp(padding), bounds.right - dp(padding), bounds.bottom - dp(padding), dp(radius), dp(radius), paint);
            }
        }

        private void icon(Canvas canvas, android.graphics.drawable.Drawable drawable, float x, float y) {
            drawable.setTint(getThemedColor(Theme.key_glass_defaultIcon));
            drawable.setBounds(Math.round(x - dp(12)), Math.round(y - dp(12)), Math.round(x + dp(12)), Math.round(y + dp(12)));
            drawable.draw(canvas);
        }

        @Override protected void onDraw(Canvas canvas) {
            background(canvas);
            if (source != null && canvas.isHardwareAccelerated()) {
                Canvas capture = source.beginRecording(getWidth(), getHeight());
                background(capture); source.endRecording();
                source.setBlur(AndroidUtilities.dpf2(LiquidGlassStyle.blurRadiusDp()), LiquidGlassStyle.enhanceColors()
                        ? org.telegram.messenger.utils.RenderNodeEffects.getSaturationX3RenderEffect() : null);
            }
            if (foldersPreview != null) {
                boolean enabled = AppearanceConfig.foldersLiquidGlass();
                if (foldersPreviewEnabled == null || foldersPreviewEnabled != enabled) {
                    foldersPreviewEnabled = enabled;
                    foldersPreview.setGlassPreviewBackground(enabled ? foldersGlass : null);
                }
                if (enabled && foldersGlass != null) {
                    if (org.telegram.ui.Components.chat.ViewPositionWatcher.computeRectInParent(foldersPreview.getTabsPreviewView(), this, foldersPosition)) {
                        foldersGlass.setSourceOffset(foldersPosition.left, foldersPosition.top);
                    }
                    LiquidGlassStyle.configure(foldersGlass);
                    ((org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableRenderNode) foldersGlass).setLiquidGlassEffectAllowed(effectsEnabled());
                    foldersGlass.updateColors();
                }
            }
            if (mode != 2) surface(canvas, glass, new android.graphics.Rect((int) panel.left, (int) panel.top, (int) panel.right, (int) panel.bottom), 30, 0);
            if (mode != 0) {
                if (mode == 1) {
                    surface(canvas, headerGlass, new android.graphics.Rect(dp(12), dp(12), getWidth()-dp(12), dp(60)), 23, 0);
                    paint.setColor(getThemedColor(Theme.key_windowBackgroundWhiteBlackText)); paint.setTextSize(dp(16));
                    canvas.drawText("Freezgram", dp(32), dp(43), paint);
                }
                int top = dp((mode == SHARED ? 220 : 150) - (AppearanceConfig.chatComposerStyle() == 1 ? AppearanceConfig.composerBottomLift() : 0));
                int side = AppearanceConfig.chatComposerStyle() == 1
                        ? Math.min(dp(AppearanceConfig.composerSideInset()), Math.max(0, (getWidth() - dp(240)) / 2)) : 0;
                android.graphics.Rect whole = new android.graphics.Rect(side, top, getWidth() - side, top + dp(58));
                boolean split = AppearanceConfig.chatComposerStyle() == 1;
                ChatComposerGlass.surfaceBounds(whole, field, leftButton, rightButton, split);
                surface(canvas, fieldGlass, field, 24, 7);
                if (split) {
                    surface(canvas, leftGlass, leftButton, 24, 7);
                    surface(canvas, rightGlass, rightButton, 24, 7);
                }
                float y = whole.bottom - dp(29);
                icon(canvas, attachIcon, whole.left + dp(split ? 29 : 26), y);
                icon(canvas, micIcon, whole.right - dp(29), y);
                float emojiX = split ? whole.right - dp(66 + ChatComposerGlass.emojiRightInsetDp()) : whole.left + dp(70);
                icon(canvas, emojiIcon, emojiX, y);
                float textX = whole.left + dp(split ? ChatComposerGlass.textLeftInsetDp() : 94);
                paint.setColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText)); paint.setTextSize(dp(16));
                canvas.save();
                canvas.clipRect(textX, top, split ? emojiX - dp(22) : whole.right - dp(55), whole.bottom);
                canvas.drawText(text(R.string.TypeMessage), textX, y - (paint.ascent() + paint.descent()) / 2, paint);
                canvas.restore();
            }
        }
    }
}
