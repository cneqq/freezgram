package org.telegram.ui.Components;

import static org.telegram.messenger.AndroidUtilities.dp;
import static org.telegram.messenger.AndroidUtilities.dpf2;
import static org.telegram.messenger.AndroidUtilities.lerp;
import static org.telegram.messenger.LocaleController.formatString;
import static org.telegram.messenger.LocaleController.getString;
import static org.telegram.ui.ActionBar.Theme.multAlpha;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.UserObject;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;

import xyz.nextalone.nagram.NaConfig;

public class ProfileMusicView extends View {

    private final Theme.ResourcesProvider resourcesProvider;
    protected final boolean iosLayout;
    private final PorterDuffColorFilter filterColorWhite = new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);
    private final PorterDuffColorFilter filterColorBlack = new PorterDuffColorFilter(Color.BLACK, PorterDuff.Mode.SRC_IN);

    private Text author, title;
    private ProfileMusicView trailingView;
    private ProfileMusicView leadingView;

    // The two pills share a centered row and the same profile/blur animation state.
    public void setTrailingView(ProfileMusicView view) {
        trailingView = view;
        view.leadingView = this;
        invalidate();
        view.invalidate();
    }

    private final Paint iconPaint = new Paint();
    private final Paint arrowPaint = new Paint();
    private final Path arrowPath = new Path();
    private final Drawable icon;

    private final RectF rect = new RectF();

    public void getButtonBoundsOnScreen(RectF bounds) {
        int[] location = new int[2];
        getLocationOnScreen(location);
        bounds.set(rect);
        bounds.offset(location[0], location[1]);
    }

    private final Paint backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ProfileButtonsGlass.IosRim iosRim = new ProfileButtonsGlass.IosRim();
    private final Paint iosFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int iosCoverColor = Color.GRAY;
    private final Path clipPath = new Path();

    private final ButtonBounce bounce = new ButtonBounce(this);

    public ProfileMusicView(Context context, Theme.ResourcesProvider resourcesProvider, boolean iosLayout) {
        super(context);
        this.resourcesProvider = resourcesProvider;
        this.iosLayout = iosLayout;

        icon = context.getResources().getDrawable(R.drawable.files_music).mutate();

        arrowPaint.setStyle(Paint.Style.STROKE);
        arrowPaint.setStrokeCap(Paint.Cap.ROUND);
        arrowPaint.setStrokeJoin(Paint.Join.ROUND);
        arrowPath.moveTo(0, -dpf2(3.33f));
        arrowPath.lineTo(dpf2(3.16f), 0);
        arrowPath.lineTo(0, dpf2(3.33f));

        setColor(null);
        setText("Author", " - Title");
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(
                MeasureSpec.getSize(widthMeasureSpec),
                MeasureSpec.EXACTLY
            ),
            MeasureSpec.makeMeasureSpec(
                dp(37),
                MeasureSpec.EXACTLY
            )
        );
    }

    protected int textColor = Color.WHITE;
    protected boolean hasProfileColor;
    private float parentExpanded;
    private int buttonColor;
    private boolean hasColorById;
    private RadialGradient radialGradient;
    private float radialGradientWidth;
    private final Paint shaderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Matrix matrix = new Matrix();

    public void setColor(MessagesController.PeerColor peerColor) {
        hasProfileColor = peerColor != null;
        if (trailingView != null) trailingView.setColor(peerColor);
        invalidate();
    }

    // Same color as ProfileActionsView buttons.
    public void setButtonColor(int color, boolean hasColorById) {
        if (trailingView != null) trailingView.setButtonColor(color, hasColorById);
        if (buttonColor == color && this.hasColorById == hasColorById) return;
        buttonColor = color;
        this.hasColorById = hasColorById;
        radialGradient = null;
        backgroundPaint.setColor(color);
        checkTextColor();
    }

    public void setIosCoverColor(int color) {
        if (trailingView != null) trailingView.setIosCoverColor(color);
        if (iosCoverColor != color) {
            iosCoverColor = color;
            invalidate();
        }
    }

    private boolean isButtonColorLight() {
        return AndroidUtilities.computePerceivedBrightness(buttonColor) > 0.72f;
    }

    private void checkTextColor() {
        final boolean useBlackText = parentExpanded < 0.8f && isButtonColorLight();
        textColor = useBlackText ? Color.BLACK : Color.WHITE;
        icon.setColorFilter(useBlackText ? filterColorBlack : filterColorWhite);
        iconPaint.setColor(textColor);
        arrowPaint.setColor(Theme.multAlpha(textColor, 0.85f));
        invalidate();
    }

    public void setParentExpanded(float expanded) {
        if (trailingView != null) trailingView.setParentExpanded(expanded);
        if (parentExpanded != expanded) {
            parentExpanded = expanded;
            checkTextColor();
            invalidate();
        }
    }



    public void setMusicDocument(TLRPC.Document document) {
        CharSequence author = getAuthor(document);
        CharSequence title = getTitle(document);
        if (iosLayout) {
            if (TextUtils.isEmpty(title)) {
                title = TextUtils.isEmpty(author) ? getString(R.string.AudioUnknownTitle) : author;
                author = "";
            }
            setText(title, TextUtils.isEmpty(author) ? "" : " – " + author);
            return;
        }
        if (TextUtils.isEmpty(author)) {
            if (TextUtils.isEmpty(title)) {
                author = getString(R.string.AudioUnknownArtist);
                title = " - " + getString(R.string.AudioUnknownTitle);
            } else {
                author = "";
            }
        } else if (!TextUtils.isEmpty(title)) {
            title = " - " + title;
        } else {
            title = "";
        }
        setText(author, title);
    }

    public static CharSequence getTitle(TLRPC.Document document) {
        if (document == null) {
            return null;
        }
        for (int a = 0; a < document.attributes.size(); a++) {
            TLRPC.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                String title = attribute.title;
                if (title == null || title.length() == 0) {
                    title = FileLoader.getDocumentFileName(document);
                }
                return title;
            }
        }
        String fileName = FileLoader.getDocumentFileName(document);
        if (!TextUtils.isEmpty(fileName)) {
            return fileName;
        }
