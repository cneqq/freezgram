package app.freezgram;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.tgnet.tl.TL_stars;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatActionCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.utils.glass.source.GlassSourceRenderNode;
import org.telegram.ui.Components.chat.ViewPositionWatcher;
import org.telegram.ui.Stars.StarsController;
import org.telegram.ui.Stories.recorder.PreviewView;

/** A real catalog gift rendered by the same cell and wallpaper as SendGiftSheet. */
public final class GiftCardPreview extends SizeNotifierFrameLayout implements NotificationCenter.NotificationCenterDelegate {
    private final int account;
    private final ChatActionCell cell;
    private final TextView status;
    private GlassSourceRenderNode source;
    private long selectedGiftId;

    public GiftCardPreview(Context context, int account, Theme.ResourcesProvider provider) {
        super(context);
        this.account = account;
        long self = UserConfig.getInstance(account).getClientUserId();
        setBackgroundImage(PreviewView.getBackgroundDrawable(null, account, self, Theme.isCurrentThemeDark()), false);
        cell = new ChatActionCell(context, false, provider) {
            @Override protected void onDraw(Canvas canvas) {
                captureWallpaper(canvas);
                super.onDraw(canvas);
            }
        };
        cell.setDelegate(new ChatActionCell.ChatActionCellDelegate() {});
        cell.setVisibility(View.GONE);
        addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 0, 16, 0, 16));
        status = new TextView(context);
        status.setText(LocaleController.getString(R.string.Loading));
        status.setTextSize(14); status.setGravity(Gravity.CENTER);
        status.setTextColor(Theme.getColor(Theme.key_chat_serviceText, provider));
        addView(status, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 200, Gravity.CENTER));
        if (Build.VERSION.SDK_INT >= 33) {
            source = new GlassSourceRenderNode(null);
            var factory = new BlurredBackgroundDrawableViewFactory(new ViewPositionWatcher(this), this, SmallElementsGlass.createGiftSource(source));
            factory.setLiquidGlassEffectAllowed(true); cell.setServiceGlassFactory(factory);
        }
    }
    private void captureWallpaper(Canvas canvas) {
        if (source == null || !canvas.isHardwareAccelerated() || !SmallElementsGlass.areGiftCardsEnabled() || getWidth() <= 0 || getHeight() <= 0) return;
        Canvas capture = source.beginRecording(getWidth(), getHeight());
        drawWallpaperForGlass(capture); source.endRecording();
    }
    static TL_stars.StarGift selectGift(java.util.List<TL_stars.StarGift> gifts) {
        TL_stars.StarGift selected = null;
        for (TL_stars.StarGift gift : gifts) {
            if (!(gift instanceof TL_stars.TL_starGift) || gift.sticker == null || gift.sold_out || gift.auction || gift.stars <= 0) continue;
            if (selected == null || gift.stars < selected.stars) selected = gift;
        }
        return selected;
    }
    private void updateGift() {
        StarsController controller = StarsController.getInstance(account);
        TL_stars.StarGift gift = selectGift(controller.gifts);
        if (gift == null) {
            if (controller.giftsLoaded) status.setText(LocaleController.getString(R.string.FreezgramGiftPreviewUnavailable));
            return;
        }
        if (selectedGiftId == gift.id) return;
        selectedGiftId = gift.id;
        TLRPC.TL_messageActionStarGift action = new TLRPC.TL_messageActionStarGift();
        action.gift = gift; action.flags |= 2;
        action.message = new TLRPC.TL_textWithEntities();
        action.convert_stars = gift.convert_stars; action.forceIn = true;
        long self = UserConfig.getInstance(account).getClientUserId();
        TLRPC.TL_messageService message = new TLRPC.TL_messageService();
        message.id = 1; message.dialog_id = self;
        message.from_id = MessagesController.getInstance(account).getPeer(self);
        message.peer_id = MessagesController.getInstance(account).getPeer(self);
        message.action = action;
        cell.setMessageObject(new MessageObject(account, message, false, false), true);
        status.setVisibility(View.GONE); cell.setVisibility(View.VISIBLE); requestLayout();
    }
    public void refreshStyle() { cell.invalidate(); invalidate(); requestLayout(); }
    @Override protected boolean isActionBarVisible() { return false; }
    @Override protected boolean isStatusBarVisible() { return false; }
    @Override protected boolean useRootView() { return false; }
    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = org.telegram.messenger.AndroidUtilities.dp(240);
        if (cell.getVisibility() == View.VISIBLE) {
            cell.measure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
            height = cell.getMeasuredHeight() + org.telegram.messenger.AndroidUtilities.dp(32);
        }
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }
    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        cell.setVisiblePart(cell.getY(), getBackgroundSizeY());
    }
    @Override protected void onBackgroundViewInvalidate() {
        super.onBackgroundViewInvalidate();
        if (cell != null) cell.invalidate();
    }
    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        NotificationCenter.getInstance(account).addObserver(this, NotificationCenter.starGiftsLoaded);
        updateGift(); StarsController.getInstance(account).loadStarGifts();
    }
    @Override protected void onDetachedFromWindow() {
        NotificationCenter.getInstance(account).removeObserver(this, NotificationCenter.starGiftsLoaded);
        super.onDetachedFromWindow();
    }
    @Override public void didReceivedNotification(int id, int currentAccount, Object... args) {
        if (id == NotificationCenter.starGiftsLoaded) updateGift();
    }
}
