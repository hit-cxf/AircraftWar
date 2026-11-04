package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.EnemyBullet;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;

import java.util.List;

public class EnemySingleShootStrategy implements ShootStrategy {
    private final GameConfig config = GameConfigStore.active();

    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        BaseBullet bullet = new EnemyBullet(aircraft.getLocationX(),
                aircraft.getLocationY() + 2, 0, 0, config.integer(ENEMY_POWER));
        bullet.setVelocity(0, aircraft.getVelocityY() + config.value(ENEMY_BULLET_SPEED));
        return List.of(bullet);
    }
}
