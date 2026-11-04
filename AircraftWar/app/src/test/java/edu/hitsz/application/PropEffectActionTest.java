package edu.hitsz.application;

import edu.hitsz.aircraft.*;
import edu.hitsz.bullet.*;
import edu.hitsz.prop.BombProp;
import edu.hitsz.prop.FreezeProp;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PropEffectActionTest {
    @Test
    void bombCountsOnlyNewKillsAndDoesNotCountBulletsOrDamageToSurvivors() {
        MobEnemy alreadyDead = new MobEnemy(256, 200, 0, 10, 30);
        alreadyDead.vanish();
        EliteProEnemy ace = new EliteProEnemy(256, 200, 2, 10, 120);
        BossEnemy boss = new BossEnemy(256, 120, 2, 0, 300);
        List<AbstractAircraft> enemies = List.of(
                alreadyDead, new MobEnemy(256, 200, 0, 10, 30),
                new EliteEnemy(256, 200, 0, 10, 60),
                new ElitePlusEnemy(256, 200, 2, 10, 90), ace, boss);
        EnemyBullet bullet = new EnemyBullet(256, 200, 0, 10, 10);
        BombProp bomb = new BombProp(256, 200, 0, 3);

        assertEquals(3, PropEffectAction.activate(bomb, null, enemies, List.of(bullet)));
        assertTrue(bullet.notValid());
        assertEquals(60, ace.getHp());
        assertEquals(300, boss.getHp());
        // 一次通知结束后移除注册，消耗掉的主题不再持有这些对象。
        bomb.activate(null);
        assertEquals(60, ace.getHp());

        assertEquals(1, PropEffectAction.activate(new BombProp(0, 0, 0, 0),
                null, enemies, List.of(bullet)));
        assertTrue(ace.notValid());
        assertEquals(0, PropEffectAction.activate(new BombProp(0, 0, 0, 0),
                null, enemies, List.of(bullet)));
    }

    @Test
    void freezeStopsLiveTargetsWithoutAwardingKillsOrAffectingHeroBullets() {
        EliteEnemy enemy = new EliteEnemy(256, 200, 0, 10, 60);
        EnemyBullet bullet = new EnemyBullet(256, 200, 0, 10, 10);
        HeroBullet heroBullet = new HeroBullet(256, 200, 0, -10, 30);
        assertEquals(0, PropEffectAction.activate(new FreezeProp(0, 0, 0, 0),
                null, List.of(enemy), List.of(bullet, heroBullet)));
        enemy.forward();
        bullet.forward();
        heroBullet.forward();
        assertEquals(200, enemy.getLocationY());
        assertEquals(200, bullet.getLocationY());
        assertEquals(190, heroBullet.getLocationY());
        assertFalse(enemy.notValid());
    }
}
