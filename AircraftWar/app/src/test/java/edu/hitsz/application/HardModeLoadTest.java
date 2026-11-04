package edu.hitsz.application;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.audio.AudioManager;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.config.GameConfigStore;
import edu.hitsz.effects.CombatEffects;
import edu.hitsz.prop.AbstractProp;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static edu.hitsz.config.ConfigKey.HERO_BULLET_SPEED;
import static org.junit.jupiter.api.Assertions.*;

/** 真实困难参数的无声、无窗口压测；直接推进固定逻辑步，不启动 AnimationTimer。 */
class HardModeLoadTest {
    private static final int STEPS = 1500;
    private static final int PHASE_TWO_STEP = 300;
    private static final int PROTECT_BOSS_UNTIL_STEP = 400;

    @Test
    @Timeout(20)
    void realHardModeRunsSixtyLogicalSecondsWithoutUnboundedState() throws Exception {
        SilentAudio audio = new SilentAudio();
        AtomicInteger gameOverCalls = new AtomicInteger();
        Game game = new Game(Difficulty.HARD, ignored -> gameOverCalls.incrementAndGet(), audio) {
            @Override protected GameParameters parametersAtLevel(int level) {
                return config.parameters(Difficulty.HARD, level, score(this));
            }
            @Override protected int bossHpForSpawn(int number) {
                return config.bossHp(Difficulty.HARD, number);
            }
        };
        try {
            GameParameters initial = game.config.parameters(Difficulty.HARD, 0, 100);
            set(game, "parameters", initial);
            set(game, "nextBossScore", initial.bossThreshold());
            set(game, "score", 100);
            HeroAircraft hero = HeroAircraft.getInstance();
            int heroY = Main.WINDOW_HEIGHT - hero.getHeight();
            List<AbstractAircraft> enemies = list(game, "enemyAircrafts");
            List<BaseBullet> heroBullets = list(game, "heroBullets");
            List<BaseBullet> enemyBullets = list(game, "enemyBullets");
            List<AbstractProp> props = list(game, "props");
            CombatEffects effects = (CombatEffects) get(game, "combatEffects");
            Method tick = Game.class.getDeclaredMethod("tick");
            tick.setAccessible(true);
            int maxEnemies = 0, maxHeroBullets = 0, maxEnemyBullets = 0, maxProps = 0;
            int maxExplosions = 0, maxHits = 0, maxShockwaves = 0, maxRecoveries = 0;
            int completed = 0, offscreenSteps = 0;
            long totalTickNanos = 0;
            boolean bossWarningSeen = false, bossSeen = false, phaseTwoSeen = false, phaseTwoShotSeen = false;
            BossEnemy testedBoss = null;
            for (int step = 1; step <= STEPS; step++) {
                BossEnemy boss = (BossEnemy) get(game, "activeBoss");
                if (step == PHASE_TWO_STEP) {
                    assertNotNull(boss, "第300步应仍有真实生成的Boss，以覆盖半血阶段切换");
                    assertFalse(boss.isSecondPhase(), "之前的避让直射不应提前进入二阶段");
                    boss.decreaseHp(boss.getHp() - boss.getMaxHp() / 2);
                    testedBoss = boss;
                    assertTrue(boss.isSecondPhase());
                    assertTrue(boss.isCharging());
                }
                hero.increaseHp(hero.getMaxHp());
                int lane = safeLane(hero, heroY, enemies, props,
                        step <= PROTECT_BOSS_UNTIL_STEP ? boss : null,
                        game.config.value(HERO_BULLET_SPEED));
                if (lane < 0) offscreenSteps++;
                hero.setLocation(lane, heroY);
                long start = System.nanoTime();
                tick.invoke(game);
                effects.update(.04);
                totalTickNanos += System.nanoTime() - start;
                completed++;
                assertFalse((boolean) get(game, "gameOverFlag"), "压测角色不应在第" + step + "步提前结束");
                bossWarningSeen |= (boolean) get(game, "bossIncoming");
                boss = (BossEnemy) get(game, "activeBoss");
                bossSeen |= boss != null;
                phaseTwoSeen |= boss != null && boss.isSecondPhase();
                phaseTwoShotSeen |= testedBoss != null && boss == testedBoss && !boss.isCharging()
                        && (int) get(game, "enemyShootCounter") == 0;
                maxEnemies = Math.max(maxEnemies, enemies.size());
                maxHeroBullets = Math.max(maxHeroBullets, heroBullets.size());
                maxEnemyBullets = Math.max(maxEnemyBullets, enemyBullets.size());
                maxProps = Math.max(maxProps, props.size());
                maxExplosions = Math.max(maxExplosions, effects.explosions().size());
                maxHits = Math.max(maxHits, effects.hits().size());
                maxShockwaves = Math.max(maxShockwaves, effects.shockwaves().size());
                maxRecoveries = Math.max(maxRecoveries, effects.recoveries().size());
            }
            System.out.printf("[HardModeLoad] %d steps / 60s logic; mean tick %.3f ms; "
                            + "max enemies=%d heroBullets=%d enemyBullets=%d props=%d; "
                            + "FX explosions=%d hits=%d shockwaves=%d recoveries=%d; offscreen steps=%d%n",
                    completed, totalTickNanos / 1_000_000.0 / completed, maxEnemies, maxHeroBullets,
                    maxEnemyBullets, maxProps, maxExplosions, maxHits, maxShockwaves, maxRecoveries,
                    offscreenSteps);
            assertEquals(STEPS, completed);
            assertEquals(STEPS, get(game, "elapsedTicks"));
            assertEquals(3, get(game, "difficultyLevel"), "必须跨越20/40/60秒的三个真实困难阶段");
            assertEquals(game.config.parameters(Difficulty.HARD, 3, score(game)), get(game, "parameters"));
            assertTrue(bossWarningSeen);
            assertTrue(bossSeen);
            assertTrue(phaseTwoSeen);
            assertTrue(phaseTwoShotSeen, "半血蓄力结束后必须实际走过二阶段射击周期");
            assertEquals(0, gameOverCalls.get());
            assertNull(get(game, "timer"), "压测不启动定时器");
            assertTrue(audio.backgroundCalls > 0, "Boss登场走真实背景音切换入口，但输出为无声替身");
            assertTrue(maxEnemies <= 8, "默认困难最多7架普通敌机加1架Boss");
            assertTrue(maxHeroBullets < 1000);
            assertTrue(maxEnemyBullets < 1000);
            assertTrue(maxEnemyBullets >= 20, "应包含真实Boss环射弹幕");
            assertTrue(maxProps < 100);
            assertTrue(maxExplosions < 64);
            assertTrue(maxHits <= 128);
            assertTrue(maxShockwaves < 16);
            assertTrue(maxRecoveries <= 16);
            effects.update(10.0);
            assertTrue(effects.explosions().isEmpty());
            assertTrue(effects.hits().isEmpty());
            assertTrue(effects.shockwaves().isEmpty());
            assertTrue(effects.recoveries().isEmpty());
            assertEquals(0.0, effects.freezeOpacity());
            assertEquals(0.0, effects.damageOpacity());
            for (AbstractAircraft enemy : enemies) assertEquals(0.0, effects.flashOpacity(enemy));
        } finally {
            game.stop();
            GameConfigStore.activate(Difficulty.NORMAL);
        }
    }

