package edu.hitsz.prop;

public class BombProp extends AbstractEffectProp {

    public BombProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    protected void notifyObservers() {
        for (var observer : observers) {
            observer.onBomb();
        }
    }
}
