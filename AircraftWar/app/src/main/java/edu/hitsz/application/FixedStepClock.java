package edu.hitsz.application;

/** 固定逻辑步长；长时间阻塞后丢弃超额时间，避免追算历史帧挤占渲染。 */
final class FixedStepClock {
    record Frame(int steps, double elapsedSeconds) { }

    private static final Frame EMPTY = new Frame(0, 0.0);
    private final long stepNanos;
    private final long maximumFrameNanos;
    private long previousNanos;
    private long remainderNanos;
    private boolean initialized;

    FixedStepClock(long stepNanos, int maximumSteps) {
        if (stepNanos <= 0 || maximumSteps <= 0) {
            throw new IllegalArgumentException("逻辑步长与每帧最大步数必须为正数");
        }
        this.stepNanos = stepNanos;
        this.maximumFrameNanos = Math.multiplyExact(stepNanos, maximumSteps);
    }

    Frame advance(long nowNanos, boolean running) {
        if (!running) {
            reset();
            return EMPTY;
        }
        if (!initialized) {
            previousNanos = nowNanos;
            initialized = true;
            return EMPTY;
        }
        long elapsed = Math.min(maximumFrameNanos, Math.max(0, nowNanos - previousNanos));
        previousNanos = nowNanos;
        // 丢弃超额整步，同时保留正常刷新率下不足一个逻辑步的余数。
        long accepted = elapsed + remainderNanos;
        int steps = (int) (accepted / stepNanos);
        remainderNanos = accepted % stepNanos;
        return new Frame(steps, elapsed / 1_000_000_000.0);
    }

    void reset() {
        previousNanos = 0;
        remainderNanos = 0;
        initialized = false;
    }
}
