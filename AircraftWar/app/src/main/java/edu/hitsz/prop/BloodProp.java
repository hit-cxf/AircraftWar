package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;


public class BloodProp extends AbstractProp {
    private final GameConfig config = GameConfigStore.active();
    
    public BloodProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void activate(HeroAircraft heroAircraft) {
        heroAircraft.increaseHp(config.integer(HEAL_AMOUNT));
    }
}
