package edu.hitsz.prop;

import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.observer.PropEffectObserver;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractEffectProp extends AbstractProp {
    protected final List<PropEffectObserver> observers = new ArrayList<>();

    public AbstractEffectProp(int x, int y, int speedX, int speedY) {
        super(x, y, speedX, speedY);
    }

    public void addObserver(PropEffectObserver observer) {
        if (!observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(PropEffectObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void activate(HeroAircraft heroAircraft) {
        notifyObservers();
    }

    protected abstract void notifyObservers();
}
