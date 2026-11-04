package edu.hitsz.application;

import edu.hitsz.aircraft.*;
import edu.hitsz.prop.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameEffectsIntegrationTest {
    @Test
    void pickupAddsRingOnTheSameTickWithoutWaitingForNormalShooting() throws Exception {
        try (EffectsFixture fixture = new EffectsFixture()) {
            fixture.props().add(new SuperFireProp(256, 670, 0, 0));
            fixture.call("tick");
            assertEquals(20, fixture.heroBullets().size());
            fixture.call("tick");
            assertEquals(20, fixture.heroBullets().size(), "下一帧不能重复发射同一圈");
            assertEquals(1, HeroAircraft.getInstance().shoot().size(), "常驻直射不被替换");
        }
    }

    @Test
    void freezePickupTriggersMaskButOnlyAffectedEnemiesKeepFrost() throws Exception {
        try (EffectsFixture fixture = new EffectsFixture()) {
            MobEnemy mob = new MobEnemy(100, 200, 0, 10, 30);
            BossEnemy boss = new BossEnemy(256, 120, 2, 0, 300);
            fixture.enemies().add(mob);
            fixture.enemies().add(boss);
            fixture.props().add(new FreezeProp(256, 670, 0, 0));
            fixture.call("crashCheckAction");
            fixture.effects().update(.25);
            assertTrue(fixture.effects().freezeOpacity() > 0);
            assertTrue(mob.hasMovementEffect());
            assertFalse(boss.hasMovementEffect());
            fixture.effects().update(6);
            assertEquals(0, fixture.effects().freezeOpacity());
            assertTrue(mob.hasMovementEffect(), "全屏蒙板淡出后普通敌机仍永久冻结");
        }
    }

    @Test
    void bombPickupShakesAndEachNewKillHasOneExplosionWithoutDrops() throws Exception {
        try (EffectsFixture fixture = new EffectsFixture()) {
            fixture.enemies().add(new MobEnemy(100, 200, 0, 10, 30));
            fixture.enemies().add(new EliteEnemy(300, 250, 0, 10, 60));
            fixture.props().add(new BombProp(256, 670, 0, 0));
            fixture.call("crashCheckAction");
            fixture.call("postProcessAction");
            assertEquals(20, fixture.get("score"));
            assertTrue(fixture.enemies().isEmpty());
            assertTrue(fixture.props().isEmpty());
            assertEquals(3, fixture.effects().explosions().size(), "炸弹本体 + 两架坠毁敌机");
            assertEquals(1, fixture.effects().shockwaves().size());
            assertTrue(fixture.audio.events.contains("/videos/bomb_explosion.wav"));
            fixture.call("postProcessAction");
            assertEquals(3, fixture.effects().explosions().size(), "清理后不重复生成坠毁动画");
        }
    }

    @Test
    void allInViewDestructionExplodesButOrdinaryExitDoesNot() throws Exception {
        try (EffectsFixture fixture = new EffectsFixture()) {
            AbstractAircraft[] enemies = {
                new MobEnemy(100, 180, 0, 10, 30), new EliteEnemy(200, 200, 0, 10, 60),
                new ElitePlusEnemy(300, 220, 2, 10, 90), new EliteProEnemy(400, 240, 2, 10, 120),
                new BossEnemy(256, 120, 2, 0, 300), new MobEnemy(100, Main.WINDOW_HEIGHT, 0, 10, 30)
            };
            for (AbstractAircraft enemy : enemies) { enemy.vanish(); fixture.enemies().add(enemy); }
            fixture.call("postProcessAction");
            assertEquals(5, fixture.effects().explosions().size());
            assertTrue(fixture.enemies().isEmpty());
            fixture.game.stop();
            assertFalse(fixture.effects().hasExplosions());
            assertEquals(0, fixture.effects().freezeOpacity());
        }
    }
}
