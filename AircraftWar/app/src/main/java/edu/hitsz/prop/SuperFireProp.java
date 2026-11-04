package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;

public class SuperFireProp extends AbstractProp {

    public SuperFireProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void activate(HeroAircraft heroAircraft) {
        System.out.println("Ring burst active!");
        heroAircraft.enableRingShoot();
    }
}
