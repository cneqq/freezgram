package app.freezgram;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import app.exteraless.appearance.AppearanceConfig;
import app.exteraless.appearance.AvatarCornersSeekBar;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.LayoutHelper;

/** Independent gift settings; received cards and sending previews share this profile. */
public final class GiftGlassSettingsActivity extends BaseFragment {
    private boolean changed;
    @Override public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.FreezgramGiftGlassSettings));
        StyleSettingsLinks.install(this, "gifts");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ScrollView scroll = StyleEditorUi.scroll(context, resourceProvider);
        LinearLayout content = StyleEditorUi.content(scroll);
        GiftCardPreview preview = new GiftCardPreview(context, currentAccount, resourceProvider);
        StyleEditorUi.preview(content, preview, LayoutHelper.WRAP_CONTENT, resourceProvider);
        TextView info = new TextView(context);
        info.setText(LocaleController.getString(R.string.FreezgramStyleGiftInfo));
        info.setTextSize(14); info.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText));
        info.setPadding(dp(22), dp(12), dp(22), dp(12)); content.addView(info);
        LinearLayout settings = StyleEditorUi.section(content, R.string.FreezgramStyleAppearance, resourceProvider);
        TextSettingsCell style = new TextSettingsCell(context, resourceProvider);
        style.setBackground(Theme.getSelectorDrawable(false));
        Runnable updateStyle = () -> style.setTextAndValue(LocaleController.getString(R.string.FreezgramGiftCardStyle), styleNames()[GiftGlassStyle.style()], false);
        updateStyle.run(); settings.addView(style);
        LinearLayout optics = new LinearLayout(context); optics.setOrientation(LinearLayout.VERTICAL);
        optics.setVisibility(GiftGlassStyle.style() == GiftGlassStyle.GLASS ? View.VISIBLE : View.GONE);
        settings.addView(optics);
        LinearLayout ordinary = new LinearLayout(context); ordinary.setOrientation(LinearLayout.VERTICAL);
        ordinary.setVisibility(GiftGlassStyle.style() == GiftGlassStyle.ORDINARY ? View.VISIBLE : View.GONE); settings.addView(ordinary);
        LinearLayout gradient = new LinearLayout(context); gradient.setOrientation(LinearLayout.VERTICAL);
        gradient.setVisibility(GiftGlassStyle.style() == GiftGlassStyle.SOFT ? View.VISIBLE : View.GONE); settings.addView(gradient);
        LinearLayout corners = new LinearLayout(context); corners.setOrientation(LinearLayout.VERTICAL);
        settings.addView(corners);
        slider(context, corners, R.string.FreezgramGiftCorners, 40, AppearanceConfig.giftCardCorners() < 0 ? 20 : AppearanceConfig.giftCardCorners(), value -> {
            AppearanceConfig.giftCardCorners.setConfigInt(value); changed = true; preview.refreshStyle();
        });
        corners.setVisibility(GiftGlassStyle.style() == GiftGlassStyle.GLASS ? View.GONE : View.VISIBLE);
        style.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(LocaleController.getString(R.string.FreezgramGiftCardStyle));
            builder.setItems(styleNames(), (dialog, which) -> {
                AppearanceConfig.giftCardStyle.setConfigInt(which);
                AppearanceConfig.giftCardsGlass.setConfigBool(which != GiftGlassStyle.ORDINARY);
                changed = true; updateStyle.run(); preview.refreshStyle();
                optics.setVisibility(which == GiftGlassStyle.GLASS ? View.VISIBLE : View.GONE);
                ordinary.setVisibility(which == GiftGlassStyle.ORDINARY ? View.VISIBLE : View.GONE);
                gradient.setVisibility(which == GiftGlassStyle.SOFT ? View.VISIBLE : View.GONE);
                corners.setVisibility(which == GiftGlassStyle.GLASS ? View.GONE : View.VISIBLE);
            });
            showDialog(builder.create());
        });
        slider(context, optics, R.string.FreezgramPanelBlur, 30, AppearanceConfig.giftGlassBlur(), value -> {
            AppearanceConfig.giftGlassBlur.setConfigInt(value); changed = true; preview.refreshStyle();
        });
        slider(context, optics, R.string.FreezgramPanelIntensity, 100, AppearanceConfig.giftGlassIntensity(), value -> {
            AppearanceConfig.giftGlassIntensity.setConfigInt(value); changed = true; preview.refreshStyle();
        });
        slider(context, optics, R.string.FreezgramPanelTransparency, 100, AppearanceConfig.giftGlassTransparency(), value -> {
            AppearanceConfig.giftGlassTransparency.setConfigInt(value); changed = true; preview.refreshStyle();
        });
        slider(context, ordinary, R.string.FreezgramGiftOpacity, 100, AppearanceConfig.giftOrdinaryOpacity(), value -> {
            AppearanceConfig.giftOrdinaryOpacity.setConfigInt(value); changed = true; preview.refreshStyle();
        });
        slider(context, gradient, R.string.FreezgramGiftOpacity, 100, AppearanceConfig.giftGradientOpacity(), value -> {
            AppearanceConfig.giftGradientOpacity.setConfigInt(value); changed = true; preview.refreshStyle();
        });
        slider(context, gradient, R.string.FreezgramGiftGradientStrength, 100, AppearanceConfig.giftGradientStrength(), value -> {
            AppearanceConfig.giftGradientStrength.setConfigInt(value); changed = true; preview.refreshStyle();
        });
        slider(context, gradient, R.string.FreezgramGiftGradientAngle, 360, AppearanceConfig.giftGradientAngle(), value -> {
            AppearanceConfig.giftGradientAngle.setConfigInt(value); changed = true; preview.refreshStyle();
        });
        slider(context, gradient, R.string.FreezgramGiftGradientOutline, 100, AppearanceConfig.giftGradientOutline(), value -> {
            AppearanceConfig.giftGradientOutline.setConfigInt(value); changed = true; preview.refreshStyle();
        });
        fragmentView = scroll;
        return scroll;
    }
    private String[] styleNames() {
        return new String[] {LocaleController.getString(R.string.FreezgramGiftCardOrdinary),
                LocaleController.getString(R.string.FreezgramGiftCardGlass), LocaleController.getString(R.string.FreezgramGiftCardSoft)};
    }
    private void slider(Context context, LinearLayout content, int title, int max, int value, AvatarCornersSeekBar.OnDrag drag) {
        AvatarCornersSeekBar slider = new AvatarCornersSeekBar(context, drag, 0, max, LocaleController.getString(title), "0", String.valueOf(max), true);
        if (title == R.string.FreezgramGiftGradientAngle) slider.setValueSuffix("°");
        else if (title == R.string.FreezgramGiftCorners || title == R.string.FreezgramPanelBlur) slider.setValueSuffix("dp");
        else slider.setValueSuffix("%");
        slider.setValue(value); content.addView(slider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 84));
    }
    @Override public void onFragmentDestroy() {
        android.app.Activity activity = getParentActivity();
        super.onFragmentDestroy();
        if (changed && activity instanceof org.telegram.ui.LaunchActivity) {
            AndroidUtilities.runOnUIThread(() -> ((org.telegram.ui.LaunchActivity) activity).rebuildAllFragments(false));
        }
    }
    private static int dp(float n) { return AndroidUtilities.dp(n); }
}
