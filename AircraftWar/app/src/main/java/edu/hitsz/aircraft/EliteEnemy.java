package edu.hitsz.aircraft;

import edu.hitsz.application.Main;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;

import edu.hitsz.observer.PropEffectObserver;
import edu.hitsz.strategy.EnemySingleShootStrategy;


/**
 * 精英敌机
 * 可射击、会掉落道具
 * @author hitsz
 */
public class EliteEnemy extends AbstractAircraft implements PropEffectObserver {
    private final GameConfig config = GameConfigStore.active();

    public EliteEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        setShootStrategy(new EnemySingleShootStrategy());
    }

    @Override
    public void forward() {
        if (isMovementFrozen()) {
            return;
        }
        super.forward();
        // 判定 y 轴向下飞行出界
        if (locationY >= Main.WINDOW_HEIGHT ) {
            vanish();
        }
    }


    @Override
    public void onBomb() {
        vanish();
    }

    @Override
    public void onFreeze() {
        movementEffect.freeze(config.integer(ELITE_FREEZE_MS));
    }
}
