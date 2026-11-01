package app.freezgram;

import android.content.Context;
import android.view.View;
import androidx.recyclerview.widget.RecyclerView;
import app.exteraless.appearance.AppearanceConfig;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.Cells.TextRadioCell;
import org.telegram.messenger.AndroidUtilities;
import tw.nekomimi.nekogram.NekoConfig;
import xyz.nextalone.nagram.NaConfig;
import tw.nekomimi.nekogram.settings.BaseNekoSettingsActivity;
import tw.nekomimi.nekogram.ui.cells.HeaderCell;

/** Single entry point; opening this list never changes the selected styles. */
public final class FreezgramStyleActivity extends BaseNekoSettingsActivity {
    private boolean motionExpanded, snowExpanded, effectsChanged;
    @Override protected String getActionBarTitle() { return text(R.string.FreezgramStyleTitle); }
    private String text(int id) { return LocaleController.getString(id); }
    @Override protected void updateRows() {
        super.updateRows();
        addRow("interface");
        addRow("navigation"); addRow("header"); addRow("composer"); addRow("chatList"); addRow();
        addRow("chat"); addRow("messages"); addRow("gifts"); addRow("surfaces"); addRow();
        addRow("effects"); addRow("glass"); addRow("dividers"); addRow("motion");
        if (motionExpanded) { addRow("motionFreezgram"); addRow("motionStandard"); }
        addRow("snow");
        if (snowExpanded) { addRow("snowOff"); addRow("snowFlakes"); addRow("snowDots"); }
        addRow();
    }
    public static BaseFragment editorFor(String key) {
        if (key == null) return null;
        switch (key) {
            case "navigation": return new NavigationGlassSettingsActivity(NavigationGlassSettingsActivity.NAVIGATION);
            case "header": return new ChatHeaderSettingsActivity();
            case "composer": return new NavigationGlassSettingsActivity(NavigationGlassSettingsActivity.COMPOSER);
            case "chatList": return new StyleSurfacesSettingsActivity(StyleSurfacesSettingsActivity.CHAT_LIST);
            case "messages": return new MessageSurfaceSettingsActivity();
            case "gifts": return new GiftGlassSettingsActivity();
            case "surfaces": return new StyleSurfacesSettingsActivity(StyleSurfacesSettingsActivity.CHAT);
            case "glass": return new NavigationGlassSettingsActivity(NavigationGlassSettingsActivity.SHARED);
            case "dividers": return new GlassDividerSettingsActivity();
            default: return null;
        }
    }
    @Override protected void onItemClick(View view, int position, float x, float y) {
        String key = rowMapReverse.get(position);
        if (key == null) return;
        if (key.equals("motion")) { motionExpanded = !motionExpanded; refreshRows(); return; }
        if (key.equals("snow")) { snowExpanded = !snowExpanded; refreshRows(); return; }
        if (key.equals("motionFreezgram") || key.equals("motionStandard")) {
            AppearanceConfig.transitionStyle.setConfigInt(key.equals("motionFreezgram") ? 1 : 0);
            effectsChanged = true; refreshRows(); return;
        }
        if (key.equals("snowOff") || key.equals("snowFlakes") || key.equals("snowDots")) {
            boolean enabled = !key.equals("snowOff");
            NekoConfig.actionBarDecoration.setConfigInt(enabled ? 1 : 0);
            NaConfig.INSTANCE.getChatDecoration().setConfigInt(enabled ? 1 : 0);
            if (enabled) AppearanceConfig.snowStyle.setConfigInt(key.equals("snowDots") ? 1 : 0);
            effectsChanged = true; refreshRows(); return;
        }
        BaseFragment editor = editorFor(key);
        if (editor != null) presentFragment(editor);
    }
    private void refreshRows() {
        int oldCount = rowCount;
        java.util.HashMap<Integer, String> oldRows = new java.util.HashMap<>(rowMapReverse);
        updateRows();
        if (listAdapter == null) return;
        androidx.recyclerview.widget.DiffUtil.calculateDiff(new androidx.recyclerview.widget.DiffUtil.Callback() {
            @Override public int getOldListSize() { return oldCount; }
            @Override public int getNewListSize() { return rowCount; }
            @Override public boolean areItemsTheSame(int oldPosition, int newPosition) {
                String key = oldRows.get(oldPosition);
                return key != null && key.equals(rowMapReverse.get(newPosition));
            }
            @Override public boolean areContentsTheSame(int oldPosition, int newPosition) { return false; }
            @Override public Object getChangePayload(int oldPosition, int newPosition) { return PARTIAL; }
        }, false).dispatchUpdatesTo(listAdapter);
    }
    private int label(String key) {
        switch (key) {
            case "interface": return R.string.FreezgramStyleInterface;
            case "chat": return R.string.FreezgramStyleChat;
            case "effects": return R.string.FreezgramStyleEffects;
            case "navigation": return R.string.FreezgramPanelSettings;
            case "header": return R.string.FreezgramStyleChatHeader;
            case "composer": return R.string.FreezgramComposerSettings;
            case "chatList": return R.string.FreezgramStyleChatList;
            case "messages": return R.string.FreezgramMessageSurfaces;
            case "gifts": return R.string.FreezgramGiftGlassSettings;
            case "surfaces": return R.string.FreezgramStyleMenus;
            case "glass": return R.string.FreezgramStyleGlass;
            case "dividers": return R.string.FreezgramGlassMenuDividers;
            case "motion": return R.string.FreezgramTransitionStyle;
            case "motionFreezgram": return R.string.FreezgramTransitionSmooth;
            case "motionStandard": return R.string.FreezgramTransitionStandard;
            case "snowOff": return R.string.FreezgramStyleOff;
            case "snowFlakes": return R.string.FreezgramSnowStyleFlakes;
            case "snowDots": return R.string.FreezgramSnowStyleDots;
            default: return R.string.FreezgramSnowStyle;
        }
    }
    private String value(String key) {
        switch (key) {
            case "navigation": return AppearanceConfig.iosNavigationBarStyle() ? "iOS" : text(AppearanceConfig.newNavigationBarStyle() ? R.string.FreezgramStyleMaterial : R.string.FreezgramChatComposerDefault);
            case "header": return AppearanceConfig.iosChatHeader() ? "iOS" : text(AppearanceConfig.newChatHeaderStyle() ? R.string.FreezgramStyleMaterial : R.string.FreezgramChatComposerDefault);
            case "composer": return AppearanceConfig.chatComposerStyle() == 1 ? "iOS" : "Android";
            case "messages": return text(AppearanceConfig.sameMessageSurfaces() ? R.string.FreezgramStyleLinked : R.string.FreezgramStyleSeparate);
            case "gifts": return text(new int[]{R.string.FreezgramGiftCardOrdinary, R.string.FreezgramGiftCardGlass, R.string.FreezgramGiftCardSoft}[GiftGlassStyle.style()]);
            case "glass": return LiquidGlassStyle.blurRadiusDp() + " dp · " + AppearanceConfig.navigationGlassIntensity() + "%";
            case "dividers": return text(AppearanceConfig.glassMenuDividers() ? R.string.FreezgramStyleOn : R.string.FreezgramStyleOff);
            case "motion": return text(AppearanceConfig.transitionStyle() == 1 ? R.string.FreezgramTransitionSmooth : R.string.FreezgramTransitionStandard);
            case "snow": return text(tw.nekomimi.nekogram.NekoConfig.actionBarDecoration.Int() != 1 ? R.string.FreezgramStyleOff
                    : AppearanceConfig.snowStyle() == 1 ? R.string.FreezgramSnowStyleDots : R.string.FreezgramSnowStyleFlakes);
            default: return "";
        }
    }
    @Override protected BaseListAdapter createAdapter(Context context) {
        return new BaseListAdapter(context) {
            @Override public int getItemViewType(int position) {
                String key = rowMapReverse.get(position);
                if (key == null || key.isEmpty()) return TYPE_SHADOW;
                if (key.equals("motionFreezgram") || key.equals("motionStandard") || key.equals("snowOff") || key.equals("snowFlakes") || key.equals("snowDots")) return TYPE_RADIO;
                return key.equals("interface") || key.equals("chat") || key.equals("effects") ? TYPE_HEADER : TYPE_SETTINGS;
            }
            @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, boolean partial) {
                String key = rowMapReverse.get(position);
                if (getItemViewType(position) == TYPE_HEADER) ((HeaderCell) holder.itemView).setText(text(label(key)));
                else if (getItemViewType(position) == TYPE_RADIO) {
                    boolean snowEnabled = NekoConfig.actionBarDecoration.Int() == 1;
                    boolean checked = key.equals("motionFreezgram") ? AppearanceConfig.transitionStyle() == 1
                            : key.equals("motionStandard") ? AppearanceConfig.transitionStyle() == 0
                            : key.equals("snowOff") ? !snowEnabled
                            : snowEnabled && (key.equals("snowDots") ? AppearanceConfig.snowStyle() == 1 : AppearanceConfig.snowStyle() == 0);
                    TextRadioCell cell = (TextRadioCell) holder.itemView;
                    if (partial) cell.setChecked(checked);
                    else cell.setTextAndValueAndCheck(text(label(key)), "", checked, false, false);
                }
                else if (getItemViewType(position) == TYPE_SETTINGS) ((TextSettingsCell) holder.itemView).setTextAndValue(text(label(key)), value(key), position + 1 < rowCount && getItemViewType(position + 1) == TYPE_SETTINGS);
            }
        };
    }
    @Override public void onFragmentDestroy() {
        android.app.Activity activity = getParentActivity(); super.onFragmentDestroy();
        if (effectsChanged && activity instanceof org.telegram.ui.LaunchActivity) AndroidUtilities.runOnUIThread(() -> ((org.telegram.ui.LaunchActivity) activity).rebuildAllFragments(false));
    }
}