//        return getString(R.string.AudioUnknownTitle);
        return null;
    }

    public static CharSequence getAuthor(TLRPC.Document document) {
        if (document == null) {
            return null;
        }
        for (int a = 0; a < document.attributes.size(); a++) {
            TLRPC.DocumentAttribute attribute = document.attributes.get(a);
            if (attribute instanceof TLRPC.TL_documentAttributeAudio) {
                if (!attribute.voice) {
                    return attribute.performer;
                }
            }
        }
        return null;
    }

    public void setText(CharSequence author, CharSequence title) {
        this.author = new Text(author, iosLayout ? 12 : 11,
                iosLayout ? app.freezgram.IosTypography.Profile.SEMIBOLD : AndroidUtilities.bold());
        this.title = iosLayout ? new Text(title, 12, app.freezgram.IosTypography.Profile.REGULAR) : new Text(title, 11);

        setContentDescription(getString(R.string.AccDescrProfileMusic) + " " + author + title);
    }

    /** The pill's glass, like the action buttons'; without it the pill is filled with their color. */
    @Nullable
    private BlurredBackgroundDrawable glass;
    private float glassRadius;

    /** The pill on glass, see {@link ProfileButtonsGlass#actionsFactory}. */
    public void setGlassFactory(@Nullable BlurredBackgroundDrawableViewFactory factory) {
        if (trailingView != null) trailingView.setGlassFactory(factory);
        glass = factory != null ? factory.create(this, ProfileButtonsGlass.NO_COLORS) : null;
        glassRadius = 0;
        invalidate();
    }

    private float currentHeight;

    @Override
    public void setAlpha(float alpha) {
        super.setAlpha(alpha);
        if (trailingView != null) trailingView.setAlpha(alpha);
    }

    protected boolean isOnProfilePhoto() {
        return parentExpanded >= 0.8f;
    }

    public void updatePosition(float y, float newHeight) {
        if (trailingView != null) trailingView.updatePosition(y, newHeight);
        currentHeight = newHeight;
        setTranslationY(iosLayout ? y + dp(17) : y - dp(12));
        invalidate();
    }

    private boolean longPressed;
    private final Runnable longPressRunnable = () -> {
        if (bounce.isPressed()) {
            longPressed = true;
            bounce.setPressed(false);
            performLongClick();
        }
    };

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        final float alpha = getRevealProgress();
        if (alpha <= 0) return false;
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            longPressed = false;
            bounce.setPressed(rect.contains(event.getX(), event.getY()));
            if (bounce.isPressed() && isLongClickable()) {
                postDelayed(longPressRunnable, ViewConfiguration.getLongPressTimeout());
            }
        } else if (event.getAction() == MotionEvent.ACTION_MOVE && bounce.isPressed()) {
            if (!rect.contains(event.getX(), event.getY())) {
                bounce.setPressed(false);
                removeCallbacks(longPressRunnable);
            }
        } else if (event.getAction() == MotionEvent.ACTION_CANCEL) {
            bounce.setPressed(false);
            removeCallbacks(longPressRunnable);
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            removeCallbacks(longPressRunnable);
            if (bounce.isPressed() && !longPressed) {
                performClick();
            }
            bounce.setPressed(false);
        }
        return bounce.isPressed() || longPressed;
    }

    private float getRevealProgress() {
        return Utilities.clamp01(currentHeight / dp(iosLayout ? 34 : 21));
    }

    protected int getPillHorizontalPadding() {
        return dp(iosLayout ? 12 : 8);
    }

    protected float getContentCenterY() {
        return iosLayout ? rect.centerY() : getHeight() / 2f;
    }

    protected float getContentWidth(float maxWidth) {
        final int arrowWidth = iosLayout ? 0 : dp(8);
        final float textWidth = Math.max(0, maxWidth - 2 * getPillHorizontalPadding() - dp(16.6f) - arrowWidth);
        final float authorWidth = iosLayout
                ? Math.max(textWidth * .45f, textWidth - title.getCurrentWidth()) : textWidth / 2f;
        this.author.ellipsize(authorWidth);
        this.title.ellipsize(Math.max(0, textWidth - this.author.getWidth()));
        return dp(16.6f) + this.author.getWidth() + this.title.getWidth() + arrowWidth;
    }

    protected void drawContent(Canvas canvas, float alpha) {
        final float cy = getContentCenterY();

        if (iosLayout) {
            iconPaint.setStrokeCap(Paint.Cap.ROUND);
            iconPaint.setStrokeWidth(dpf2(1.6f));
            iconPaint.setAlpha(Math.round(255 * alpha));
            canvas.drawLine(dpf2(2), cy - dp(5), dpf2(2), cy + dp(5), iconPaint);
            canvas.drawLine(dpf2(6), cy - dp(3), dpf2(6), cy + dp(3), iconPaint);
            canvas.drawLine(dpf2(10), cy - dpf2(1.5f), dpf2(10), cy + dpf2(1.5f), iconPaint);
        } else {
            final int iconSz = dp(13);
            final int iconTop = Math.round(cy - iconSz / 2f);
            icon.setBounds(0, iconTop, iconSz, iconTop + iconSz);
            icon.setAlpha(Math.round(255 * alpha));
            icon.draw(canvas);
        }

        canvas.translate(dp(16.6f), 0);
        this.author.draw(canvas, 0, cy, textColor, alpha);
        canvas.translate(this.author.getWidth(), 0);
        this.title.draw(canvas, 0, cy, textColor, (iosLayout ? .73f : .85f) * alpha);
        if (iosLayout) {
            return;
        }
        canvas.translate(this.title.getWidth(), 0);

        arrowPaint.setStrokeWidth(dpf2(1.16f));
        arrowPaint.setAlpha(Math.round(217 * alpha));
        canvas.translate(dpf2(4.8f), cy);
        canvas.drawPath(arrowPath, arrowPaint);
    }

    private boolean hasVisibleCompanion() {
        ProfileMusicView companion = trailingView != null ? trailingView : leadingView;
        return companion != null && companion.getVisibility() == VISIBLE;
    }

    private float getPillWidth(float maxWidth) {
        return 2 * getPillHorizontalPadding() + getContentWidth(maxWidth);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        if (this.author == null || this.title == null) return;

        final float alpha = getRevealProgress();
        final float scale = bounce.getScale(0.02f);
        if (alpha <= 0) {
            if (glass != null) {
                glass.setAlpha(0);
            }
            return;
        }

        final int padding = dp(iosLayout ? 18 : 12);
        final int gap = dp(iosLayout ? 8 : 6);
        final int availableWidth = Math.max(0, getWidth() - padding * 2);
        final int maxWidth = iosLayout ? Math.min(dp(400), availableWidth) : availableWidth;

        float containerWidth;
        float left;
        if (hasVisibleCompanion()) {
            ProfileMusicView leading = leadingView != null ? leadingView : this;
            ProfileMusicView trailing = trailingView != null ? trailingView : this;
            final float trailingWidth;
            final float leadingWidth;
            if (iosLayout) {
                final float halfRow = Math.max(0, (maxWidth - gap) / 2f);
                trailingWidth = Math.min(halfRow, Math.max(dp(140), trailing.getPillWidth(halfRow)));
                leadingWidth = trailingWidth;
            } else {
                trailingWidth = trailing.getPillWidth(maxWidth / 2f);
                leadingWidth = leading.getPillWidth(Math.max(0, maxWidth - trailingWidth - gap));
            }
            left = (getWidth() - leadingWidth - gap - trailingWidth) / 2f;
            containerWidth = leadingView != null ? trailingWidth : leadingWidth;
            if (leadingView != null) left += leadingWidth + gap;
        } else {
            containerWidth = getPillWidth(maxWidth);
            left = (getWidth() - containerWidth) / 2f;
        }

        if (iosLayout && LocaleController.isRTL) {
            left = getWidth() - left - containerWidth;
        }
        final int top = iosLayout ? 0 : dp(10);
        rect.set(left, top, left + containerWidth, top + dp(iosLayout ? 26 : 17) * alpha);
        canvas.save();
        canvas.scale(scale, scale, rect.centerX(), getContentCenterY());

        if (glass != null) {
            final float radius = rect.height() / 2f;
            if (glassRadius != radius) {
                glassRadius = radius;
                glass.setRadius(radius);
            }
            glass.setBounds((int) rect.left, (int) rect.top, (int) Math.ceil(rect.right), (int) Math.ceil(rect.bottom));
            glass.setAlpha((int) (0xFF * alpha));
            glass.draw(canvas);
        } else {
            final float bgAlpha = hasColorById ? 0.1f : 1f;
            backgroundPaint.setShadowLayer(0, 0, 0, 0);
            int wasAlpha = backgroundPaint.getAlpha();
            backgroundPaint.setAlpha((int) (wasAlpha * alpha * bgAlpha));
            canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, backgroundPaint);
            backgroundPaint.setAlpha(wasAlpha);
            if (hasColorById && buttonColor != 0) {
                if (radialGradient == null || radialGradientWidth != rect.width()) {
                    radialGradientWidth = rect.width();
                    radialGradient = new RadialGradient(
                        rect.width() / 2f, dp(iosLayout ? 26 : 17) / 2f, rect.width() * 0.65f,
                        Theme.multAlpha(buttonColor, 0.8f), buttonColor,
                        Shader.TileMode.CLAMP
                    );
                    shaderPaint.setShader(radialGradient);
                }
                matrix.setTranslate(rect.left, rect.top);
                radialGradient.setLocalMatrix(matrix);
                shaderPaint.setAlpha((int) (0xFF * alpha));
                canvas.drawRoundRect(rect, rect.height() / 2f, rect.height() / 2f, shaderPaint);
            }
        }

        if (iosLayout) {
            final float radius = rect.height() / 2f;
            iosFill.setColor(Theme.multAlpha(ProfileButtonsGlass.iosSurfaceTint(Theme.isCurrentThemeDark()), alpha));
            canvas.drawRoundRect(rect, radius, radius, iosFill);
            iosRim.draw(canvas, rect, radius, iosCoverColor, alpha);
        }
        clipPath.rewind();
        clipPath.addRoundRect(rect, rect.height() / 2f, rect.height() / 2f, Path.Direction.CW);

        canvas.save();
        canvas.clipPath(clipPath);
        final float contentInset = iosLayout ? (containerWidth - getContentWidth(containerWidth)) / 2f : getPillHorizontalPadding();
        canvas.translate(rect.left + contentInset, 0);
        drawContent(canvas, alpha);

        canvas.restore();

        canvas.restore();
    }
}
