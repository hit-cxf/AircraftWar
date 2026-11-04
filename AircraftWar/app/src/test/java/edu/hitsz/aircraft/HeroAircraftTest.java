package edu.hitsz.aircraft;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeroAircraftTest {
    @Test
    void pausedScatterKeepsItsCountdownAndNewGameClearsIt() {
        HeroAircraft hero = HeroAircraft.getInstance();
        assertEquals(0, hero.getFireRemainingMillis());
        hero.enableScatterShoot();
        hero.pauseFireEffect();
        long pausedRemaining = hero.getFireRemainingMillis();
        assertTrue(pausedRemaining > 0 && pausedRemaining <= 10_000);
        hero.enableRingShoot();
        assertEquals(pausedRemaining, hero.getFireRemainingMillis());
        hero.resumeFireEffect();
        assertTrue(hero.getFireRemainingMillis() > 0);
        hero.resetForNewGame();
        assertEquals(0, hero.getFireRemainingMillis());
    }

    @BeforeEach
    void resetHero() {
        HeroAircraft.getInstance().resetForNewGame();
    }

    @AfterEach
    void clearPendingEffects() {
        HeroAircraft.getInstance().resetForNewGame();
    }

    @Test
    void ringIsOneImmediateVolleyAndKeepsStraightShooting() {
        HeroAircraft hero = HeroAircraft.getInstance();
        hero.setLocation(120, 300);
        hero.enableRingShoot();
        var ring = hero.drainSpecialBullets();
        assertEquals(20, ring.size());
        assertTrue(ring.stream().allMatch(bullet ->
                bullet.getLocationX() == 120 && bullet.getLocationY() == 300));
        assertTrue(hero.drainSpecialBullets().isEmpty());
        assertEquals(1, hero.shoot().size());
        assertEquals(1, hero.shoot().size());
    }

    @Test
    void ringLeavesTheExistingScatterStrategyInPlace() {
        HeroAircraft hero = HeroAircraft.getInstance();
        hero.enableScatterShoot();
        assertEquals(3, hero.shoot().size());
        hero.enableRingShoot();
        assertEquals(20, hero.drainSpecialBullets().size());
        assertEquals(3, hero.shoot().size());
    }

    @Test
    void eachPickupCreatesOneRingAndNewGameClearsUnconsumedRings() {
        HeroAircraft hero = HeroAircraft.getInstance();
        hero.enableRingShoot();
        hero.enableRingShoot();
        assertEquals(40, hero.drainSpecialBullets().size());
        hero.enableRingShoot();
        hero.resetForNewGame();
        assertTrue(hero.drainSpecialBullets().isEmpty());
        assertEquals(1, hero.shoot().size());
    }

    @Test
    void newGameRestoresADeadHeroWithoutCreatingAnotherInstance() {
        HeroAircraft hero = HeroAircraft.getInstance();
        int maximum = hero.getHp();
        hero.decreaseHp(Integer.MAX_VALUE);
        org.junit.jupiter.api.Assertions.assertTrue(hero.notValid());
        hero.resetForNewGame();
        assertSame(hero, HeroAircraft.getInstance());
        assertEquals(maximum, hero.getHp());
        org.junit.jupiter.api.Assertions.assertFalse(hero.notValid());
    }

    @Test
    void getInstanceReturnsTheSameHero() {
        assertSame(HeroAircraft.getInstance(), HeroAircraft.getInstance());
    }

    @Test
    void inheritedSetLocationUpdatesCoordinates() {
        HeroAircraft hero = HeroAircraft.getInstance();
        int oldX = hero.getLocationX();
        int oldY = hero.getLocationY();
        try {
            hero.setLocation(120.8, 340.6);
            assertEquals(120, hero.getLocationX());
            assertEquals(340, hero.getLocationY());
        } finally {
            hero.setLocation(oldX, oldY);
        }
    }

    @Test
    void damageAndHealingRestoreHpWithoutExceedingMaximum() {
        HeroAircraft hero = HeroAircraft.getInstance();
        int initialHp = hero.getHp();
        try {
            hero.decreaseHp(100);
            assertEquals(initialHp - 100, hero.getHp());

            hero.increaseHp(40);
            assertEquals(initialHp - 60, hero.getHp());

            hero.increaseHp(1000);
            assertEquals(initialHp, hero.getHp());

            hero.increaseHp(-10);
            assertEquals(initialHp, hero.getHp());
        } finally {
            hero.increaseHp(initialHp);
        }
    }
}
