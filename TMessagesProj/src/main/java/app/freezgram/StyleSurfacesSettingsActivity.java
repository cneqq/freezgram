package app.freezgram;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;

/** Surface opt-ins are separate from the shared material's optical parameters. */
public final class StyleSurfacesSettingsActivity extends BaseFragment {
    public static final int CHAT_LIST = 0, CHAT = 1;
    private final int mode;
    private boolean changed;
    public StyleSurfacesSettingsActivity(int mode) { this.mode = mode; }
    @Override public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(mode == CHAT_LIST ? R.string.FreezgramStyleChatList : R.string.FreezgramStyleMenus));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ScrollView scroll = StyleEditorUi.scroll(context, resourceProvider);
        LinearLayout content = StyleEditorUi.content(scroll);
        if (mode == CHAT_LIST) {
            // Keep the actual FilterTabsView preview rather than drawing mock tabs.
            FoldersGlassPreview folders = new FoldersGlassPreview(context, currentAccount, resourceProvider);
            StyleEditorUi.preview(content, folders, 120, resourceProvider, false);
            LinearLayout group = StyleEditorUi.section(content, R.string.FreezgramStyleGlass, resourceProvider);
            toggle(group, R.string.FreezgramDialogsHeaderGlass, AppearanceConfig::dialogsHeaderGlass,
                    value -> AppearanceConfig.dialogsHeaderGlass.setConfigBool(value));
            toggle(group, R.string.FreezgramFoldersGlass, AppearanceConfig::foldersLiquidGlass,
                    value -> { AppearanceConfig.foldersLiquidGlass.setConfigBool(value); folders.refresh(); });
        } else {
            LinearLayout group = StyleEditorUi.section(content, R.string.FreezgramStyleGlass, resourceProvider);
            toggle(group, R.string.FreezgramSmallElementsGlass, AppearanceConfig::smallElementsGlass,
                    value -> AppearanceConfig.smallElementsGlass.setConfigBool(value));
            toggle(group, R.string.FreezgramAttachmentPanelsGlass, AppearanceConfig::attachmentPanelsGlass,
                    value -> AppearanceConfig.attachmentPanelsGlass.setConfigBool(value));
        }
        fragmentView = scroll;
        return scroll;
    }
    private void toggle(LinearLayout group, int label, BooleanSupplier getter, Consumer<Boolean> setter) {
        TextCheckCell row = new TextCheckCell(group.getContext(), resourceProvider);
        row.setTextAndCheck(LocaleController.getString(label), getter.getAsBoolean(), true);
        row.setBackground(Theme.getSelectorDrawable(false));
        row.setOnClickListener(v -> { setter.accept(!getter.getAsBoolean()); row.setChecked(getter.getAsBoolean()); changed = true; });
        group.addView(row);
    }
    @Override public void onFragmentDestroy() {
        android.app.Activity activity = getParentActivity();
        super.onFragmentDestroy();
        if (changed && activity instanceof org.telegram.ui.LaunchActivity) AndroidUtilities.runOnUIThread(() -> ((org.telegram.ui.LaunchActivity) activity).rebuildAllFragments(false));
    }
}
