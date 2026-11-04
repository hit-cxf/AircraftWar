package edu.hitsz.bullet;

import edu.hitsz.observer.PropEffectObserver;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;


/**
 * 敌机子弹
 * @Author hitsz
 */
public class EnemyBullet extends BaseBullet implements PropEffectObserver {
    private final GameConfig config = GameConfigStore.active();

    public EnemyBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY, power);
    }

    @Override
    public void forward() {
        if (!isMovementFrozen()) {
            super.forward();
        }
    }

    @Override
    public void onBomb() {
        vanish();
    }

    @Override
    public void onFreeze() {
        movementEffect.freeze(config.integer(BULLET_FREEZE_MS));
    }

}
