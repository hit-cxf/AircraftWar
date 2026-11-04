package edu.hitsz.aircraft;

import edu.hitsz.application.Main;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;

import edu.hitsz.observer.PropEffectObserver;
import edu.hitsz.strategy.EnemyScatterShootStrategy;


/**
 * 王牌敌机
 * 可射击、会掉落道具
 * @author hitsz
 */
public class EliteProEnemy extends AbstractAircraft implements PropEffectObserver {
    /** 兼容旧测试/示例的默认值；实际伤害由配置快照决定。 */
    @Deprecated
    public static final int BOMB_DAMAGE = 60;
    private final GameConfig config = GameConfigStore.active();

    public EliteProEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        setShootStrategy(new EnemyScatterShootStrategy());
    }

    @Override
    public void forward() {
        if (isMovementFrozen()) {
            return;
        }
        super.forward();
        // 左右移动
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
        decreaseHp(config.integer(PRO_BOMB_DAMAGE));
    }

    @Override
    public void onFreeze() {
        movementEffect.slow(config.integer(PRO_SLOW_MS), config.value(PRO_SLOW_SCALE));
    }
}
