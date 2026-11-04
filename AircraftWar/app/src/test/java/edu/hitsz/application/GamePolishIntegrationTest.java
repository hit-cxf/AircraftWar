package edu.hitsz.application;

import edu.hitsz.aircraft.*;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.prop.BloodProp;
import edu.hitsz.prop.SuperFireProp;
import javafx.event.Event;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GamePolishIntegrationTest {
    @Test
    void customScoreCurveUpdatesLivingEnemiesWithoutUnfreezingThem() throws Exception {
        try (EffectsFixture f = new EffectsFixture(Difficulty.CUSTOM)) {
            MobEnemy mob = new MobEnemy(100, 200, 0, 6, 30);
            f.enemies().add(mob);
            f.set("score", 37);
            f.call("updateDifficulty");
            double expected = f.game.config.parameters(Difficulty.CUSTOM, 0, 37).enemySpeed();
            assertEquals(expected, mob.getVelocityY(), 1e-9);
            int y = mob.getLocationY();
            mob.onFreeze();
            f.set("score", 103);
            f.call("updateDifficulty");
            assertTrue(mob.getVelocityY() > expected);
            mob.forward();
            assertEquals(y, mob.getLocationY(), "速度曲线更新不能清除永久冻结");
            assertTrue(mob.hasMovementEffect());
        }
    }
    @Test
    void escapeStopsSimulationAndInputUntilThePlayerResumes() throws Exception {
        try (EffectsFixture f = new EffectsFixture()) {
            HeroAircraft hero = HeroAircraft.getInstance();
            hero.enableScatterShoot();
            MobEnemy enemy = new MobEnemy(100, 200, 0, 10, 30);
            f.enemies().add(enemy);
            SuperFireProp prop = new SuperFireProp(256, 670, 0, 0);
            f.props().add(prop);
            Event.fireEvent(f.game, escape());
            assertTrue(f.game.isPaused());
            long remaining = hero.getFireRemainingMillis();
            f.call("tick");
            f.call("tick");
            Event.fireEvent(f.game, mouseAt(f.game, 350, 500));
            assertEquals(200, enemy.getLocationY());
            assertEquals(256, hero.getLocationX());
            assertFalse(prop.notValid());
            assertTrue(f.heroBullets().isEmpty());
            assertEquals(remaining, hero.getFireRemainingMillis(), "暂停时强化计时也应固定");

            Event.fireEvent(f.game, escape());
            assertFalse(f.game.isPaused());
            f.call("tick");
            assertEquals(210, enemy.getLocationY());
            assertEquals(20, f.heroBullets().size());
            assertEquals(3, hero.shoot().size(), "恢复后仍保留散射");
        }
    }

    @Test
    void focusPauseAlsoStopsBossChargeAndCannotReviveAStoppedGame() throws Exception {
        try (EffectsFixture f = new EffectsFixture()) {
            BossEnemy boss = new BossEnemy(256, 230, 2, 0, 300);
            boss.decreaseHp(150);
            f.enemies().add(boss);
            f.set("activeBoss", boss);
            f.game.pauseForFocusLoss();
            assertTrue(f.game.isPaused());
            double progress = boss.chargeProgress();
            for (int i = 0; i < 50; i++) f.call("tick");
            assertEquals(progress, boss.chargeProgress());
            f.game.resume();
            f.call("tick");
            assertTrue(boss.chargeProgress() > progress);
            f.game.stop();
            f.game.resume();
            int x = boss.getLocationX();
            f.call("tick");
            assertEquals(x, boss.getLocationX(), "停止的游戏不能被继续按钮重新启动");
        }
    }

    @Test
    void bossWarningPrecedesSpawnAndDoesNotCreateDuplicateBosses() throws Exception {
        try (EffectsFixture f = new EffectsFixture()) {
            f.set("parameters", new GameParameters(5, .2, .1, .05, 100000, 100000,
                    100000, 8, 1, 100));
            f.set("score", 100);
            f.set("nextBossScore", 100);
            f.call("spawnBoss");
            assertEquals(true, f.get("bossIncoming"));
            assertNull(f.get("activeBoss"));
            for (int i = 0; i < 39; i++) f.call("spawnBoss");
            assertNull(f.get("activeBoss"));
            f.call("spawnBoss");
            assertNotNull(f.get("activeBoss"));
            assertEquals(200, f.get("nextBossScore"));
            f.set("score", 500);
            for (int i = 0; i < 80; i++) f.call("spawnBoss");
            assertEquals(1, f.enemies().size());
            assertEquals(false, f.get("bossIncoming"));
        }
    }

    @Test
    void realCollisionsProduceDamageSparksAndOnlyActualHealingIsShown() throws Exception {
        try (EffectsFixture f = new EffectsFixture()) {
            EliteEnemy enemy = new EliteEnemy(100, 200, 0, 10, 90);
            f.enemies().add(enemy);
            f.heroBullets().add(new HeroBullet(100, 200, 0, -5, 30));
            f.enemyBullets().add(new EnemyBullet(256, 670, 0, 5, 50));
            f.props().add(new BloodProp(256, 670, 0, 0));
            f.call("crashCheckAction");
            assertEquals(60, enemy.getHp());
            assertTrue(f.effects().flashOpacity(enemy) > 0);
            assertTrue(f.effects().damageOpacity() > 0);
            assertEquals(2, f.effects().hits().size());
            assertEquals(20, f.effects().recoveries().getFirst().amount());
            assertEquals(970, HeroAircraft.getInstance().getHp());
        }
    }

    private static KeyEvent escape() {
        return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false);
    }

    private static MouseEvent mouseAt(Game game, double x, double y) {
        return new MouseEvent(MouseEvent.MOUSE_MOVED, x, y, x, y, MouseButton.NONE, 0,
                false, false, false, false, false, false, false, false, false, false,
                new PickResult(game, x, y));
    }
}
