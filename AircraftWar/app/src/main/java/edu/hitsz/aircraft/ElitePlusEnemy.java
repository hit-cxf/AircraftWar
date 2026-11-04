package edu.hitsz.aircraft;

import edu.hitsz.application.Main;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;

import edu.hitsz.observer.PropEffectObserver;
import edu.hitsz.strategy.EnemyDoubleShootStrategy;


/**
 * 精锐敌机
 * 可射击、会掉落道具
 * @author hitsz
 */
public class ElitePlusEnemy extends AbstractAircraft implements PropEffectObserver {
    private final GameConfig config = GameConfigStore.active();

    public ElitePlusEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        setShootStrategy(new EnemyDoubleShootStrategy());
    }

    @Override
    public void forward() {
        if (isMovementFrozen()) {
            return;
        }
        super.forward();
        int halfWidth = getWidth() / 2;
        if (locationX <= halfWidth) {
            locationX = halfWidth;
            speedX = Math.abs(speedX);
        } else if (locationX >= Main.WINDOW_WIDTH - halfWidth) {
            locationX = Main.WINDOW_WIDTH - halfWidth;
            speedX = -Math.abs(speedX);
        }
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
        movementEffect.freeze(config.integer(PLUS_FREEZE_MS));
    }
}
