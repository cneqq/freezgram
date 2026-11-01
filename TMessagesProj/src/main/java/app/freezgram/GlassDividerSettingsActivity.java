package app.freezgram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Build;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable;
import app.exteraless.appearance.AppearanceConfig;
import app.exteraless.appearance.AvatarCornersSeekBar;

public final class GlassDividerSettingsActivity extends BaseFragment {
    private boolean changed;
    @Override public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle(LocaleController.getString(R.string.FreezgramGlassMenuDividers));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        ScrollView scroll = StyleEditorUi.scroll(context, resourceProvider);
        LinearLayout content = StyleEditorUi.content(scroll);
        Preview preview = new Preview(context);
        StyleEditorUi.preview(content, preview, 200, resourceProvider, false);
        LinearLayout settings = StyleEditorUi.section(content, R.string.FreezgramStyleAppearance, resourceProvider);
        TextCheckCell enabled = new TextCheckCell(context);
        enabled.setTextAndCheck(LocaleController.getString(R.string.FreezgramGlassMenuDividers), AppearanceConfig.glassMenuDividers(), false);
        enabled.setOnClickListener(v -> {
            AppearanceConfig.glassMenuDividers.setConfigBool(!AppearanceConfig.glassMenuDividers());
            enabled.setChecked(AppearanceConfig.glassMenuDividers()); changed = true; preview.invalidate();
        });
        settings.addView(enabled);
        enabled.setBackground(Theme.getSelectorDrawable(false));
        slider(context, settings, R.string.FreezgramGlassDividerWeight, 1, 4, AppearanceConfig.glassDividerWeight(), "", value -> {
            AppearanceConfig.glassDividerWeight.setConfigInt(value); changed = true; preview.invalidate();
        });
        slider(context, settings, R.string.FreezgramGlassDividerOpacity, 0, 100, AppearanceConfig.glassDividerOpacity(), "%", value -> {
            AppearanceConfig.glassDividerOpacity.setConfigInt(value); changed = true; preview.invalidate();
        });
        slider(context, settings, R.string.FreezgramGlassDividerInset, 0, 32, AppearanceConfig.glassDividerInset(), "dp", value -> {
            AppearanceConfig.glassDividerInset.setConfigInt(value); changed = true; preview.invalidate();
        });
        fragmentView = scroll;
        return scroll;
    }
    private void slider(Context context, LinearLayout content, int title, int min, int max, int value, String suffix, AvatarCornersSeekBar.OnDrag drag) {
        AvatarCornersSeekBar slider = new AvatarCornersSeekBar(context, drag, min, max, LocaleController.getString(title), String.valueOf(min), String.valueOf(max), true);
        slider.setValueSuffix(suffix); slider.setValue(value);
        content.addView(slider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 84));
    }
    @Override public void onFragmentDestroy() {
        android.app.Activity activity = getParentActivity();
        super.onFragmentDestroy();
        if (changed && activity instanceof org.telegram.ui.LaunchActivity) {
            AndroidUtilities.runOnUIThread(() -> ((org.telegram.ui.LaunchActivity) activity).rebuildAllFragments(false));
        }
    }
    private static int dp(float n) { return AndroidUtilities.dp(n); }
    private final class Preview extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private BlurredBackgroundSourceRenderNode source;
        private BlurredBackgroundDrawable glass;
        Preview(Context context) {
            super(context);
            if (Build.VERSION.SDK_INT >= 33) {
                source = new BlurredBackgroundSourceRenderNode(null);
                glass = source.createDrawable();
                glass.setColorProvider(LiquidGlassStyle.surfaceColors(resourceProvider, this::enabled));
                glass.setRadius(dp(22));
            }
        }
        private boolean enabled() { return Build.VERSION.SDK_INT >= 33 && SharedConfig.chatBlurEnabled() && !LiteMode.isGlassPowerSaverApplied(); }
        private void backdrop(Canvas canvas) {
            canvas.drawColor(getThemedColor(Theme.key_windowBackgroundGray));
            paint.setColor(0xff448ac4); canvas.drawCircle(dp(65), dp(95), dp(64), paint);
            paint.setColor(0xff8058b0); canvas.drawCircle(getWidth() - dp(65), dp(120), dp(55), paint);
        }
        @Override protected void onDraw(Canvas canvas) {
            backdrop(canvas);
            if (source != null && canvas.isHardwareAccelerated()) {
                Canvas recording = source.beginRecording(getWidth(), getHeight()); backdrop(recording); source.endRecording();
                source.setBlur(AndroidUtilities.dpf2(LiquidGlassStyle.blurRadiusDp()));
                ((org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawableRenderNode) glass).setLiquidGlassEffectAllowed(enabled());
                LiquidGlassStyle.configure(glass); glass.updateColors();
                glass.setBounds(dp(12), dp(12), getWidth() - dp(12), getHeight() - dp(12)); glass.draw(canvas);
            } else {
                paint.setColor(getThemedColor(Theme.key_windowBackgroundWhite));
                canvas.drawRoundRect(dp(12), dp(12), getWidth() - dp(12), getHeight() - dp(12), dp(22), dp(22), paint);
            }
            int color = getThemedColor(Theme.key_actionBarDefaultSubmenuItem);
            paint.setColor(color); paint.setTextSize(dp(16));
            int[] labels = {R.string.AddToContacts, R.string.Copy, R.string.Close};
            for (int i = 0; i < labels.length; i++) {
                float y = dp(40 + i * 56);
                canvas.drawText(LocaleController.getString(labels[i]), dp(32), y - (paint.ascent() + paint.descent()) / 2, paint);
                if (i > 0) GlassDividers.draw(canvas, dp(12), getWidth() - dp(12), y - dp(28), 1, color);
            }
        }
    }
}
