package app.freezgram;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.function.IntConsumer;
import app.exteraless.appearance.AppearanceConfig;
import app.exteraless.appearance.AvatarCornersSeekBar;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

public final class MessageSurfaceSettingsActivity extends BaseFragment {
    private boolean changed;
    private MessageSurfacePreview preview;
    private String text(int id) { return LocaleController.getString(id); }
    @Override public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back); actionBar.setTitle(text(R.string.FreezgramMessageSurfaces));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ScrollView scroll = StyleEditorUi.scroll(context, resourceProvider);
        LinearLayout content = StyleEditorUi.content(scroll);
        preview = new MessageSurfacePreview(context, currentAccount, resourceProvider);
        StyleEditorUi.preview(content, preview, LayoutHelper.WRAP_CONTENT, resourceProvider);
        TextCheckCell same = new TextCheckCell(context);
        same.setTextAndCheck(text(R.string.FreezgramSameMessageSurfaces), AppearanceConfig.sameMessageSurfaces(), false);
        StyleEditorUi.section(content, 0, resourceProvider).addView(same);
        same.setBackground(Theme.getSelectorDrawable(false));
        LinearLayout groups = new LinearLayout(context); groups.setOrientation(LinearLayout.VERTICAL); content.addView(groups);
        rebuildGroups(context, groups);
        same.setOnClickListener(v -> {
            AppearanceConfig.sameMessageSurfaces.setConfigBool(!AppearanceConfig.sameMessageSurfaces());
            same.setChecked(AppearanceConfig.sameMessageSurfaces()); changed = true;
            rebuildGroups(context, groups); preview.refresh();
        });
        TextView info = new TextView(context); info.setText(text(R.string.FreezgramStyleMessageInfo)); info.setTextSize(14);
        info.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText)); info.setPadding(dp(20), dp(12), dp(20), dp(20)); content.addView(info);
        fragmentView = scroll; return scroll;
    }
    private void rebuildGroups(Context context, LinearLayout content) {
        content.removeAllViews();
        section(context, content, AppearanceConfig.sameMessageSurfaces() ? R.string.FreezgramAllMessagesSurface : R.string.FreezgramIncomingSurface, 0,
                value -> AppearanceConfig.incomingSurfaceStyle.setConfigInt(value), value -> AppearanceConfig.incomingSurfaceOpacity.setConfigInt(value),
                value -> AppearanceConfig.incomingSurfaceBlur.setConfigInt(value), value -> AppearanceConfig.incomingSurfaceIntensity.setConfigInt(value));
        if (!AppearanceConfig.sameMessageSurfaces()) section(context, content, R.string.FreezgramOutgoingSurface, 1,
                value -> AppearanceConfig.outgoingSurfaceStyle.setConfigInt(value), value -> AppearanceConfig.outgoingSurfaceOpacity.setConfigInt(value),
                value -> AppearanceConfig.outgoingSurfaceBlur.setConfigInt(value), value -> AppearanceConfig.outgoingSurfaceIntensity.setConfigInt(value));
    }
    private void section(Context context, LinearLayout content, int title, int profile, IntConsumer setStyle, IntConsumer setOpacity, IntConsumer setBlur, IntConsumer setIntensity) {
        LinearLayout group = StyleEditorUi.section(content, title, resourceProvider);
        String[] names = {text(R.string.FreezgramGiftCardOrdinary), text(R.string.FreezgramGiftCardGlass)};
        TextSettingsCell selector = new TextSettingsCell(context);
        Runnable refresh = () -> selector.setTextAndValue(text(R.string.FreezgramMessageSurfaceStyle), names[MessageSurfaceStyle.style(profile)], false);
        refresh.run(); group.addView(selector);
        selector.setBackground(Theme.getSelectorDrawable(false));
        LinearLayout controls = new LinearLayout(context); controls.setOrientation(LinearLayout.VERTICAL); group.addView(controls);
        LinearLayout optics = new LinearLayout(context); optics.setOrientation(LinearLayout.VERTICAL);
        slider(context, controls, R.string.FreezgramGiftOpacity, 100, MessageSurfaceStyle.opacity(profile), setOpacity);
        controls.addView(optics);
        slider(context, optics, R.string.FreezgramPanelBlur, 30, MessageSurfaceStyle.blur(profile), setBlur);
        slider(context, optics, R.string.FreezgramPanelIntensity, 100, MessageSurfaceStyle.intensity(profile), setIntensity);
        Runnable visibility = () -> { controls.setVisibility(MessageSurfaceStyle.style(profile) == 0 ? View.GONE : View.VISIBLE); optics.setVisibility(MessageSurfaceStyle.style(profile) == 1 ? View.VISIBLE : View.GONE); };
        visibility.run();
        selector.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context); builder.setTitle(text(title));
            builder.setItems(names, (dialog, which) -> { setStyle.accept(which); changed = true; refresh.run(); visibility.run(); preview.refresh(); }); showDialog(builder.create());
        });
    }
    private void slider(Context context, LinearLayout content, int title, int max, int value, IntConsumer setter) {
        AvatarCornersSeekBar slider = new AvatarCornersSeekBar(context, v -> { setter.accept(v); changed = true; preview.refresh(); }, 0, max, text(title), "0", String.valueOf(max), true);
        slider.setValueSuffix(max == 30 ? "dp" : "%");
        slider.setValue(value); content.addView(slider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 84));
    }
    @Override public void onFragmentDestroy() {
        android.app.Activity activity = getParentActivity(); super.onFragmentDestroy();
        if (changed && activity instanceof org.telegram.ui.LaunchActivity) AndroidUtilities.runOnUIThread(() -> ((org.telegram.ui.LaunchActivity) activity).rebuildAllFragments(false));
    }
    private static int dp(float n) { return AndroidUtilities.dp(n); }
}
