package edu.hitsz.config;

import edu.hitsz.aircraft.*;
import edu.hitsz.application.Difficulty;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.basic.MovementEffect;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.factory.PropFactory;
import edu.hitsz.prop.BloodProp;
import edu.hitsz.prop.PropType;
import edu.hitsz.strategy.EnemyDoubleShootStrategy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static edu.hitsz.config.ConfigKey.*;
import static org.junit.jupiter.api.Assertions.*;

/** 使用内存配置快照验证真实域行为，不触碰玩家的配置文件。 */
class DomainConfigurationTest {
    private AtomicReference<GameConfig> current;
    private GameConfig previous;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void useDefaultsInMemory() throws Exception {
        GameConfigStore.current();
        Class<?> holder = Class.forName(GameConfigStore.class.getName() + "$StoreHolder");
        Field storeField = holder.getDeclaredField("VALUE");
        storeField.setAccessible(true);
        Object repository = storeField.get(null);
        Field currentField = repository.getClass().getDeclaredField("current");
        currentField.setAccessible(true);
        current = (AtomicReference<GameConfig>) currentField.get(repository);
        previous = current.getAndSet(GameConfigStore.defaults());
        GameConfigStore.activate(Difficulty.CUSTOM);
        HeroAircraft.getInstance().resetForNewGame();
    }

    @AfterEach
    void restorePreviousSnapshot() {
        current.set(previous);
        GameConfigStore.activate(Difficulty.NORMAL);
        HeroAircraft.getInstance().resetForNewGame();
    }

    @Test
    void newGameRefreshesTheSingletonAndEvenScatterIsCentered() {
        use(Map.of(HERO_HP, 345.0, HERO_POWER, 47.0,
                HERO_BULLET_SPEED, 7.0, HERO_SCATTER_COUNT, 4.0,
                HERO_SCATTER_SPREAD, 3.0, HERO_RING_COUNT, 12.0,
                HERO_RING_SPEED, 9.0, FIRE_DURATION_MS, 4321.0));
        HeroAircraft hero = HeroAircraft.getInstance();
        hero.resetForNewGame();
        assertEquals(345, hero.getHp());
        assertEquals(345, hero.getMaxHp());
        assertEquals(47, hero.shoot().getFirst().getPower());
        assertEquals(-7.0, hero.shoot().getFirst().getVelocityY());
        assertEquals(4321, hero.getFireDurationMillis());

        hero.enableScatterShoot();
        hero.pauseFireEffect();
        long remaining = hero.getFireRemainingMillis();
        assertTrue(remaining > 4000 && remaining <= 4321);
        var scatter = hero.shoot();
        assertEquals(4, scatter.size());
        assertArrayEquals(new double[]{-4.5, -1.5, 1.5, 4.5},
                scatter.stream().mapToDouble(BaseBullet::getVelocityX).toArray());

        hero.enableRingShoot();
        var ring = hero.drainSpecialBullets();
        assertEquals(12, ring.size());
        assertTrue(ring.stream().allMatch(b -> b.getPower() == 47
                && Math.abs(Math.hypot(b.getVelocityX(), b.getVelocityY()) - 9) < 1e-9));
        assertEquals(remaining, hero.getFireRemainingMillis());
        assertEquals(4, hero.shoot().size());

        use(Map.of(HERO_HP, 500.0, HERO_POWER, 99.0));
        assertEquals(345, hero.getMaxHp());
        assertEquals(47, hero.shoot().getFirst().getPower());
        hero.resetForNewGame();
        assertSame(hero, HeroAircraft.getInstance());
        assertEquals(500, hero.getHp());
        assertEquals(99, hero.shoot().getFirst().getPower());
        assertEquals(0, hero.getFireRemainingMillis());
    }

