package edu.hitsz.config;

import edu.hitsz.application.Difficulty;
import edu.hitsz.application.GameParameters;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Map;

import static edu.hitsz.config.ConfigKey.*;
import static org.junit.jupiter.api.Assertions.*;

class GameConfigTest {
    @Test
    void bundledDefaultsContainEveryFieldAndMetadataKeysAreUnique() {
        GameConfig defaults = GameConfigStore.defaults();
        assertEquals(ConfigKey.values().length, defaults.values().size());
        for (ConfigKey key : ConfigKey.values()) {
            assertEquals(key, ConfigKey.fromKey(key.key()));
            assertFalse(key.label().isBlank());
            assertFalse(key.section().isBlank());
            assertTrue(key.step() > 0);
        }
    }

    @Test
    void standardDefaultsPreservePreviouslyPlayableDifficultyProgression() {
        GameConfig config = GameConfigStore.defaults();
        assertEquals(new GameParameters(3, .15, .05, .02, 10, 30, 30, 6, .7, 0),
                config.parameters(Difficulty.EASY, 20, 5000));
        assertEquals(new GameParameters(5, .2, .1, .05, 12, 24, 12, 14, 1.9, 100),
                config.parameters(Difficulty.NORMAL, 6, 500));
        GameParameters hard = config.parameters(Difficulty.HARD, 20, 900);
        assertEquals(18, hard.heroShootCycle());
        assertEquals(6, hard.enemyShootCycle());
        assertEquals(6, hard.spawnCycle());
        assertEquals(24, hard.enemySpeed());
        assertEquals(5.4, hard.hpMultiplier(), 1e-10);
        assertEquals(0, config.bossHp(Difficulty.EASY, 1));
        assertEquals(300, config.bossHp(Difficulty.NORMAL, 3));
        assertEquals(750, config.bossHp(Difficulty.HARD, 3));
    }

    @Test
    void customSpeedUsesScoreWithoutAddingStageGrowthAndKeepsFractionalValues() {
        EnumMap<ConfigKey, Double> values = editable();
        values.put(CUSTOM_BASE_SPEED, 4.5);
        values.put(CUSTOM_SPEED_RATE, .015);
        GameConfig config = GameConfig.from(values);
        Difficulty custom = Difficulty.valueOf("CUSTOM");
        assertEquals(6.045, config.parameters(custom, 500, 103).enemySpeed(), 1e-10);
        assertEquals(config.value(CUSTOM_SPEED_MAX),
                config.parameters(custom, Integer.MAX_VALUE, Integer.MAX_VALUE).enemySpeed());
        assertEquals(4.5, config.parameters(custom, -1, -1).enemySpeed());
    }

    @Test
    void standardModesCanAlsoOptIntoScoreBasedSpeed() {
        EnumMap<ConfigKey, Double> values = editable();
        values.put(NORMAL_SPEED_RATE, .01);
        assertEquals(12.5, GameConfig.from(values).parameters(Difficulty.NORMAL, 3, 150).enemySpeed());
    }

    @Test
    void millisecondPeriodsRoundUpToWholeFortyMillisecondLogicSteps() {
        EnumMap<ConfigKey, Double> values = editable();
        values.put(CUSTOM_HERO_SHOOT_MS, 41.0);
        values.put(CUSTOM_HERO_SHOOT_MAX_MS, 41.0);
        values.put(CUSTOM_ENEMY_SHOOT_MS, 81.0);
        values.put(CUSTOM_ENEMY_SHOOT_MIN_MS, 40.0);
        values.put(CUSTOM_SPAWN_MS, 40.0);
        values.put(CUSTOM_SPAWN_MIN_MS, 40.0);
        GameParameters result = GameConfig.from(values).parameters(Difficulty.valueOf("CUSTOM"), 0, 0);
        assertEquals(2, result.heroShootCycle());
        assertEquals(3, result.enemyShootCycle());
        assertEquals(1, result.spawnCycle());
    }

    @Test
    void configRejectsMissingNonFiniteOutOfBoundsAndFractionalIntegerValues() {
        assertThrows(IllegalArgumentException.class, () -> GameConfig.from(Map.of()));
        for (double invalid : new double[]{Double.NaN, Double.POSITIVE_INFINITY, 0, 10001, 1.5}) {
            EnumMap<ConfigKey, Double> values = editable();
            values.put(HERO_HP, invalid);
            assertThrows(IllegalArgumentException.class, () -> GameConfig.from(values));
        }
        EnumMap<ConfigKey, Double> invalidSlow = editable();
        invalidSlow.put(PRO_SLOW_SCALE, 1.0);
        assertThrows(IllegalArgumentException.class, () -> GameConfig.from(invalidSlow));
    }

    @Test
    void configRejectsInvalidProbabilitiesAndContradictoryLimits() {
        assertInvalid(CUSTOM_ELITE_CHANCE, .9, CUSTOM_PRO_CHANCE, .2);
        assertInvalid(CUSTOM_SPAWN_MIN_MS, 1000, CUSTOM_SPAWN_MS, 800);
        assertInvalid(CUSTOM_HERO_SHOOT_MAX_MS, 200, CUSTOM_HERO_SHOOT_MS, 400);
        assertInvalid(CUSTOM_ENEMY_SHOOT_MIN_MS, 1000, CUSTOM_ENEMY_SHOOT_MS, 800);
        assertInvalid(CUSTOM_SPEED_MAX, 4, CUSTOM_BASE_SPEED, 6);
        assertInvalid(CUSTOM_SPAWN_STEP_MS, 20, CUSTOM_SPAWN_MS, 800);
        assertInvalid(FIRE_DURATION_MS, 0, CUSTOM_SPAWN_MS, 800);
    }

    @Test
    void immutableSnapshotDoesNotTrackSourceMutationsAndBoundsLargeProgression() {
        EnumMap<ConfigKey, Double> values = editable();
        GameConfig config = GameConfig.from(values);
        values.put(HERO_HP, 2000.0);
        assertEquals(1000, config.integer(HERO_HP));
        assertThrows(UnsupportedOperationException.class, () -> config.values().put(HERO_HP, 2000.0));
        GameParameters progressed = config.parameters(Difficulty.HARD, Integer.MAX_VALUE, Integer.MAX_VALUE);
        assertTrue(Double.isFinite(progressed.hpMultiplier()));
        assertEquals(HARD_HP_MULTIPLIER.max(), progressed.hpMultiplier());
        assertEquals(HARD_BOSS_HP.max(), config.bossHp(Difficulty.HARD, Integer.MAX_VALUE));
    }

    private static EnumMap<ConfigKey, Double> editable() {
        return new EnumMap<>(GameConfigStore.defaults().values());
    }

    private static void assertInvalid(ConfigKey first, double a, ConfigKey second, double b) {
        EnumMap<ConfigKey, Double> values = editable();
        values.put(first, a);
        values.put(second, b);
        assertThrows(IllegalArgumentException.class, () -> GameConfig.from(values));
    }
}
