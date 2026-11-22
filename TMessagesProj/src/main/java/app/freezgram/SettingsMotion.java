package app.freezgram;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.os.Build;
import android.view.View;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SharedConfig;
import app.exteraless.appearance.AppearanceConfig;
import java.util.function.Consumer;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.CubicBezierInterpolator;

/** Shared screen transitions. Native preview and specialized fragment transitions stay with their owners. */
public final class SettingsMotion {
    private SettingsMotion() {}

    public static boolean appliesTo(BaseFragment fragment) {
        String name = fragment.getClass().getName();
        return name.startsWith("app.exteraless.settings.") || name.startsWith("app.freezgram.");
    }

    public static AnimatorSet transition(View container, boolean opening, boolean fastChatOpen,
                                         Runnable completion, Consumer<Float> progress) {
        if (AppearanceConfig.transitionStyle() != 1 || !SharedConfig.animationsEnabled()
                || Build.VERSION.SDK_INT >= 26 && !ValueAnimator.areAnimatorsEnabled() || container == null) return null;
        float travel = AndroidUtilities.dp(28);
        container.setAlpha(opening ? 0f : 1f);
        container.setTranslationX(opening ? travel : 0f);
        ValueAnimator frames = ValueAnimator.ofFloat(0f, 1f);
        frames.addUpdateListener(frame -> {
            float value = (float) frame.getAnimatedValue();
            container.setAlpha(opening ? value : 1f - value);
            container.setTranslationX(travel * (opening ? 1f - value : value));
            progress.accept(value);
        });
        AnimatorSet animation = new AnimatorSet();
        animation.playTogether(frames);
        animation.setDuration(fastChatOpen ? 210 : opening ? 320 : 280);
        animation.setInterpolator(CubicBezierInterpolator.EASE_BOTH);
        animation.addListener(new AnimatorListenerAdapter() {
            private boolean finished;
            private void finish() {
                if (finished) return;
                finished = true;
                container.setAlpha(1f);
                container.setTranslationX(0f);
                completion.run();
            }
            @Override public void onAnimationCancel(Animator animator) { finish(); }
            @Override public void onAnimationEnd(Animator animator) { finish(); }
        });
        animation.start();
        return animation;
    }
}
