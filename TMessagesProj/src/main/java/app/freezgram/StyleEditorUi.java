package app.freezgram;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Components.LayoutHelper;

/** Common spacing and native rows for Freezgram's editors. No preference ownership. */
final class StyleEditorUi {
    private StyleEditorUi() {}
    static ScrollView scroll(Context context, Theme.ResourcesProvider provider) {
        ScrollView view = new ScrollView(context);
        view.setFillViewport(true);
        view.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray, provider));
        return view;
    }
    static LinearLayout content(ScrollView scroll) {
        LinearLayout content = new LinearLayout(scroll.getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(0, 0, 0, AndroidUtilities.dp(20));
        scroll.addView(content);
        return content;
    }
    static LinearLayout section(LinearLayout content, int title, Theme.ResourcesProvider provider) {
        Context context = content.getContext();
        final TextView header = title == 0 ? null : new TextView(context);
        if (header != null) {
            header.setText(LocaleController.getString(title));
            header.setTextSize(13);
            header.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText, provider));
            header.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
            header.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(20), AndroidUtilities.dp(24), AndroidUtilities.dp(8));
            content.addView(header);
        }
        LinearLayout group = new LinearLayout(context) {
            @Override public void setVisibility(int visibility) {
                super.setVisibility(visibility);
                if (header != null) header.setVisibility(visibility);
            }
        };
        group.setOrientation(LinearLayout.VERTICAL);
        group.setBackground(Theme.createRoundRectDrawable(AndroidUtilities.dp(16), Theme.getColor(Theme.key_windowBackgroundWhite, provider)));
        group.setClipToOutline(true);
        content.addView(group, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 12, title == 0 ? 12 : 0, 12, 0));
        return group;
    }
    static LinearLayout advanced(LinearLayout content, Theme.ResourcesProvider provider) {
        LinearLayout heading = section(content, 0, provider);
        TextSettingsCell row = new TextSettingsCell(content.getContext(), provider);
        row.setTextAndValue(LocaleController.getString(R.string.FreezgramStyleAdvanced), LocaleController.getString(R.string.FreezgramStyleExpand), false);
        row.setBackground(Theme.getSelectorDrawable(false));
        heading.addView(row);
        LinearLayout options = section(content, 0, provider);
        options.setVisibility(View.GONE);
        row.setOnClickListener(v -> {
            boolean open = options.getVisibility() != View.VISIBLE;
            options.setVisibility(open ? View.VISIBLE : View.GONE);
            row.setTextAndValue(LocaleController.getString(R.string.FreezgramStyleAdvanced),
                    LocaleController.getString(open ? R.string.FreezgramStyleCollapse : R.string.FreezgramStyleExpand), false);
        });
        return options;
    }
    static void preview(LinearLayout content, View preview, int height, Theme.ResourcesProvider provider, boolean collapsed) {
        LinearLayout group = section(content, 0, provider);
        TextSettingsCell row = new TextSettingsCell(content.getContext(), provider);
        row.setBackground(Theme.getSelectorDrawable(false));
        row.setTextAndValue(LocaleController.getString(R.string.FreezgramStylePreview),
                LocaleController.getString(collapsed ? R.string.FreezgramStyleExpand : R.string.FreezgramStyleCollapse), false);
        group.addView(row);
        group.addView(preview, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, height));
        preview.setVisibility(collapsed ? View.GONE : View.VISIBLE);
        row.setOnClickListener(v -> {
            boolean open = preview.getVisibility() != View.VISIBLE;
            preview.setVisibility(open ? View.VISIBLE : View.GONE);
            row.setTextAndValue(LocaleController.getString(R.string.FreezgramStylePreview),
                    LocaleController.getString(open ? R.string.FreezgramStyleCollapse : R.string.FreezgramStyleExpand), false);
        });
    }
    static TextSettingsCell reset(LinearLayout content, Theme.ResourcesProvider provider, Runnable action) {
        TextSettingsCell row = new TextSettingsCell(content.getContext(), provider);
        row.setText(LocaleController.getString(R.string.FreezgramPanelReset), false);
        row.getTextView().setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText, provider));
        row.setBackground(Theme.getSelectorDrawable(false));
        section(content, 0, provider).addView(row);
        row.setOnClickListener(v -> action.run());
        return row;
    }
}
