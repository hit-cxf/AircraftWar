package edu.hitsz.application;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.observer.PropEffectObserver;
import edu.hitsz.prop.AbstractEffectProp;

import java.util.ArrayList;
import java.util.List;

/** 为一次道具拾取注册当前存活对象，并统计这次通知新击毁的敌机。 */
public final class PropEffectAction {
    private PropEffectAction() {
    }

    public static int activate(AbstractEffectProp prop, HeroAircraft hero,
                               List<? extends AbstractAircraft> enemies,
                               List<? extends BaseBullet> bullets) {
        List<AbstractAircraft> targets = new ArrayList<>();
        List<PropEffectObserver> registered = new ArrayList<>();
        for (AbstractAircraft enemy : enemies) {
            if (!enemy.notValid() && enemy instanceof PropEffectObserver observer) {
                targets.add(enemy);
                registered.add(observer);
            }
        }
        for (BaseBullet bullet : bullets) {
            if (!bullet.notValid() && bullet instanceof PropEffectObserver observer) {
                registered.add(observer);
            }
        }

        registered.forEach(prop::addObserver);
        try {
            prop.activate(hero);
            int destroyed = 0;
            for (AbstractAircraft enemy : targets) {
                if (enemy.notValid()) {
                    destroyed++;
                }
            }
            return destroyed;
        } finally {
            registered.forEach(prop::removeObserver);
        }
    }
}
