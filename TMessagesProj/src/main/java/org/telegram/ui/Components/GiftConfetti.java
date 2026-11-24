package org.telegram.ui.Components;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;

/** Independent celebration used for incoming gifts. Positions use viewport units. */
final class GiftConfetti {
    private static final float DURATION = 3.1f;
    private static final int[] COLORS = {
            0xffff607d, 0xffffca53, 0xff68ebad, 0xff58ccff,
            0xffab8aff, 0xffff83d5, 0xffffa45e
    };

    private static final int DOT = 0, STRIP = 1, STAR = 2;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path star = new Path();
    private final Particle[] particles;
    private long startTime = -1;

    GiftConfetti() {
        final boolean lowPerformance = SharedConfig.getDevicePerformanceClass() == SharedConfig.PERFORMANCE_CLASS_LOW;
        final int sideCount = lowPerformance ? 32 : 46;
        final int rainCount = lowPerformance ? 44 : 68;
        particles = new Particle[sideCount * 2 + rainCount];
        for (int i = 0; i < particles.length; i++) {
            final int side = i < sideCount ? 1 : i < sideCount * 2 ? -1 : 0;
            particles[i] = new Particle(side);
        }
        for (int i = 0; i < 10; i++) {
            final double angle = -Math.PI / 2 + i * Math.PI / 5;
            final float radius = i % 2 == 0 ? 1f : .46f;
            final float x = (float) Math.cos(angle) * radius;
            final float y = (float) Math.sin(angle) * radius;
            if (i == 0) star.moveTo(x, y);
            else star.lineTo(x, y);
        }
        star.close();
    }

    boolean draw(Canvas canvas, int width, int height, long frameTime) {
        if (width <= 0 || height <= 0) return true;
        if (startTime < 0) startTime = frameTime;
        final float elapsed = (frameTime - startTime) / 1000f;
        if (elapsed >= DURATION) return false;
        final float opacity = Math.min(1f, (DURATION - elapsed) / .4f);
        for (Particle particle : particles) {
            final float age = elapsed - particle.delay;
            if (age < 0) continue;
            final float x = (particle.x + particle.vx * age / (1f + .56f * age)
                    + .018f * (float) Math.sin(particle.phase + 6f * age)) * width;
            final float y = (particle.y + particle.vy * age + particle.acceleration * age * age) * height;
            final float size = particle.size;
            if (x < -size || x > width + size || y < -size || y > height + size) continue;
            paint.setColor(particle.color);
            paint.setAlpha(Math.round(255 * opacity));
            if (particle.shape == DOT) {
                canvas.drawCircle(x, y, size * .52f, paint);
            } else {
                final float flip = .24f + .76f * Math.abs((float) Math.cos(particle.phase + age * 9f));
                canvas.save();
                canvas.translate(x, y);
                canvas.rotate(particle.phase * 55f + particle.spin * age);
                if (particle.shape == STAR) {
                    canvas.scale(size * flip, size);
                    canvas.drawPath(star, paint);
                } else {
                    canvas.drawRoundRect(-size * .55f * flip, -size, size * .55f * flip, size,
                            size * .22f, size * .22f, paint);
                }
                canvas.restore();
            }
        }
        return true;
    }

    private static float between(float min, float max) {
        return min + Utilities.random.nextFloat() * (max - min);
    }

    private static final class Particle {
        final int color = COLORS[Utilities.random.nextInt(COLORS.length)];
        final int shape = Utilities.random.nextInt(5) == 0 ? STAR : Utilities.random.nextInt(2);
        final float phase = between(0, 6.28f);
        final float spin = between(-520, 520);
        final float size = AndroidUtilities.dpf2(between(2.5f, 5.5f));
        final float delay, x, y, vx, vy, acceleration;

        Particle(int side) {
            if (side == 0) {
                delay = between(.02f, .82f);
                x = between(.02f, .98f);
                y = between(-.24f, -.03f);
                vx = between(-.12f, .12f);
                vy = between(.20f, .50f);
                acceleration = between(.34f, .62f);
            } else {
                delay = between(0, .18f);
                x = (side > 0 ? 0 : 1) + side * between(-.02f, .04f);
                y = between(.86f, .98f);
                vx = side * between(.84f, 1.70f);
                vy = -between(1.65f, 2.60f);
                acceleration = between(1.02f, 1.54f);
            }
        }
    }
}
