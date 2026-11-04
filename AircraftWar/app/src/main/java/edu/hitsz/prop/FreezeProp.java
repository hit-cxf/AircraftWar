package edu.hitsz.prop;

public class FreezeProp extends AbstractEffectProp {

    public FreezeProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    protected void notifyObservers() {
        for (var observer : observers) {
            observer.onFreeze();
        }
    }
}
