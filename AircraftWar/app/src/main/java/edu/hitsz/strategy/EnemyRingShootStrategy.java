package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;

import java.util.ArrayList;
import java.util.List;

public class EnemyRingShootStrategy implements ShootStrategy {
    private final GameConfig config = GameConfigStore.active();

    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        int count = config.integer(ENEMY_RING_COUNT);
        double speed = config.value(ENEMY_RING_SPEED);
        List<BaseBullet> bullets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double angle = 2 * Math.PI * i / count;
            BaseBullet bullet = new EnemyBullet(aircraft.getLocationX(),
                    aircraft.getLocationY(), 0, 0, config.integer(ENEMY_POWER));
            bullet.setVelocity(speed * Math.cos(angle), speed * Math.sin(angle));
            bullets.add(bullet);
        }
        return bullets;
    }
}