    @Test
    void enemiesKeepConstructionSnapshotsForDamageShotsAndBossPhases() {
        use(Map.of(ENEMY_POWER, 23.0, ENEMY_BULLET_SPEED, 8.0,
                ENEMY_SCATTER_COUNT, 4.0, ENEMY_SCATTER_SPREAD, 3.0,
                ENEMY_RING_COUNT, 12.0, ENEMY_RING_SPEED, 9.0,
                ENEMY_DOUBLE_OFFSET, 17.0, PRO_BOMB_DAMAGE, 37.0,
                BOSS_PHASE_RATIO, .75, BOSS_CHARGE_MS, 1200.0));
        EliteProEnemy pro = new EliteProEnemy(200, 120, 1, 2, 100);
        pro.setVelocity(1, .5);
        BossEnemy boss = new BossEnemy(200, 120, 2, 0, 200);
        var doubleStrategy = new EnemyDoubleShootStrategy();
        use(Map.of());

        pro.onBomb();
        assertEquals(63, pro.getHp());
        var scatter = pro.shoot();
        assertEquals(4, scatter.size());
        assertTrue(scatter.stream().allMatch(b -> b.getPower() == 23
                && b.getVelocityY() == 8.5));
        assertArrayEquals(new double[]{-4.5, -1.5, 1.5, 4.5},
                scatter.stream().mapToDouble(BaseBullet::getVelocityX).toArray());
        var doubleShot = doubleStrategy.shoot(pro);
        assertEquals(183, doubleShot.getFirst().getLocationX());
        assertEquals(217, doubleShot.getLast().getLocationX());
        assertTrue(doubleShot.stream().allMatch(b -> b.getPower() == 23));

        assertEquals(12, boss.shoot().size());
        boss.decreaseHp(49);
        assertFalse(boss.isSecondPhase());
        boss.decreaseHp(1);
        assertTrue(boss.isCharging());
        boss.updateCombat(.6);
        assertEquals(.5, boss.chargeProgress(), 1e-9);
        boss.updateCombat(.6);
        assertFalse(boss.isCharging());
        assertEquals(4, boss.shoot().size());
        assertEquals(12, boss.shoot().size());
        assertEquals(200, boss.getMaxHp());
    }

    @Test
    void customModeCannotResolveARankingFile() {
        var exception = assertThrows(IllegalStateException.class,
                () -> Difficulty.CUSTOM.scoreFile(java.nio.file.Path.of("scores")));
        assertEquals("自定义模式不记录排行榜", exception.getMessage());
    }

    @Test
    void freezeHealingAndPropMotionUseTheConfiguredValues() throws Exception {
        use(Map.of(ELITE_FREEZE_MS, 800.0, PLUS_FREEZE_MS, 1200.0,
                PRO_SLOW_MS, 1600.0, PRO_SLOW_SCALE, .25,
                BULLET_FREEZE_MS, 2000.0, HEAL_AMOUNT, 123.0, PROP_SPEED, 4.0));
        EliteEnemy elite = new EliteEnemy(200, 120, 0, 2, 50);
        ElitePlusEnemy plus = new ElitePlusEnemy(200, 120, 2, 2, 50);
        EliteProEnemy pro = new EliteProEnemy(200, 120, 2, 2, 100);
        EnemyBullet bullet = new EnemyBullet(200, 120, 0, 5, 10);
        BloodProp blood = (BloodProp) PropFactory.create(PropType.BLOOD, 200, 120);
        use(Map.of());

        elite.setEffectsPaused(true);
        elite.onFreeze();
        plus.setEffectsPaused(true);
        plus.onFreeze();
        pro.setEffectsPaused(true);
        pro.onFreeze();
        bullet.setEffectsPaused(true);
        bullet.onFreeze();
        assertEquals(800, movement(elite).remainingMillis());
        assertEquals(1200, movement(plus).remainingMillis());
        assertEquals(1600, movement(pro).remainingMillis());
        assertEquals(.25, movement(pro).scale());
        assertEquals(2000, movement(bullet).remainingMillis());
        assertEquals(4.0, blood.getVelocityY());
        HeroAircraft hero = HeroAircraft.getInstance();
        hero.decreaseHp(200);
        blood.activate(hero);
        assertEquals(hero.getMaxHp() - 77, hero.getHp());
    }

    private void use(Map<ConfigKey, Double> overrides) {
        EnumMap<ConfigKey, Double> values = new EnumMap<>(GameConfigStore.defaults().values());
        values.putAll(overrides);
        current.set(GameConfig.from(values));
        GameConfigStore.activate(Difficulty.CUSTOM);
    }

    private static MovementEffect movement(AbstractFlyingObject object) throws Exception {
        Field field = AbstractFlyingObject.class.getDeclaredField("movementEffect");
        field.setAccessible(true);
        return (MovementEffect) field.get(object);
    }
}
