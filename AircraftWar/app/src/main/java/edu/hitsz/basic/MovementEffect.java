package edu.hitsz.basic;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** 临时移动效果；重复触发时刷新时限，不修改对象的原始速度。 */
public final class MovementEffect {
    private final LongSupplier clock;
    private long startedAt;
    private long durationNanos;
    private double scale = 1.0;
    private boolean permanent;
    private boolean paused;
    private long pausedAt;

    public MovementEffect() {
        this(System::nanoTime);
    }

    public MovementEffect(LongSupplier clock) {
        this.clock = clock;
    }

    public void freeze(long durationMillis) {
        slow(durationMillis, 0.0);
    }

    public void freezePermanently() {
        permanent = true;
        scale = 0.0;
    }

    public void slow(long durationMillis, double scale) {
        if (durationMillis <= 0 || scale < 0 || scale >= 1 || Double.isNaN(scale)) {
            throw new IllegalArgumentException("移动效果的持续时间须为正数，倍率须在 [0, 1) 内");
        }
        if (permanent) {
            return;
        }
        this.startedAt = effectiveNow();
        this.durationNanos = TimeUnit.MILLISECONDS.toNanos(durationMillis);
        this.scale = scale;
    }

    /** 暂停的只是效果时钟，游戏主循环负责停止对象移动。 */
    public void pause() {
        if (!paused) {
            pausedAt = clock.getAsLong();
            paused = true;
        }
    }

    public void resume() {
        if (paused) {
            startedAt += clock.getAsLong() - pausedAt;
            paused = false;
        }
    }

    public long remainingMillis() {
        if (scale() == 1.0) {
            return 0;
        }
        if (permanent) {
            return Long.MAX_VALUE;
        }
        long nanos = Math.max(0, durationNanos - (effectiveNow() - startedAt));
        return nanos == 0 ? 0 : 1 + (nanos - 1) / 1_000_000;
    }

    private long effectiveNow() {
        return paused ? pausedAt : clock.getAsLong();
    }

    public double scale() {
        if (!permanent && scale != 1.0 && effectiveNow() - startedAt >= durationNanos) {
            scale = 1.0;
        }
        return scale;
    }
}
