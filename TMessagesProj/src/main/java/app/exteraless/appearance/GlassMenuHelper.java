package app.exteraless.appearance;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBarPopupWindow;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.ReactionsContainerLayout;
import org.telegram.ui.Components.ScrimOptions;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProvider;
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProviderBuilder;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceBitmap;
import org.telegram.ui.Components.blur3.utils.Blur3Utils;

/**
 * Матовое стекло под меню сообщения и панелью реакций.
 *
 * <p>Freezgram SmallElementsGlass selects the shared Liquid Glass material when supported.
 * Otherwise {@link AppearanceConfig#glassMessageMenu()} retains the legacy frosted menu
 * and its server-controlled blur policy.
 */
public abstract class GlassMenuHelper {

    private GlassMenuHelper() {
    }

    public static boolean isEnabled(int currentAccount, Theme.ResourcesProvider resourcesProvider) {
        return app.freezgram.SmallElementsGlass.isEnabled()
                || AppearanceConfig.glassMessageMenu() && BlurredBackgroundProviderImpl.checkBlurEnabled(currentAccount, resourcesProvider);
    }

    /**
     * Legacy frosted menu colors; the optional Liquid Glass path below adds a visible surface and contour.
     */
    private static BlurredBackgroundProviderBuilder scrimMenuGlass(Theme.ResourcesProvider resourcesProvider) {
        return new BlurredBackgroundProviderBuilder(resourcesProvider)
            .setBackgroundColor((r, isDark) -> Theme.multAlpha(
                Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground), isDark ? 0.85f : 0.76f))
            .setStrokeColorTop(0, 0)
            .setStrokeColorBottom(0, 0)
            .setStrokeColorFull(0, 0)
            .setStrokeWidth(0.0f, 0.0f);
    }

    public static BlurredBackgroundProvider scrimMenuBackground(Theme.ResourcesProvider resourcesProvider) {
        if (app.freezgram.SmallElementsGlass.isEnabled()) return app.freezgram.LiquidGlassStyle.surfaceColors(resourcesProvider, app.freezgram.SmallElementsGlass::isEnabled);
        return scrimMenuGlass(resourcesProvider)
            .setShadowColor(0x26000000, 0)
            .setShadowLayer(AndroidUtilities.dpf2(4.0f), 0.0f, 0.0f)
            .build();
    }

    public static BlurredBackgroundProvider scrimMenuBackgroundFill(Theme.ResourcesProvider resourcesProvider) {
        if (app.freezgram.SmallElementsGlass.isEnabled()) return app.freezgram.LiquidGlassStyle.surfaceColors(resourcesProvider, app.freezgram.SmallElementsGlass::isEnabled);
        return scrimMenuGlass(resourcesProvider)
            .setShadowColor(0, 0)
            .build();
    }

    public static BlurredBackgroundDrawable createPanelBackground(BlurredBackgroundDrawableViewFactory factory,
                                                                  Theme.ResourcesProvider resourcesProvider,
                                                                  View view) {
        BlurredBackgroundDrawable drawable = app.freezgram.SmallElementsGlass.isEnabled()
                ? factory.createWithSource(app.freezgram.SmallElementsGlass.createSource(factory.getSource()), view, scrimMenuBackground(resourcesProvider), true)
                : factory.create(view, true).setColorProvider(scrimMenuBackground(resourcesProvider));
        if (app.freezgram.SmallElementsGlass.isEnabled()) app.freezgram.LiquidGlassStyle.configure(drawable);
        return drawable
            .setRadius(AndroidUtilities.dp(12.0f))
            .setPadding(AndroidUtilities.dp(8.0f))
            .setHasPadding(true);
    }

    public static BlurredBackgroundDrawable createFill(BlurredBackgroundDrawableViewFactory factory,
                                                       Theme.ResourcesProvider resourcesProvider,
                                                       View view) {
        if (app.freezgram.SmallElementsGlass.isEnabled()) {
            BlurredBackgroundDrawable drawable = factory.createWithSource(app.freezgram.SmallElementsGlass.createSource(factory.getSource()), view, scrimMenuBackgroundFill(resourcesProvider), true);
            app.freezgram.LiquidGlassStyle.configure(drawable); return drawable;
        }
        return factory.create(view, true).setColorProvider(scrimMenuBackgroundFill(resourcesProvider));
    }

    public static void draw(BlurredBackgroundDrawable drawable, Canvas canvas, RectF rect, float radius, int alpha) {
        draw(drawable, canvas, rect, radius, alpha, 0.0f, 0.0f);
    }

    /**
     * Смещение компенсируется сдвигом холста: сам drawable рисуется по целым
     * координатам, поэтому его двигают вместе с канвасом.
     */
    public static void draw(BlurredBackgroundDrawable drawable, Canvas canvas, RectF rect,
                            float radius, int alpha, float offsetX, float offsetY) {
        final boolean translate = offsetX != 0.0f || offsetY != 0.0f;
        if (translate) {
            canvas.save();
            canvas.translate(-offsetX, -offsetY);
        }
        drawable.setRadius(radius);
        drawable.setAlpha(alpha);
        drawable.setBounds(
            Math.round(rect.left + offsetX),
            Math.round(rect.top + offsetY),
            Math.round(rect.right + offsetX),
            Math.round(rect.bottom + offsetY));
        drawable.updateColors();
        drawable.draw(canvas);
        if (translate) {
            canvas.restore();
        }
    }

    public static void applyToGaps(View view, int color) {
        applyToGaps(view, color, false);
    }

    /** Рекурсивно по всем {@code GapView} внутри меню. */
    public static void applyToGaps(View view, int color, boolean dividerVisible) {
        if (view instanceof ActionBarPopupWindow.GapView) {
            final ActionBarPopupWindow.GapView gapView = (ActionBarPopupWindow.GapView) view;
            gapView.setColor(color);
            gapView.setDividerVisible(dividerVisible);
        } else if (view instanceof ViewGroup) {
            final ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                applyToGaps(viewGroup.getChildAt(i), color, dividerVisible);
            }
        }
    }

    public static void applyDividers(ActionBarPopupWindow.ActionBarPopupWindowLayout popup, Theme.ResourcesProvider provider) {
        popup.setGlassDividers(() -> app.freezgram.SmallElementsGlass.isEnabled() && AppearanceConfig.glassMenuDividers(),
                Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, provider));
    }

    /** Apply the same material to the main popup and its swipe-back panels. */
    public static void applyToPopup(BlurredBackgroundDrawableViewFactory factory,
                                    Theme.ResourcesProvider resourcesProvider,
                                    ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout) {
        popupLayout.setBackground(createPanelBackground(factory, resourcesProvider, popupLayout));
        applyDividers(popupLayout, resourcesProvider);
        if (popupLayout.getSwipeBack() != null) {
            popupLayout.getSwipeBack().setGlassForeground(app.freezgram.SmallElementsGlass.isEnabled()
                    ? createFill(factory, resourcesProvider, popupLayout.getSwipeBack()) : null);
        }
    }

    /** Standalone notification menus have no ActionBarMenuItem capture factory. Capture once on open. */
    public static void applyToStandalonePopup(ActionBarPopupWindow.ActionBarPopupWindowLayout popup,
                                             Theme.ResourcesProvider provider, View root) {
        if (!app.freezgram.SmallElementsGlass.isEnabled() || !(root instanceof ViewGroup)) return;
        BlurredBackgroundSourceBitmap bitmap = new BlurredBackgroundSourceBitmap();
        BlurredBackgroundDrawableViewFactory factory = new BlurredBackgroundDrawableViewFactory(bitmap);
        // Attach the position observer to the popup, so it stops when the window closes.
        factory.setSourceRootView(new org.telegram.ui.Components.chat.ViewPositionWatcher(popup), (ViewGroup) root);
        applyToPopup(factory, provider, popup);
        applyToGaps(popup, separatorColor(true, provider));
        captureBlur(bitmap, factory, root);
    }

    public static void applyToReactions(BlurredBackgroundDrawableViewFactory factory,
                                        Theme.ResourcesProvider resourcesProvider,
                                        ReactionsContainerLayout reactionsContainerLayout) {
        if (app.freezgram.SmallElementsGlass.isEnabled()) {
            factory = factory.withSource(app.freezgram.SmallElementsGlass.createSource(factory.getSource()));
            factory.setDrawableConfigurator(app.freezgram.LiquidGlassStyle::configure);
        }
        reactionsContainerLayout.setGlassBackground(factory, scrimMenuBackgroundFill(resourcesProvider));
    }

    /**
     * Переиспользуемое меню переключают между стеклом
     * и обычным popup'ом на лету.
     */
    public static void applyToReusedMenu(BlurredBackgroundDrawableViewFactory factory,
                                         BlurredBackgroundSourceBitmap sourceBitmap,
                                         Theme.ResourcesProvider resourcesProvider,
                                         ActionBarPopupWindow.ActionBarPopupWindowLayout popupLayout,
                                         View rootView,
                                         boolean glass) {
        if (popupLayout == null) {
            return;
        }
        if (glass) {
            applyToPopup(factory, resourcesProvider, popupLayout);
            applyToGaps(popupLayout, separatorColor(true, resourcesProvider));
            captureBlur(sourceBitmap, factory, rootView);
        } else {
            if (popupLayout.getSwipeBack() != null) popupLayout.getSwipeBack().setGlassForeground(null);
            final Drawable background = popupLayout.getResources().getDrawable(R.drawable.popup_fixed_alert4).mutate();
            popupLayout.setBackgroundDrawable(background);
            popupLayout.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefaultSubmenuBackground, resourcesProvider));
            applyToGaps(popupLayout, separatorColor(false, resourcesProvider), true);
        }
    }

    /** Снимок экрана под меню для источника блюра. */
    public static void captureBlur(final BlurredBackgroundSourceBitmap sourceBitmap,
                                   final BlurredBackgroundDrawableViewFactory factory,
                                   final View view) {
        if (app.freezgram.SmallElementsGlass.isEnabled()) {
            AndroidUtilities.makeGlobalBlurBitmap(bitmap -> {
                sourceBitmap.setBitmap(bitmap);
                Blur3Utils.checkBitmapSourceMatrixScale(sourceBitmap, view);
                factory.invalidateAllLinkedViews();
            }, 4f, 0, null, null);
            return;
        }
        ScrimOptions.makeGlobalBlurBitmaps((bitmapBg, bitmapOptions) -> {
            sourceBitmap.setBitmap(bitmapOptions);
            Blur3Utils.checkBitmapSourceMatrixScale(sourceBitmap, view);
            factory.invalidateAllLinkedViews();
        });
    }

    /**
     * Цвет «зазора» между группами пунктов.
     * На стекле сплошной разделитель заменяется еле заметной подложкой цвета текста.
     */
    public static int separatorColor(boolean glass, Theme.ResourcesProvider resourcesProvider) {
        if (!glass) {
            return Theme.getColor(Theme.key_actionBarDefaultSubmenuSeparator, resourcesProvider);
        }
        final boolean isDark = resourcesProvider != null ? resourcesProvider.isDark() : Theme.isCurrentThemeDark();
        return Theme.multAlpha(Theme.getColor(Theme.key_actionBarDefaultSubmenuItem, resourcesProvider),
            isDark ? 0.03f : 0.06f);
    }
}
