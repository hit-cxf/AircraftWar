package edu.hitsz.observer;

import edu.hitsz.aircraft.*;
import edu.hitsz.bullet.EnemyBullet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PropEffectObserverTest {
    @Test
    void bombDestroysThreeEnemyTypesAndEnemyBullets() {
        AbstractAircraft[] enemies = {
            new MobEnemy(256, 200, 2, 10, 100),
            new EliteEnemy(256, 200, 2, 10, 100),
            new ElitePlusEnemy(256, 200, 2, 10, 100)
        };
        for (AbstractAircraft enemy : enemies) {
            ((PropEffectObserver) enemy).onBomb();
            assertTrue(enemy.notValid());
        }
        EnemyBullet bullet = new EnemyBullet(256, 200, 2, 10, 10);
        bullet.onBomb();
        assertTrue(bullet.notValid());
    }

    @Test
    void bombDamagesAceButDoesNotAffectBoss() {
        EliteProEnemy ace = new EliteProEnemy(256, 200, 2, 10, 120);
        ace.onBomb();
        assertEquals(60, ace.getHp());
        assertFalse(ace.notValid());
        ace.onBomb();
        assertEquals(0, ace.getHp());
        assertTrue(ace.notValid());

        BossEnemy boss = new BossEnemy(256, 120, 2, 0, 300);
        boss.onBomb();
        boss.onFreeze();
        boss.forward();
        assertEquals(300, boss.getHp());
        assertFalse(boss.notValid());
        assertEquals(258, boss.getLocationX());
    }

    @Test
    void frozenEnemiesAndBulletRemainAtTheirCurrentLocation() {
        AbstractAircraft[] enemies = {
            new MobEnemy(256, 200, 2, 10, 100),
            new EliteEnemy(256, 200, 2, 10, 100),
            new ElitePlusEnemy(256, 200, 2, 10, 100)
        };
        for (AbstractAircraft enemy : enemies) {
            ((PropEffectObserver) enemy).onFreeze();
            enemy.forward();
            assertEquals(256, enemy.getLocationX());
            assertEquals(200, enemy.getLocationY());
            assertFalse(enemy.notValid());
        }
        EnemyBullet bullet = new EnemyBullet(256, 200, 2, 10, 10);
        bullet.onFreeze();
        bullet.forward();
        assertEquals(256, bullet.getLocationX());
        assertEquals(200, bullet.getLocationY());
        assertFalse(bullet.notValid());
    }

    @Test
    void aceMovesAtHalfSpeedAndRepeatedFreezeDoesNotHalveItAgain() {
        EliteProEnemy ace = new EliteProEnemy(256, 200, 2, 10, 120);
        ace.onFreeze();
        ace.forward();
        assertEquals(257, ace.getLocationX());
        assertEquals(205, ace.getLocationY());
        ace.onFreeze();
        ace.forward();
        assertEquals(258, ace.getLocationX());
        assertEquals(210, ace.getLocationY());
        assertEquals(10, ace.getSpeedY(), "减速不能改写原始速度");
    }
}
