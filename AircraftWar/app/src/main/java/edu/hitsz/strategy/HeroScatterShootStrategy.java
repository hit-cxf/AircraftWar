package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;

import java.util.ArrayList;
import java.util.List;

public class HeroScatterShootStrategy implements ShootStrategy {
    private final GameConfig config = GameConfigStore.active();
    private final int power;

    public HeroScatterShootStrategy(int power) {
        this.power = power;
    }

    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        int count = config.integer(HERO_SCATTER_COUNT);
        List<BaseBullet> bullets = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double speedX = (i - (count - 1) / 2.0) * config.value(HERO_SCATTER_SPREAD);
            BaseBullet bullet = new HeroBullet(aircraft.getLocationX(),
                    aircraft.getLocationY() - 2, 0, 0, power);
            bullet.setVelocity(speedX, aircraft.getVelocityY() - config.value(HERO_BULLET_SPEED));
            bullets.add(bullet);
        }
        return bullets;
    }
}
