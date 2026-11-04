package edu.hitsz.aircraft;

import edu.hitsz.bullet.EnemyBullet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BossEnemyTest {

    @Test
    void halfHealthStartsTheChargeAndPreservesMaximumHealth() {
        BossEnemy boss = new BossEnemy(200, 120, 2, 0, 300);
        assertEquals(20, boss.shoot().size());
        assertFalse(boss.isSecondPhase());
        assertEquals(0.0, boss.chargeProgress());

        boss.decreaseHp(149);
        assertFalse(boss.isSecondPhase());
        boss.decreaseHp(1);

        assertEquals(150, boss.getHp());
        assertEquals(300, boss.getMaxHp());
        assertTrue(boss.isSecondPhase());
        assertTrue(boss.isCharging());
        assertEquals(0.0, boss.chargeProgress());
        assertTrue(boss.shoot().isEmpty());
    }

    @Test
    void chargeUsesOnlySuppliedGameTimeThenAlternatesScatterAndRing() {
        BossEnemy boss = new BossEnemy(200, 120, 2, 0, 300);
        boss.decreaseHp(150);

        boss.updateCombat(0.45);
        assertEquals(0.5, boss.chargeProgress(), 1e-9);
        boss.updateCombat(0.0);
        boss.updateCombat(-1.0);
        boss.updateCombat(Double.NaN);
        boss.updateCombat(Double.POSITIVE_INFINITY);
        assertEquals(0.5, boss.chargeProgress(), 1e-9);
        assertTrue(boss.shoot().isEmpty());

        boss.updateCombat(0.44);
        assertTrue(boss.isCharging());
        assertTrue(boss.shoot().isEmpty());
        boss.updateCombat(0.01);
        assertFalse(boss.isCharging());
        assertEquals(1.0, boss.chargeProgress());

        var scatter = boss.shoot();
        assertEquals(3, scatter.size());
        assertTrue(scatter.stream().allMatch(bullet ->
                bullet instanceof EnemyBullet && bullet.getSpeedY() > 0
                        && bullet.getPower() == 10));
        assertEquals(20, boss.shoot().size());
        assertEquals(3, boss.shoot().size());
        assertEquals(20, boss.shoot().size());
    }

    @Test
    void furtherDamageDoesNotRestartChargeOrResetTheVolleySequence() {
        BossEnemy boss = new BossEnemy(200, 120, 2, 0, 301);
        boss.decreaseHp(151);
        assertTrue(boss.isCharging());
        boss.updateCombat(0.4);
        double progress = boss.chargeProgress();
        boss.decreaseHp(10);
        assertEquals(progress, boss.chargeProgress());
        boss.updateCombat(0.5);
        assertEquals(3, boss.shoot().size());

        boss.decreaseHp(10);
        assertFalse(boss.isCharging());
        assertEquals(1.0, boss.chargeProgress());
        assertEquals(20, boss.shoot().size());
    }

    @Test
    void deathCancelsChargingAndCannotFireEvenAfterTimeAdvances() {
        BossEnemy boss = new BossEnemy(200, 120, 2, 0, 300);
        boss.decreaseHp(150);
        boss.updateCombat(0.4);
        boss.decreaseHp(150);

        assertTrue(boss.notValid());
        assertFalse(boss.isCharging());
        assertEquals(0.0, boss.chargeProgress());
        assertTrue(boss.shoot().isEmpty());
        boss.updateCombat(10.0);
        assertFalse(boss.isCharging());
        assertTrue(boss.shoot().isEmpty());

        BossEnemy killedImmediately = new BossEnemy(200, 120, 2, 0, 300);
        killedImmediately.decreaseHp(300);
        assertFalse(killedImmediately.isSecondPhase());
        assertFalse(killedImmediately.isCharging());
        assertTrue(killedImmediately.shoot().isEmpty());

        BossEnemy removed = new BossEnemy(200, 120, 2, 0, 300);
        removed.decreaseHp(150);
        removed.vanish();
        assertFalse(removed.isCharging());
        assertEquals(0.0, removed.chargeProgress());
        assertTrue(removed.shoot().isEmpty());
    }
}
