package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;

import java.util.ArrayList;
import java.util.List;

public class EnemyScatterShootStrategy implements ShootStrategy {
    private final GameConfig config = GameConfigStore.active();

    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        int count = config.integer(ENEMY_SCATTER_COUNT);
        List<BaseBullet> bullets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double speedX = (i - (count - 1) / 2.0) * config.value(ENEMY_SCATTER_SPREAD);
            BaseBullet bullet = new EnemyBullet(aircraft.getLocationX(),
                    aircraft.getLocationY() + 2, 0, 0, config.integer(ENEMY_POWER));
            bullet.setVelocity(speedX, aircraft.getVelocityY() + config.value(ENEMY_BULLET_SPEED));
            bullets.add(bullet);
        }
        return bullets;
    }
}
