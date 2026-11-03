package app.freezgram;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;

/** Read-only links: opening a style destination never applies a preference value. */
final class StyleSettingsLinks {
    private StyleSettingsLinks() {}
    static String link(String section) {
        return "t.me/freezgram/style" + (section == null ? "" : "?r=" + section);
    }
    static void install(BaseFragment fragment, String section) {
        var menu = fragment.getActionBar().createMenu();
        var item = menu.getItem(9001);
        if (item == null) item = menu.addItem(9001, R.drawable.msg_link);
        item.setContentDescription(LocaleController.getString(R.string.CopyLink));
        item.setOnClickListener(v -> {
            AndroidUtilities.addToClipboard(link(section));
            BulletinFactory.of(fragment).createCopyLinkBulletin().show();
        });
    }
}
