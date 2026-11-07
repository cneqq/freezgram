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
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

public final class ChatHeaderSettingsActivity extends BaseFragment {
    private boolean changed;
    private ChatHeaderPreview preview;
    private ChatHeaderPreview profilePreview;
    @Override public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.FreezgramStyleChatHeader));
        StyleSettingsLinks.install(this, "header");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ScrollView scroll = StyleEditorUi.scroll(context, resourceProvider);
        LinearLayout content = StyleEditorUi.content(scroll);
        if (preview != null) preview.release();
        preview = new ChatHeaderPreview(context, this, currentAccount, resourceProvider);
        content.addView(preview, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 120));
        LinearLayout group = StyleEditorUi.section(content, R.string.FreezgramStyleAppearance, resourceProvider);
        TextSettingsCell style = new TextSettingsCell(context, resourceProvider);
        String[] names = {LocaleController.getString(R.string.FreezgramChatComposerDefault), LocaleController.getString(R.string.FreezgramStyleMaterial), "iOS"};
        Runnable refresh = () -> style.setTextAndValue(LocaleController.getString(R.string.FreezgramMessageSurfaceStyle), names[AppearanceConfig.iosChatHeader() ? 2 : AppearanceConfig.newChatHeaderStyle() ? 1 : 0], false);
        refresh.run(); group.addView(style);
        style.setBackground(Theme.getSelectorDrawable(false));
        LinearLayout options = StyleEditorUi.section(content, R.string.FreezgramStyleGlass, resourceProvider);
        options.setVisibility(AppearanceConfig.iosChatHeader() ? View.VISIBLE : View.GONE);
        TextCheckCell dark = new TextCheckCell(context, resourceProvider);
        dark.setTextAndCheck(LocaleController.getString(R.string.FreezgramChatHeaderGlassDim), AppearanceConfig.chatHeaderGlassDim(), false);
        dark.setBackground(Theme.getSelectorDrawable(false));
        dark.setOnClickListener(v -> {
            AppearanceConfig.chatHeaderGlassDim.setConfigBool(!AppearanceConfig.chatHeaderGlassDim());
            dark.setChecked(AppearanceConfig.chatHeaderGlassDim()); changed = true; preview.refresh();
        });
        options.addView(dark);
        style.setOnClickListener(v -> showDialog(new AlertDialog.Builder(context).setTitle(LocaleController.getString(R.string.FreezgramStyleChatHeader)).setItems(names, (dialog, which) -> {
            AppearanceConfig.iosChatHeader.setConfigBool(which == 2);
            AppearanceConfig.newChatHeaderStyle.setConfigBool(which == 1);
            changed = true; refresh.run(); options.setVisibility(which == 2 ? View.VISIBLE : View.GONE); preview.refresh();
        }).create()));
        LinearLayout profileGroup = StyleEditorUi.section(content, R.string.FreezgramProfileHeaderStyle, resourceProvider);
        if (profilePreview != null) profilePreview.release();
        profilePreview = new ChatHeaderPreview(context, this, currentAccount, resourceProvider, true);
        profileGroup.addView(profilePreview, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 104));
        TextSettingsCell profileStyle = new TextSettingsCell(context, resourceProvider);
        String[] profileStyles = {
                LocaleController.getString(R.string.FreezgramProfileHeaderCurrent),
                LocaleController.getString(R.string.FreezgramStyleGlass),
                LocaleController.getString(R.string.FreezgramProfileHeaderRound)
        };
        Runnable refreshProfileStyle = () -> profileStyle.setTextAndValue(
                LocaleController.getString(R.string.FreezgramMessageSurfaceStyle),
                profileStyles[AppearanceConfig.profileHeaderStyle()], false);
        refreshProfileStyle.run();
        profileStyle.setBackground(Theme.getSelectorDrawable(false));
        profileStyle.setOnClickListener(v -> showDialog(new AlertDialog.Builder(context)
                .setTitle(LocaleController.getString(R.string.FreezgramProfileHeaderStyle))
                .setItems(profileStyles, (dialog, which) -> {
                    AppearanceConfig.profileHeaderStyle.setConfigInt(which);
                    changed = true;
                    refreshProfileStyle.run();
                    profilePreview.refresh();
                }).create()));
        profileGroup.addView(profileStyle);

        TextCheckCell actionButtons = new TextCheckCell(context, resourceProvider);
        actionButtons.setTextAndCheck(LocaleController.getString(R.string.FreezgramChatActionButtonsGlass),
                AppearanceConfig.chatActionButtonsGlass(), false);
        actionButtons.setBackground(Theme.getSelectorDrawable(false));
        actionButtons.setOnClickListener(v -> {
            boolean enabled = !AppearanceConfig.chatActionButtonsGlass();
            AppearanceConfig.chatActionButtonsGlass.setConfigBool(enabled);
            actionButtons.setChecked(enabled);
            changed = true;
        });
        group.addView(actionButtons);
        fragmentView = scroll;
        return scroll;
    }
    @Override public void onFragmentDestroy() {
        if (preview != null) preview.release();
        if (profilePreview != null) profilePreview.release();
        android.app.Activity activity = getParentActivity(); super.onFragmentDestroy();
        if (changed && activity instanceof org.telegram.ui.LaunchActivity) AndroidUtilities.runOnUIThread(() -> ((org.telegram.ui.LaunchActivity) activity).rebuildAllFragments(false));
    }
}
