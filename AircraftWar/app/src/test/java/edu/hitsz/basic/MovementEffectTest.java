package edu.hitsz.basic;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class MovementEffectTest {
    @Test
    void freezeAndSlowingTimersDoNotExpireWhilePaused() {
        AtomicLong clock = new AtomicLong();
        MovementEffect effect = new MovementEffect(clock::get);
        effect.freeze(4_000);
        clock.set(1_000_000_000L);
        effect.pause();
        assertEquals(3_000, effect.remainingMillis());
        clock.set(100_000_000_000L);
        assertEquals(0.0, effect.scale());
        assertEquals(3_000, effect.remainingMillis());
        effect.resume();
        clock.set(102_000_000_000L);
        assertEquals(1_000, effect.remainingMillis());
        clock.set(103_000_000_000L);
        assertEquals(1.0, effect.scale());
        assertEquals(0, effect.remainingMillis());

        effect.slow(5_000, 0.5);
        effect.pause();
        clock.set(200_000_000_000L);
        assertEquals(0.5, effect.scale());
        assertEquals(5_000, effect.remainingMillis());
        effect.resume();
        clock.set(205_000_000_000L);
        assertEquals(1.0, effect.scale());
    }

    @Test
    void applyingAnotherEffectDuringPauseRefreshesItsFullDuration() {
        AtomicLong clock = new AtomicLong();
        MovementEffect effect = new MovementEffect(clock::get);
        effect.freeze(3_000);
        clock.set(1_000_000_000L);
        effect.pause();
        clock.set(90_000_000_000L);
        effect.freeze(5_000);
        assertEquals(5_000, effect.remainingMillis());
        effect.resume();
        clock.set(94_000_000_000L);
        assertEquals(1_000, effect.remainingMillis());
        clock.set(95_000_000_000L);
        assertEquals(0, effect.remainingMillis());
    }

    @Test
    void permanentFreezeReportsAnUnlimitedRemainingDuration() {
        AtomicLong clock = new AtomicLong();
        MovementEffect effect = new MovementEffect(clock::get);
        assertEquals(0, effect.remainingMillis());
        effect.freezePermanently();
        effect.pause();
        clock.set(100_000_000_000L);
        effect.resume();
        assertEquals(Long.MAX_VALUE, effect.remainingMillis());
        assertEquals(0.0, effect.scale());
    }

    @Test
    void permanentFreezeStaysVisibleAndCannotBeReplacedByTimedSlowing() {
        AtomicLong clock = new AtomicLong();
        MovementEffect effect = new MovementEffect(clock::get);
        effect.freezePermanently();
        clock.set(60_000_000_000L);
        effect.slow(5_000, 0.5);
        assertEquals(0.0, effect.scale());
        var mob = new edu.hitsz.aircraft.MobEnemy(200, 100, 0, 10, 30);
        mob.onFreeze();
        assertTrue(mob.hasMovementEffect(), "永久冻结也需要保留冰霜叠层状态");
    }

    @Test
    void freezesForTheRequestedDurationAndRefreshesOnAnotherPickup() {
        AtomicLong clock = new AtomicLong();
        MovementEffect effect = new MovementEffect(clock::get);
        effect.freeze(4_000);
        clock.set(3_000_000_000L);
        assertEquals(0.0, effect.scale());
        effect.freeze(4_000);
        clock.set(4_000_000_000L);
        assertEquals(0.0, effect.scale(), "第一次冻结到期不能取消第二次冻结");
        clock.set(6_999_999_999L);
        assertEquals(0.0, effect.scale());
        clock.set(7_000_000_000L);
        assertEquals(1.0, effect.scale());
    }

    @Test
    void repeatedSlowingDoesNotStackAndExpiresAtFiveSeconds() {
        AtomicLong clock = new AtomicLong();
        MovementEffect effect = new MovementEffect(clock::get);
        effect.slow(5_000, 0.5);
        clock.set(1_000_000_000L);
        effect.slow(5_000, 0.5);
        assertEquals(0.5, effect.scale());
        clock.set(6_000_000_000L);
        assertEquals(1.0, effect.scale());
    }

    @Test
    void subpixelMovementKeepsSlowObjectsMovingInBothDirections() {
        Probe object = new Probe();
        object.movementEffect.slow(5_000, 0.5);
        object.forward();
        object.forward();
        assertEquals(101, object.getLocationX());
        assertEquals(99, object.getLocationY());
    }

    private static class Probe extends AbstractFlyingObject {
        Probe() {
            super(100, 100, 1, -1);
        }
    }
}
