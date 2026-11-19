package org.telegram.ui.Components.chat;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Paint;
import androidx.core.graphics.ColorUtils;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

import org.telegram.messenger.Utilities;
import org.telegram.ui.ChatBackgroundDrawable;
import org.telegram.ui.Components.MotionBackgroundDrawable;
import org.telegram.utils.glass.source.GlassSource;
import org.telegram.utils.glass.source.GlassSourceBitmap;
import org.telegram.utils.glass.source.GlassSourceColor;
import org.telegram.utils.glass.source.GlassSourceWrapped;
import org.telegram.ui.Components.blur3.utils.BitmapMemoizedMetadata;

public class WallpaperBitmapProvider {

    private final GlassSourceColor sourceColor = new GlassSourceColor();
    private final GlassSourceBitmap sourceBitmap = new GlassSourceBitmap();

    private static final Rect tmpRect = new Rect();

    public GlassSource updateSourceFromBackgroundViewDrawable(
        Drawable drawable
    ) {
        if (drawable instanceof ColorDrawable) {
            final int color = ((ColorDrawable) drawable).getColor();
            sourceColor.setColor(color);
            return sourceColor;
        }

        if (drawable instanceof MotionBackgroundDrawable) {
            final MotionBackgroundDrawable motionDrawable = (MotionBackgroundDrawable) drawable;
            if (motionDrawable.getIntensity() < 0) {
                sourceColor.setColor(Color.BLACK);
                return sourceColor;
            }
            sourceBitmap.setBitmap(motionDrawable.getBitmap());
            return sourceBitmap;
        }

        if (drawable instanceof BitmapDrawable) {
            final Bitmap bitmap = blurredFromBitmap.get(((BitmapDrawable) drawable).getBitmap());
            sourceBitmap.setBitmap(bitmap);
            return sourceBitmap;
        }

        if (drawable instanceof ChatBackgroundDrawable) {
            ChatBackgroundDrawable chatDrawable = (ChatBackgroundDrawable) drawable;
            GlassSource source = updateSourceFromBackgroundViewDrawable(chatDrawable.getDrawable(false));
            float dim = Math.max(0, Math.min(1, chatDrawable.getDimAmount()));
            return dim > 0 ? new DimmedWallpaperSource(source, dim) : source;
        }

        if (drawable != null) {
            Canvas canvas = sourceBitmap.beginRecording(120, 160);
            tmpRect.set(drawable.getBounds());
            drawable.setBounds(0, 0, 120, 160);
            drawable.draw(canvas);
            drawable.setBounds(tmpRect);
            sourceBitmap.endRecording();
            sourceBitmap.setBitmap(blurredFromBitmap.get(sourceBitmap.getBitmap()));
        }

        return sourceBitmap;
    }

    public GlassSource withRenderedWallpaper(GlassSource source,
            java.util.function.Predicate<Canvas> renderWallpaper) {
        GlassSourceWrapped wrapped = new GlassSourceWrapped() {
            @Override public void draw(Canvas canvas, float left, float top, float right, float bottom) {
                canvas.save();
                try {
                    canvas.clipRect(left, top, right, bottom);
                    if (!app.exteraless.appearance.AppearanceConfig.glassWallpaperDim() || !renderWallpaper.test(canvas)) {
                        super.draw(canvas, left, top, right, bottom);
                    }
                } finally {
                    canvas.restore();
                }
            }
        };
        wrapped.setSource(source);
        return wrapped;
    }

    public int getNavigationBarColor(GlassSource source) {
        if (source instanceof DimmedWallpaperSource) {
            DimmedWallpaperSource dimmed = (DimmedWallpaperSource) source;
            return ColorUtils.compositeColors(dimmed.getDimColor(), getNavigationBarColor(dimmed.getSource()));
        }

        if (source instanceof GlassSourceColor) {
            return ((GlassSourceColor) source).getColor();
        }

        if (source instanceof GlassSourceBitmap) {
            final Bitmap bitmap = ((GlassSourceBitmap) source).getBitmap();
            return navbarColorFromBitmap.get(bitmap);
        }

        if (source instanceof GlassSourceWrapped) {
            return getNavigationBarColor(((GlassSourceWrapped) source).getSource());
        }

        return 0;
    }

    public int getStatusBarColor(GlassSource source) {
        if (source instanceof DimmedWallpaperSource) {
            DimmedWallpaperSource dimmed = (DimmedWallpaperSource) source;
            return ColorUtils.compositeColors(dimmed.getDimColor(), getStatusBarColor(dimmed.getSource()));
        }

        if (source instanceof GlassSourceColor) {
            return ((GlassSourceColor) source).getColor();
        }

        if (source instanceof GlassSourceBitmap) {
            final Bitmap bitmap = ((GlassSourceBitmap) source).getBitmap();
            return statusBarColorFromBitmap.get(bitmap);
        }

        if (source instanceof GlassSourceWrapped) {
            return getStatusBarColor(((GlassSourceWrapped) source).getSource());
        }

        return 0;
    }

    /** Apply wallpaper dim before glass captures/composites messages, without mutating bitmaps. */
    private static final class DimmedWallpaperSource extends GlassSourceWrapped {
        private final Paint paint = new Paint();
        private final float dim;

        DimmedWallpaperSource(GlassSource source, float dim) {
            setSource(source);
            this.dim = Math.max(0, Math.min(1, dim));
            getDimColor();
        }

        int getDimColor() {
            paint.setColor(Color.BLACK);
            paint.setAlpha(app.exteraless.appearance.AppearanceConfig.glassWallpaperDim() ? (int) (255 * dim) : 0);
            return paint.getColor();
        }

        @Override
        public void draw(Canvas canvas, float left, float top, float right, float bottom) {
            super.draw(canvas, left, top, right, bottom);
            getDimColor();
            canvas.drawRect(left, top, right, bottom, paint);
        }
    }

    private final BitmapMemoizedMetadata<Bitmap> blurredFromBitmap = new BitmapMemoizedMetadata<>(WallpaperBitmapProvider::blurBitmap);
    private final BitmapMemoizedMetadata<Integer> navbarColorFromBitmap = new BitmapMemoizedMetadata<>(WallpaperBitmapProvider::averageBottomColor);
    private final BitmapMemoizedMetadata<Integer> statusBarColorFromBitmap = new BitmapMemoizedMetadata<>(WallpaperBitmapProvider::averageTopColor);


    private static Bitmap blurBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }

        final float scale = Math.max(bitmap.getWidth() / 90f, bitmap.getHeight() / 120f);
        final Bitmap result = Utilities.stackBlurBitmapWithScaleFactor(bitmap, scale);
        result.setHasAlpha(false);
        return result;
    }



    private static int averageTopColor(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return 0;
        }

        final int height = bitmap.getHeight();
        final int width = bitmap.getWidth();
        final int bottom = height / 10;
        return Utilities.averageBitmapColor(bitmap, 0, 0, width, bottom);
    }

    private static int averageBottomColor(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return 0;
        }

        final int height = bitmap.getHeight();
        final int width = bitmap.getWidth();
        final int top = height * 9 / 10;
        return Utilities.averageBitmapColor(bitmap, 0, top, width, height);
    }

}
