package edu.hitsz.application;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Modifier;
import static org.junit.jupiter.api.Assertions.*;

class DifficultyTemplateTest {
    @Test
    void actionIsAFinalTemplateOnAnAbstractGame() throws Exception {
        assertTrue(Modifier.isAbstract(Game.class.getModifiers()));
        assertTrue(Modifier.isFinal(Game.class.getMethod("action").getModifiers()));
    }

    @Test
    void easyStaysConstantAndNeverSpawnsBoss() {
        Game game = new EasyGame(score -> {});
        assertEquals(game.parametersAtLevel(0), game.parametersAtLevel(20));
        assertEquals(0, game.parametersAtLevel(0).bossThreshold());
        assertEquals(0, game.bossHpForSpawn(1));
    }

    @Test
    void normalProgressesEnemiesButKeepsShootingAndBossHpConstant() {
        Game game = new NormalGame(score -> {});
        GameParameters first = game.parametersAtLevel(0);
        GameParameters later = game.parametersAtLevel(3);
        assertTrue(later.spawnCycle() < first.spawnCycle());
        assertTrue(later.enemySpeed() > first.enemySpeed());
        assertTrue(later.hpMultiplier() > first.hpMultiplier());
        assertEquals(first.heroShootCycle(), later.heroShootCycle());
        assertEquals(first.enemyShootCycle(), later.enemyShootCycle());
        assertEquals(game.bossHpForSpawn(1), game.bossHpForSpawn(5));
    }

    @Test
    void hardProgressesBothShootingCyclesAndEachBossGetsStronger() {
        Game game = new HardGame(score -> {});
        GameParameters first = game.parametersAtLevel(0);
        GameParameters later = game.parametersAtLevel(3);
        assertTrue(later.spawnCycle() < first.spawnCycle());
        assertTrue(later.enemySpeed() > first.enemySpeed());
        assertTrue(later.hpMultiplier() > first.hpMultiplier());
        assertTrue(later.heroShootCycle() > first.heroShootCycle());
        assertTrue(later.enemyShootCycle() < first.enemyShootCycle());
        assertTrue(game.bossHpForSpawn(2) > game.bossHpForSpawn(1));
        assertTrue(game.parametersAtLevel(100).spawnCycle() > 0);
        assertTrue(game.parametersAtLevel(100).enemyShootCycle() > 0);
    }
}