    /** 下一步可能撞机的区域都避让；前400步也避免射中Boss，保证300步阶段覆盖不依赖随机击杀。 */
    private static int safeLane(HeroAircraft hero, int y, List<AbstractAircraft> enemies,
            List<AbstractProp> props, BossEnemy protectedBoss, double bulletSpeed) {
        int edge = hero.getWidth() / 2;
        int[] lanes = {edge, Main.WINDOW_WIDTH - edge, 112, 400, 176, 336, 240, 272};
        for (int x : lanes) {
            boolean safe = true;
            for (AbstractAircraft enemy : enemies) {
                if (enemy.notValid()) continue;
                double paddingX = (hero.getWidth() + enemy.getWidth()) / 2.0
                        + Math.abs(enemy.getVelocityX()) + 2;
                double paddingY = (hero.getHeight() + enemy.getHeight()) / 4.0
                        + Math.abs(enemy.getVelocityY()) + 2;
                if (Math.abs(x - enemy.getLocationX()) < paddingX
                        && Math.abs(y - enemy.getLocationY()) < paddingY) {
                    safe = false;
                    break;
                }
            }
            if (safe && protectedBoss != null) {
                safe = avoidsBossShot(x, y, protectedBoss, bulletSpeed);
                // 强化道具会改变弹道；先保留确定的直射来保护这次Boss阶段验证。
                for (AbstractProp prop : props) {
                    if (Math.abs(x - prop.getLocationX()) < (hero.getWidth() + prop.getWidth()) / 2.0 + 2
                            && Math.abs(y - prop.getLocationY())
                            < hero.getHeight() / 4.0 + prop.getHeight() / 2.0 + Math.abs(prop.getVelocityY()) + 2) {
                        safe = false;
                        break;
                    }
                }
            }
            if (safe) return x;
        }
        return -100; // 仅用于保持压测推进；不改变碰撞、伤害、生成或清理逻辑。
    }

    private static boolean avoidsBossShot(int x, int heroY, BossEnemy boss, double bulletSpeed) {
        double halfHeight = boss.getHeight() / 4.0 + ImageManager.HERO_BULLET_IMAGE.getHeight() / 2.0;
        double distance = heroY - 2 - boss.getLocationY();
        int first = Math.max(1, (int) Math.floor((distance - halfHeight) / bulletSpeed) - 2);
        int last = (int) Math.ceil((distance + halfHeight) / bulletSpeed) + 2;
        double collisionWidth = (boss.getWidth() + ImageManager.HERO_BULLET_IMAGE.getWidth()) / 2.0 + 2;
        double left = boss.getWidth() / 2.0;
        double span = Main.WINDOW_WIDTH - boss.getWidth();
        for (int future = first; future <= last; future++) {
            double position = boss.getLocationX() - left + boss.getVelocityX() * future;
            double folded = (position % (2 * span) + 2 * span) % (2 * span);
            double projectedX = left + (folded <= span ? folded : 2 * span - folded);
            if (Math.abs(x - projectedX) < collisionWidth) return false;
        }
        return true;
    }

    private static int score(Game game) {
        try { return (int) get(game, "score"); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }

    private static Object get(Game game, String name) throws ReflectiveOperationException {
        Field field = Game.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(game);
    }

    private static void set(Game game, String name, Object value) throws ReflectiveOperationException {
        Field field = Game.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> list(Game game, String name) throws ReflectiveOperationException {
        return (List<T>) get(game, name);
    }

    private static final class SilentAudio extends AudioManager {
        private int backgroundCalls;
        @Override public synchronized void playBackground(String path) { backgroundCalls++; }
        @Override public synchronized void stopBackground() { }
        @Override public synchronized void playEffect(String path) { }
    }
}
