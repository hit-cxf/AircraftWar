package edu.hitsz.effects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CombatEffectsTest {
    @Test
    void damageFeedbackExpiresAndLeavingGameDropsObjectReferences() {
        CombatEffects effects = new CombatEffects();
        Object target = new Object();
        effects.flash(target);
        effects.addHit(100, 200);
        effects.heroDamaged();
        effects.addRecovery(200, 300, 20);
        effects.update(0.15);
        assertEquals(0, effects.flashOpacity(target));
        assertFalse(effects.hits().isEmpty());
        assertTrue(effects.damageOpacity() > 0);
        effects.update(0.3);
        assertTrue(effects.hits().isEmpty());
        assertEquals(0, effects.damageOpacity(), 1e-9);
        assertEquals(20, effects.recoveries().getFirst().amount());
        effects.flash(target);
        effects.clear();
        assertEquals(0, effects.flashOpacity(target));
        assertTrue(effects.recoveries().isEmpty());
    }
    @Test
    void simultaneousDeathsHaveIndependentAnimationLifetimes() {
        CombatEffects effects = new CombatEffects();
        effects.addExplosion(10, 20, 80);
        effects.update(0.5);
        effects.addExplosion(30, 40, 160);
        var snapshot = effects.explosions();
        assertEquals(2, snapshot.size());
        assertTrue(snapshot.get(0).progress() > snapshot.get(1).progress());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);

        effects.update(0.3);
        assertEquals(1, effects.explosions().size());
        assertEquals(30, effects.explosions().get(0).x());
        assertEquals(2, snapshot.size(), "先前的渲染快照不随下一帧改变");
        effects.update(0.5);
        assertFalse(effects.hasExplosions());
    }

    @Test
    void freezeOverlayFadesAndAnotherPickupRefreshesItsDuration() {
        CombatEffects effects = new CombatEffects();
        assertEquals(0, effects.freezeOpacity());
        effects.triggerFreeze();
        effects.update(0.08);
        assertEquals(0.5, effects.freezeOpacity(), 0.00001);
        effects.update(4.42);
        assertEquals(0.5, effects.freezeOpacity(), 0.00001);

        effects.triggerFreeze();
        assertEquals(0.5, effects.freezeOpacity(), 0.00001, "刷新不应产生透明闪烁");
        effects.update(0.2);
        assertEquals(1, effects.freezeOpacity());
        effects.update(4.8);
        assertEquals(0, effects.freezeOpacity());
    }

    @Test
    void bombShakesDecayToZeroAndShockwavesCanOverlap() {
        CombatEffects effects = new CombatEffects();
        effects.triggerBomb(100, 200);
        assertTrue(Math.abs(effects.shakeX()) > 0);
        assertTrue(Math.abs(effects.shakeX()) <= 8);
        effects.update(0.2);
        effects.triggerBomb(300, 400);
        assertEquals(2, effects.shockwaves().size());
        assertEquals(420, effects.shockwaves().get(0).size());
        effects.update(0.35);
        assertEquals(0, effects.shakeX());
        assertEquals(0, effects.shakeY());
        assertEquals(1, effects.shockwaves().size());
        effects.update(0.2);
        assertTrue(effects.shockwaves().isEmpty());
    }

    @Test
    void clearRemovesEveryEffectWhenLeavingTheGame() {
        CombatEffects effects = new CombatEffects();
        effects.triggerFreeze();
        effects.triggerBomb(100, 200);
        effects.addExplosion(100, 200, 80);
        effects.update(0.1);
        effects.clear();
        assertFalse(effects.hasExplosions());
        assertTrue(effects.shockwaves().isEmpty());
        assertEquals(0, effects.freezeOpacity());
        assertEquals(0, effects.shakeX());
        assertEquals(0, effects.shakeY());
    }
}
