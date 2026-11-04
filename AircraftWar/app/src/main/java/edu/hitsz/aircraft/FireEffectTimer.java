package edu.hitsz.aircraft;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/** Runnable 等待计时，恢复动作交给调用方指定的线程执行。支持暂停与取消。 */
public final class FireEffectTimer {
    private final Consumer<Runnable> dispatcher;
    private final LongSupplier clock;
    private long version;
    private long startedAt;
    private long remainingNanos;
    private boolean active;
    private boolean paused;
    private Runnable recovery;
    private Thread worker;

    public FireEffectTimer(Consumer<Runnable> dispatcher) {
        this(dispatcher, System::nanoTime);
    }

    /** 可注入单调时钟，便于无需真实等待即可验证暂停时限。 */
    public FireEffectTimer(Consumer<Runnable> dispatcher, LongSupplier clock) {
        this.dispatcher = Objects.requireNonNull(dispatcher);
        this.clock = Objects.requireNonNull(clock);
    }

    public synchronized void start(long durationMillis, Runnable recovery) {
        if (durationMillis < 0) {
            throw new IllegalArgumentException("火力持续时间不能为负数");
        }
        Objects.requireNonNull(recovery);
        invalidateWorker();
        this.remainingNanos = TimeUnit.MILLISECONDS.toNanos(durationMillis);
        this.recovery = recovery;
        active = true;
        // 暂停期间重新拾取时仍保持暂停，恢复后享有完整的新时限。
        if (!paused) {
            launchWorker();
        }
    }

    public synchronized void pause() {
        if (paused) {
            return;
        }
        if (active) {
            remainingNanos = currentRemainingNanos();
        }
        paused = true;
        // 同时作废已经投递到界面线程、但尚未执行的恢复任务。
        invalidateWorker();
    }

    public synchronized void resume() {
        if (!paused) {
            return;
        }
        paused = false;
        if (active) {
            launchWorker();
        }
    }

    public synchronized long remainingMillis() {
        long nanos = currentRemainingNanos();
        return nanos == 0 ? 0 : 1 + (nanos - 1) / 1_000_000;
    }

    public synchronized void cancel() {
        invalidateWorker();
        active = false;
        paused = false;
        remainingNanos = 0;
        recovery = null;
    }

    private long currentRemainingNanos() {
        if (!active) {
            return 0;
        }
        if (paused) {
            return remainingNanos;
        }
        long elapsed = Math.max(0, clock.getAsLong() - startedAt);
        return Math.max(0, remainingNanos - elapsed);
    }

    /** 调用方持有 this 锁。每次启动都获得新的回调版本。 */
    private void launchWorker() {
        long currentVersion = ++version;
        startedAt = clock.getAsLong();
        long waitNanos = remainingNanos;
        Runnable countdown = () -> {
            try {
                TimeUnit.NANOSECONDS.sleep(waitNanos);
                dispatcher.accept(() -> finish(currentVersion));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        };
        worker = new Thread(countdown, "fire-effect-timer");
        worker.setDaemon(true);
        worker.start();
    }

    private synchronized void finish(long expectedVersion) {
        if (!active || paused || version != expectedVersion) {
            return;
        }
        long remaining = currentRemainingNanos();
        if (remaining > 0) {
            // 时钟或调度器提前唤醒时继续等待，不能缩短效果。
            remainingNanos = remaining;
            launchWorker();
            return;
        }
        active = false;
        remainingNanos = 0;
        worker = null;
        Runnable action = recovery;
        recovery = null;
        // 在锁内检查并执行，防止校验后、执行前另一线程发出暂停。
        action.run();
    }

    private void invalidateWorker() {
        version++;
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }
}
