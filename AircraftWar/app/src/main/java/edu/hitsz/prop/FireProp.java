package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;

public class FireProp extends AbstractProp {

    public FireProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void activate(HeroAircraft heroAircraft) {
        System.out.println("FireSupply active!");
        heroAircraft.enableScatterShoot();
    }
}
