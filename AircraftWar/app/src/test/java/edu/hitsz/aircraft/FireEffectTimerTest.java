package edu.hitsz.aircraft;

import org.junit.jupiter.api.Test;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class FireEffectTimerTest {
    @Test
    void pausedCountdownKeepsItsRemainingTimeAndResumeContinuesIt() {
        var clock = new AtomicLong();
        var queue = new LinkedBlockingQueue<Runnable>();
        var timer = new FireEffectTimer(queue::add, clock::get);
        try {
            timer.start(5_000, () -> {});
            clock.set(2_000_000_000L);
            assertEquals(3_000, timer.remainingMillis());
            timer.pause();
            clock.set(52_000_000_000L);
            assertEquals(3_000, timer.remainingMillis());
            timer.resume();
            clock.set(53_000_000_000L);
            assertEquals(2_000, timer.remainingMillis());
            timer.cancel();
            assertEquals(0, timer.remainingMillis());
        } finally {
            timer.cancel();
        }
    }

    @Test
    void pauseInvalidatesAQueuedRecoveryAndResumeQueuesANewOne() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        var restored = new AtomicInteger();
        var timer = new FireEffectTimer(queue::add);
        try {
            timer.start(0, restored::incrementAndGet);
            Runnable stale = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(stale);
            timer.pause();
            stale.run();
            assertEquals(0, restored.get());
            timer.resume();
            Runnable resumed = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(resumed);
            resumed.run();
            assertEquals(1, restored.get());
        } finally {
            timer.cancel();
        }
    }

    @Test
    void pickupWhilePausedReplacesOldRecoveryButWaitsForResume() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        var oldEffect = new AtomicInteger();
        var newEffect = new AtomicInteger();
        var timer = new FireEffectTimer(queue::add);
        try {
            timer.start(0, oldEffect::incrementAndGet);
            Runnable stale = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(stale);
            timer.pause();
            timer.start(0, newEffect::incrementAndGet);
            assertTrue(queue.isEmpty(), "暂停时不能投递新恢复任务");
            stale.run();
            assertEquals(0, oldEffect.get());
            timer.resume();
            Runnable resumed = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(resumed);
            resumed.run();
            assertEquals(1, newEffect.get());
            assertEquals(0, oldEffect.get());
        } finally {
            timer.cancel();
        }
    }

    @Test
    void cancelWhilePausedDoesNotRearmTheOldEffectOnResume() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        var restored = new AtomicInteger();
        var timer = new FireEffectTimer(queue::add);
        timer.start(0, restored::incrementAndGet);
        Runnable stale = queue.poll(2, TimeUnit.SECONDS);
        assertNotNull(stale);
        timer.pause();
        timer.cancel();
        timer.resume();
        stale.run();
        assertEquals(0, restored.get());
        assertEquals(0, timer.remainingMillis());
        assertTrue(queue.isEmpty());
    }

    @Test
    void newPickupInvalidatesEvenAnAlreadyQueuedRecovery() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        var timer = new FireEffectTimer(queue::add);
        var restored = new AtomicInteger();
        try {
            timer.start(0, restored::incrementAndGet);
            Runnable oldRecovery = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(oldRecovery);
            timer.start(0, restored::incrementAndGet);
            Runnable currentRecovery = queue.poll(2, TimeUnit.SECONDS);
            assertNotNull(currentRecovery);
            oldRecovery.run();
            assertEquals(0, restored.get());
            currentRecovery.run();
            assertEquals(1, restored.get());
        } finally {
            timer.cancel();
        }
    }

    @Test
    void endingGameInvalidatesPendingRecovery() throws Exception {
        var queue = new LinkedBlockingQueue<Runnable>();
        var timer = new FireEffectTimer(queue::add);
        var restored = new AtomicInteger();
        timer.start(0, restored::incrementAndGet);
        Runnable recovery = queue.poll(2, TimeUnit.SECONDS);
        assertNotNull(recovery);
        timer.cancel();
        recovery.run();
        assertEquals(0, restored.get());
    }
}
