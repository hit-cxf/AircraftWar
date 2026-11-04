package edu.hitsz.aircraft;

import edu.hitsz.application.Main;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;

import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.observer.PropEffectObserver;
import edu.hitsz.strategy.BossPhaseTwoShootStrategy;
import edu.hitsz.strategy.EnemyRingShootStrategy;

import java.util.List;

/**
 * Boss 敌机
 * 可射击、会掉落道具
 * @author hitsz
 */
public class BossEnemy extends AbstractAircraft implements PropEffectObserver {

    private final GameConfig config = GameConfigStore.active();
    private final double phaseRatio = config.value(BOSS_PHASE_RATIO);
    private final double phaseChargeSeconds = config.value(BOSS_CHARGE_MS) / 1000.0;
    private final BossPhaseTwoShootStrategy phaseTwoStrategy = new BossPhaseTwoShootStrategy();

    private boolean secondPhase;
    private double chargeRemainingSeconds;

    public BossEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        setShootStrategy(new EnemyRingShootStrategy());
    }

    @Override
    public void decreaseHp(int decrease) {
        super.decreaseHp(decrease);
        if (notValid()) {
            chargeRemainingSeconds = 0.0;
        } else if (!secondPhase && hp <= maxHp * phaseRatio) {
            secondPhase = true;
            chargeRemainingSeconds = phaseChargeSeconds;
            setShootStrategy(phaseTwoStrategy);
        }
    }

    /** 仅由游戏逻辑时钟推进，暂停时无需调用，不累计现实时间。 */
    public void updateCombat(double deltaSeconds) {
        if (notValid()) {
            chargeRemainingSeconds = 0.0;
            return;
        }
        if (!Double.isFinite(deltaSeconds) || deltaSeconds <= 0.0) {
            return;
        }
        chargeRemainingSeconds = Math.max(0.0, chargeRemainingSeconds - deltaSeconds);
        // 避免固定步长相减留下极小余数，导致蓄力多持续一帧。
        if (chargeRemainingSeconds < 1e-9) {
            chargeRemainingSeconds = 0.0;
        }
    }

    public boolean isSecondPhase() {
        return secondPhase;
    }

    public boolean isCharging() {
        return !notValid() && chargeRemainingSeconds > 0.0;
    }

    /** 未进入第二阶段或已死亡时为 0，完成蓄力后为 1。 */
    public double chargeProgress() {
        if (!secondPhase || notValid()) {
            return 0.0;
        }
        if (phaseChargeSeconds <= 0) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0,
                1.0 - chargeRemainingSeconds / phaseChargeSeconds));
    }

    @Override
    public List<BaseBullet> shoot() {
        if (notValid() || isCharging()) {
            return List.of();
        }
        return super.shoot();
    }

    @Override
    public void forward() {
        int halfWidth = getWidth() / 2;
        super.forward();
        if (locationX <= halfWidth) {
            locationX = halfWidth;
            speedX = Math.abs(speedX);
        } else if (locationX >= Main.WINDOW_WIDTH - halfWidth) {
            locationX = Main.WINDOW_WIDTH - halfWidth;
            speedX = -Math.abs(speedX);
        }
    }

    @Override
    public void onBomb() {
        // Boss 不受炸弹影响。
    }

    @Override
    public void onFreeze() {
        // Boss 不受冰冻影响。
    }
}
