package org.telegram.ui.bots;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.View;

import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;

class BotWebViewActionBar extends ActionBar {

    private View optionsButton;

    BotWebViewActionBar(Context context, Theme.ResourcesProvider resourcesProvider) {
        super(context, resourcesProvider);
        if (usesIosStyle()) {
            centerTitle();
        }
    }

    static boolean usesIosStyle() {
        return app.exteraless.appearance.AppearanceConfig.iosMode();
    }

    void setOptionsButton(View button) {
        optionsButton = button;
        updateIosButtons();
    }

    @Override
    public void setItemsBackgroundColor(int color, boolean isActionMode) {
        super.setItemsBackgroundColor(color, isActionMode);
        if (!isActionMode) {
            updateIosButtons();
        }
    }

    private void updateIosButtons() {
        if (!usesIosStyle()) {
            return;
        }
        if (backButtonImageView != null) {
            backButtonImageView.setBackground(createControlBackground(dp(6)));
        }
        if (optionsButton != null) {
            optionsButton.setBackground(createControlBackground(dp(4)));
        }
    }

    private Drawable createControlBackground(int horizontalInset) {
        final int color = getItemsColor();
        final GradientDrawable surface = new GradientDrawable();
        surface.setColor(Theme.multAlpha(color, .12f));
        surface.setCornerRadius(dp(24));
        surface.setStroke(dp(1), Theme.multAlpha(color, .14f));

        final RippleDrawable ripple = new RippleDrawable(
                ColorStateList.valueOf(Theme.multAlpha(color, .18f)), surface,
                Theme.createRoundRectDrawable(dp(24), 0xffffffff));
        return new InsetDrawable(ripple, horizontalInset, dp(7), horizontalInset, dp(7));
    }
}
