package app.freezgram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.view.Gravity;
import android.widget.LinearLayout;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.ChatMessageCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SizeNotifierFrameLayout;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableSource;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSource;
import org.telegram.ui.Stories.recorder.PreviewView;

/** Native incoming/outgoing messages and a channel post, over the current wallpaper. */
public final class MessageSurfacePreview extends SizeNotifierFrameLayout {
    private final LinearLayout messages;
    private final int account;
    private long selfId;
    private ChatMessageCell stickerCell;
    private android.widget.TextView stickerStatus;
    private boolean stickerLoaded;
    private int stickerAttempts;
    private final Runnable stickerLoader = this::loadSticker;
    private void loadSticker() {
        if (stickerCell == null || stickerLoaded) return;
        TLRPC.Document document = org.telegram.messenger.MediaDataController.getInstance(account).getGreetingsSticker();
        if (document == null) { if (stickerAttempts++ < 20) postDelayed(stickerLoader, 500); return; }
        TLRPC.TL_message message = previewMessage(selfId, 0);
        TLRPC.TL_messageMediaDocument media = new TLRPC.TL_messageMediaDocument(); media.flags |= 1; media.document = document;
        message.media = media; message.message = "";
        message.reply_to = new TLRPC.TL_messageReplyHeader(); message.reply_to.reply_to_msg_id = 20;
        MessageObject object = new MessageObject(account, message, true, false);
        object.preview = false; object.replyMessageObject = new MessageObject(account, previewMessage(selfId, 1), true, false);
        stickerCell.setMessageObject(object, null, false, false, false);
        stickerLoaded = true; stickerCell.setVisibility(VISIBLE); stickerStatus.setVisibility(GONE);
        messages.setMinimumHeight(0); requestLayout(); refresh();
    }
    public MessageSurfacePreview(Context context, int account, Theme.ResourcesProvider provider) { this(context, account, provider, false); }
    public MessageSurfacePreview(Context context, int account, Theme.ResourcesProvider provider, boolean stickerDetails) {
        super(context);
        this.account = account;
        long self = UserConfig.getInstance(account).getClientUserId();
        selfId = self;
        setBackgroundImage(PreviewView.getBackgroundDrawable(null, account, self, Theme.isCurrentThemeDark()), false);
        messages = new LinearLayout(context); messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0, org.telegram.messenger.AndroidUtilities.dp(12), 0, org.telegram.messenger.AndroidUtilities.dp(12));
        addView(messages, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        BlurredBackgroundSource wallpaper = new BlurredBackgroundSource() {
            @Override public BlurredBackgroundDrawable createDrawable() { return new BlurredBackgroundDrawableSource(this); }
            @Override public void draw(Canvas canvas, float l, float t, float r, float b) { drawWallpaperForGlass(canvas); }
        };
        MessageSurfaceRenderer renderer = new MessageSurfaceRenderer(this, wallpaper, provider);
        for (int profile = 0; profile < (stickerDetails ? 1 : 3); profile++) {
            ChatMessageCell cell = new ChatMessageCell(context, account, false, null, provider);
            cell.setDelegate(new ChatMessageCell.ChatMessageCellDelegate() {
                @Override public BlurredBackgroundDrawable drawMessageDecorationGlass(ChatMessageCell c, Canvas canvas, RectF bounds, float radius, BlurredBackgroundDrawable cached) {
                    return renderer.decoration(c, canvas, bounds, radius, cached);
                }
                @Override public boolean drawMessageSurface(ChatMessageCell c, Canvas canvas, RectF bounds, int p, MessageSurfaceStyle.Surface surface, float alpha) {
                    return renderer.draw(c, canvas, bounds, p, surface, alpha);
                }
            });
            TLRPC.TL_message message = previewMessage(self, profile);
            if (profile == 2) { cell.hasDiscussion = true; cell.linkedChatId = 0; }
            MessageObject object = new MessageObject(account, message, true, false);
            // Normal ChatMessageCell.onDraw draws content with preview=false. The special
            // send-preview flag suppresses that pass; touch handling is disabled separately.
            object.preview = false; cell.isChat = false; cell.setFullyDraw(true);
            cell.setMessageObject(object, null, false, false, false);
            cell.setOnTouchListener((v, event) -> true);
            messages.addView(cell, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
            if (stickerDetails) { stickerCell = cell; cell.setVisibility(INVISIBLE); }
        }
        if (stickerDetails) {
            messages.setMinimumHeight(org.telegram.messenger.AndroidUtilities.dp(180));
            stickerStatus = new android.widget.TextView(context); stickerStatus.setGravity(Gravity.CENTER);
            stickerStatus.setText(LocaleController.getString(R.string.Loading));
            stickerStatus.setTextColor(Theme.getColor(Theme.key_chat_serviceText, provider));
            addView(stickerStatus, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 180, Gravity.TOP));
        }
    }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); if (stickerCell != null) post(stickerLoader); }
    @Override protected void onDetachedFromWindow() { removeCallbacks(stickerLoader); super.onDetachedFromWindow(); }
    static TLRPC.TL_message previewMessage(long self, int profile) {
        // A synthetic non-self dialog keeps incoming examples out of Saved Messages logic.
        long peer = self == 1 ? 2 : 1;
        TLRPC.TL_message message = new TLRPC.TL_message();
        message.id = profile + 1; message.date = 43200 + profile * 60; message.dialog_id = peer;
        message.flags = 257; message.out = profile == 1;
        message.message = LocaleController.getString(profile == 0 ? R.string.FreezgramPreviewIncomingText : profile == 1 ? R.string.FreezgramPreviewOutgoingText : R.string.FreezgramPreviewPostText);
        message.from_id = new TLRPC.TL_peerUser(); message.from_id.user_id = message.out ? self : peer;
        message.peer_id = new TLRPC.TL_peerUser(); message.peer_id.user_id = peer;
        message.media = new TLRPC.TL_messageMediaEmpty();
        if (profile == 2) {
            message.replies = new TLRPC.TL_messageReplies(); message.replies.comments = true; message.replies.replies = 12;
        }
        return message;
    }
    public void refresh() {
        for (int i = 0; i < messages.getChildCount(); i++) messages.getChildAt(i).invalidate();
        invalidate();
    }
    @Override protected boolean isActionBarVisible() { return false; }
    @Override protected boolean isStatusBarVisible() { return false; }
    @Override protected boolean useRootView() { return false; }
    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        messages.measure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(messages.getMeasuredHeight(), MeasureSpec.EXACTLY));
    }
    @Override protected void onBackgroundViewInvalidate() {
        super.onBackgroundViewInvalidate();
        if (messages != null) refresh();
    }
}
