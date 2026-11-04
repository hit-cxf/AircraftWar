package edu.hitsz.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FixedStepClockTest {
    private static final long SECOND = 1_000_000_000L;
    private static final long MILLISECOND = 1_000_000L;

    @Test
    void returningAfterAMinuteRunsAtMostFiveStepsAndLeavesNoHistoricalBacklog() {
        FixedStepClock clock = new FixedStepClock(40 * MILLISECOND, 5);
        assertEquals(0, clock.advance(0, true).steps());
        FixedStepClock.Frame returned = clock.advance(60 * SECOND, true);
        assertEquals(5, returned.steps());
        assertEquals(0.2, returned.elapsedSeconds(), 1e-9);
        assertEquals(0, clock.advance(60 * SECOND + 16 * MILLISECOND, true).steps());
        assertEquals(1, clock.advance(60 * SECOND + 40 * MILLISECOND, true).steps());
    }

    @Test
    void sixtyHertzFramesProduceExactlyTwentyFiveLogicStepsPerSecond() {
        FixedStepClock clock = new FixedStepClock(40 * MILLISECOND, 5);
        clock.advance(0, true);
        int totalSteps = 0;
        for (int frame = 1; frame <= 600; frame++) {
            FixedStepClock.Frame advanced = clock.advance(frame * SECOND / 60, true);
            assertTrue(advanced.steps() <= 1);
            totalSteps += advanced.steps();
            if (frame % 60 == 0) assertEquals(frame / 60 * 25, totalSteps);
        }
        assertEquals(250, totalSteps);
    }

    @Test
    void pauseAndResumeDiscardBothPausedTimeAndThePartialStepBeforePause() {
        FixedStepClock clock = new FixedStepClock(40 * MILLISECOND, 5);
        clock.advance(0, true);
        assertEquals(0, clock.advance(30 * MILLISECOND, true).steps());
        assertEquals(0, clock.advance(5 * SECOND, false).steps());
        assertEquals(0, clock.advance(60 * SECOND, false).steps());
        assertEquals(0, clock.advance(60 * SECOND, true).steps());
        assertEquals(0, clock.advance(60 * SECOND + 10 * MILLISECOND, true).steps());
        assertEquals(1, clock.advance(60 * SECOND + 40 * MILLISECOND, true).steps());
    }

    @Test
    void unevenNormalFramesRetainFractionalTimeInsteadOfSlowingTheGame() {
        FixedStepClock clock = new FixedStepClock(40 * MILLISECOND, 5);
        clock.advance(0, true);
        assertEquals(0, clock.advance(12 * MILLISECOND, true).steps());
        assertEquals(0, clock.advance(29 * MILLISECOND, true).steps());
        assertEquals(1, clock.advance(49 * MILLISECOND, true).steps());
        assertEquals(1, clock.advance(80 * MILLISECOND, true).steps());
    }
}
