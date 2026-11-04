package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;

import java.util.ArrayList;
import java.util.List;

public class EnemyDoubleShootStrategy implements ShootStrategy {
    private final GameConfig config = GameConfigStore.active();

    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        List<BaseBullet> bullets = new ArrayList<>(2);
        for (int direction : new int[]{-1, 1}) {
            int x = (int) Math.round(aircraft.getLocationX()
                    + direction * config.value(ENEMY_DOUBLE_OFFSET));
            BaseBullet bullet = new EnemyBullet(x, aircraft.getLocationY() + 2,
                    0, 0, config.integer(ENEMY_POWER));
            bullet.setVelocity(0, aircraft.getVelocityY() + config.value(ENEMY_BULLET_SPEED));
            bullets.add(bullet);
        }
        return bullets;
    }
}
