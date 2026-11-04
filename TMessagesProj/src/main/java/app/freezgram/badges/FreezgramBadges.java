package app.freezgram.badges;

import app.freezgram.network.FreezgramCommunity;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.LocaleController;
import org.telegram.messenger.ChatObject;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.Bulletin;
import org.telegram.ui.Components.BulletinFactory;

/** Community badge artwork is bundled; the API supplies eligible recipients. */
public final class FreezgramBadges {

    private FreezgramBadges() {}

    public static boolean hasBadge(TLRPC.User user) {
        return user != null && FreezgramCommunity.hasBadge(user.id, false)
                && !user.scam && !user.fake && !user.verified;
    }

    public static Drawable getDrawable(Context context) {
        Drawable plane = ContextCompat.getDrawable(context, R.drawable.freezgram_settings_icon);
        return plane == null ? null : new FreezgramBadgeDrawable(plane.mutate());
    }

    public static boolean hasChannelBadge(TLRPC.Chat chat) {
        return chat != null && FreezgramCommunity.hasBadge(chat.id, true) && ChatObject.isChannel(chat)
                && !chat.megagroup && !chat.scam && !chat.fake && !chat.verified;
    }

    public static Drawable getChannelDrawable(Context context) {
        return getDrawable(context);
    }

    public static String getTitle() {
        return LocaleController.getString(R.string.FreezgramBadgeMember);
    }

    public static void showInfo(BaseFragment fragment) {
        showInfo(fragment, false);
    }

    public static String getChannelTitle() {
        return LocaleController.getString(R.string.FreezgramBadgeChannel);
    }

    public static void showChannelInfo(BaseFragment fragment) {
        showInfo(fragment, true);
    }

    private static void showInfo(BaseFragment fragment, boolean channel) {
        if (fragment == null || fragment.getContext() == null) {
            return;
        }
        Bulletin bulletin = BulletinFactory.of(fragment).createSimpleBulletin(
                getDrawable(fragment.getContext()), channel ? getChannelTitle() : getTitle(),
                LocaleController.getString(R.string.FreezgramBadgeLocalInfo));
        bulletin.show();
    }
}
