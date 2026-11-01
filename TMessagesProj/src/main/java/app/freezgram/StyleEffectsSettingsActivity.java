package app.freezgram;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextSettingsCell;
import tw.nekomimi.nekogram.NekoConfig;
import xyz.nextalone.nagram.NaConfig;

public final class StyleEffectsSettingsActivity extends BaseFragment {
    public static final int MOTION = 0, SNOW = 1;
    private final int mode;
    private boolean changed;
    public StyleEffectsSettingsActivity(int mode) { this.mode = mode; }
    @Override public View createView(Context context) {
        int title = mode == MOTION ? R.string.FreezgramTransitionStyle : R.string.FreezgramSnowStyle;
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(title));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ScrollView scroll = StyleEditorUi.scroll(context, resourceProvider);
        LinearLayout content = StyleEditorUi.content(scroll);
        LinearLayout group = StyleEditorUi.section(content, 0, resourceProvider);
        if (mode == SNOW) {
            TextCheckCell enabled = new TextCheckCell(context, resourceProvider);
            enabled.setTextAndCheck(LocaleController.getString(R.string.OEAppearanceForceSnow), NekoConfig.actionBarDecoration.Int() == 1, true);
            enabled.setBackground(Theme.getSelectorDrawable(false));
            enabled.setOnClickListener(v -> {
                boolean value = NekoConfig.actionBarDecoration.Int() != 1;
                NekoConfig.actionBarDecoration.setConfigInt(value ? 1 : 0);
                NaConfig.INSTANCE.getChatDecoration().setConfigInt(value ? 1 : 0);
                enabled.setChecked(value); changed = true;
            });
            group.addView(enabled);
        }
        TextSettingsCell style = new TextSettingsCell(context, resourceProvider);
        String[] names = mode == MOTION
                ? new String[]{LocaleController.getString(R.string.FreezgramTransitionStandard), LocaleController.getString(R.string.FreezgramTransitionSmooth)}
                : new String[]{LocaleController.getString(R.string.FreezgramSnowStyleFlakes), LocaleController.getString(R.string.FreezgramSnowStyleDots)};
        Runnable refresh = () -> style.setTextAndValue(LocaleController.getString(title), names[mode == MOTION ? AppearanceConfig.transitionStyle() : AppearanceConfig.snowStyle()], false);
        refresh.run();
        style.setBackground(Theme.getSelectorDrawable(false));
        style.setOnClickListener(v -> showDialog(new AlertDialog.Builder(context).setTitle(LocaleController.getString(title)).setItems(names, (dialog, which) -> {
            if (mode == MOTION) AppearanceConfig.transitionStyle.setConfigInt(which); else AppearanceConfig.snowStyle.setConfigInt(which);
            changed = true; refresh.run();
        }).create()));
        group.addView(style);
        fragmentView = scroll;
        return scroll;
    }
    @Override public void onFragmentDestroy() {
        android.app.Activity activity = getParentActivity(); super.onFragmentDestroy();
        if (changed && activity instanceof org.telegram.ui.LaunchActivity) AndroidUtilities.runOnUIThread(() -> ((org.telegram.ui.LaunchActivity) activity).rebuildAllFragments(false));
    }
}
