package app.freezgram.badges;

import app.freezgram.network.FreezgramCommunity;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.view.Gravity;

import androidx.core.content.ContextCompat;

import org.telegram.messenger.AndroidUtilities;
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

    public static Drawable getExteraDrawable(Context context, boolean official) {
        Drawable arrow = ContextCompat.getDrawable(context, R.drawable.extera_profile_arrow).mutate();
        if (!official) {
            return arrow;
        }
        GradientDrawable plate = new GradientDrawable();
        plate.setShape(GradientDrawable.OVAL);
        plate.setColor(0xFF767676);
        plate.setSize(AndroidUtilities.dp(18), AndroidUtilities.dp(18));
        arrow.setTint(0xFFFFFFFF);
        LayerDrawable badge = new LayerDrawable(new Drawable[]{plate, arrow});
        badge.setLayerSize(1, AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        badge.setLayerGravity(1, Gravity.CENTER);
        return badge;
    }

    public static Drawable getPairedDrawable(Context context, Drawable extera) {
        Drawable freezgram = getDrawable(context);
        if (freezgram == null) {
            return extera;
        }
        int gap = AndroidUtilities.dp(2);
        LayerDrawable pair = new LayerDrawable(new Drawable[]{freezgram, extera});
        pair.setPaddingMode(LayerDrawable.PADDING_MODE_STACK);
        pair.setLayerSize(0, freezgram.getIntrinsicWidth(), freezgram.getIntrinsicHeight());
        pair.setLayerSize(1, extera.getIntrinsicWidth(), extera.getIntrinsicHeight());
        pair.setLayerGravity(0, Gravity.LEFT | Gravity.CENTER_VERTICAL);
        pair.setLayerGravity(1, Gravity.LEFT | Gravity.CENTER_VERTICAL);
        pair.setLayerInset(1, freezgram.getIntrinsicWidth() + gap, 0, 0, 0);
        return pair;
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
