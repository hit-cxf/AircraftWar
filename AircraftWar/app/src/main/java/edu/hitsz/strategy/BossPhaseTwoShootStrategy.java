package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.bullet.BaseBullet;

import java.util.List;

/** Boss 二阶段交替使用散射和环射，沿用原子弹伤害与射击策略。 */
public final class BossPhaseTwoShootStrategy implements ShootStrategy {

    private final ShootStrategy scatter = new EnemyScatterShootStrategy();
    private final ShootStrategy ring = new EnemyRingShootStrategy();
    private boolean scatterNext = true;

    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        List<BaseBullet> bullets = (scatterNext ? scatter : ring).shoot(aircraft);
        scatterNext = !scatterNext;
        return bullets;
    }
}
