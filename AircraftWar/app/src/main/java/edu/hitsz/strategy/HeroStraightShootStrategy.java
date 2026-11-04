package edu.hitsz.strategy;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.bullet.HeroBullet;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;

import java.util.List;

public class HeroStraightShootStrategy implements ShootStrategy {
    private final GameConfig config = GameConfigStore.active();
    private final int power;

    public HeroStraightShootStrategy(int power) {
        this.power = power;
    }

    @Override
    public List<BaseBullet> shoot(AbstractAircraft aircraft) {
        BaseBullet bullet = new HeroBullet(aircraft.getLocationX(),
                aircraft.getLocationY() - 2, 0, 0, power);
        bullet.setVelocity(0, aircraft.getVelocityY() - config.value(HERO_BULLET_SPEED));
        return List.of(bullet);
    }
}
