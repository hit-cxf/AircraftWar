package edu.hitsz.factory;

import edu.hitsz.prop.*;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.PROP_SPEED;

public final class PropFactory {

    private PropFactory() {
    }

    public static AbstractProp create(PropType type, int x, int y) {
        AbstractProp prop = switch (type) {
            case BLOOD -> new BloodProp(x, y, 0, 0);
            case FIRE -> new FireProp(x, y, 0, 0);
            case SUPER_FIRE -> new SuperFireProp(x, y, 0, 0);
            case BOMB -> new BombProp(x, y, 0, 0);
            case FREEZE -> new FreezeProp(x, y, 0, 0);
        };
        prop.setVelocity(0, GameConfigStore.active().value(PROP_SPEED));
        return prop;
    }
}
