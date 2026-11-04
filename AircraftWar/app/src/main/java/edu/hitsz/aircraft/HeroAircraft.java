package edu.hitsz.aircraft;

import edu.hitsz.application.ImageManager;
import edu.hitsz.application.Main;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import static edu.hitsz.config.ConfigKey.*;

import edu.hitsz.strategy.HeroRingShootStrategy;
import edu.hitsz.strategy.HeroScatterShootStrategy;
import edu.hitsz.strategy.HeroStraightShootStrategy;

import javafx.application.Platform;

import java.util.ArrayList;
import java.util.List;

/**
 * 英雄飞机，游戏玩家操控
 * @author hitsz
 */
public class HeroAircraft extends AbstractAircraft {

    //子弹威力
    private int power;

    private long fireDurationMillis;
    private final FireEffectTimer fireEffectTimer = new FireEffectTimer(Platform::runLater);
    // 一次性弹幕与持续射击策略分开，环射不会覆盖散射或重置其计时。
    private final List<BaseBullet> specialBullets = new ArrayList<>();

    // 唯一英雄机实例
    private static final HeroAircraft heroAircraft = new HeroAircraft(
        Main.WINDOW_WIDTH / 2,
        Main.WINDOW_HEIGHT - (int) ImageManager.HERO_IMAGE.getHeight(),
        0, 0, GameConfigStore.active().integer(HERO_HP));

    /*
    * 使用单例模式构建英雄机
    * 构造函数改为 private
    * 
    */ 
    private HeroAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        refreshConfiguration();
        setShootStrategy(new HeroStraightShootStrategy(power));
    }

    // 返回唯一实例
    public static HeroAircraft getInstance() {
        return heroAircraft;
    }

    @Override
    public void forward() {
        // 英雄机由鼠标控制，不通过forward函数移动
    }

    public void increaseHp(int amount) {
        if (amount > 0) {
            hp = Math.min(maxHp, hp + amount);
        }
    }

    // 恢复普通直射
    public void enableStraightShoot() {
        cancelFireEffect();
        setShootStrategy(new HeroStraightShootStrategy(power));
    }

    // 允许散射射击
    public void enableScatterShoot() {
        setShootStrategy(new HeroScatterShootStrategy(power));
        scheduleShootRecovery();
    }

    // 立即准备一圈弹幕，保持原先直射/散射策略及其剩余时限。
    public void enableRingShoot() {
        if (!notValid()) {
            specialBullets.addAll(new HeroRingShootStrategy(power).shoot(this));
        }
    }

    /** 游戏每次更新时取走一次性弹幕，不受普通射击周期限制。 */
    public List<BaseBullet> drainSpecialBullets() {
        List<BaseBullet> bullets = List.copyOf(specialBullets);
        specialBullets.clear();
        return bullets;
    }
    
    private void scheduleShootRecovery() {
        fireEffectTimer.start(fireDurationMillis, () -> {
            if (!notValid()) {
                enableStraightShoot();
            }
        });
    }

    public void pauseFireEffect() {
        fireEffectTimer.pause();
    }

    public void resumeFireEffect() {
        fireEffectTimer.resume();
    }

    /** 当前散射效果剩余毫秒；环射为瞬发，不占用该倒计时。 */
    public long getFireRemainingMillis() {
        return fireEffectTimer.remainingMillis();
    }

    public long getFireDurationMillis() {
        return fireDurationMillis;
    }

    private void refreshConfiguration() {
        GameConfig config = GameConfigStore.active();
        maxHp = config.integer(HERO_HP);
        power = config.integer(HERO_POWER);
        fireDurationMillis = config.integer(FIRE_DURATION_MS);
    }

    public void cancelFireEffect() {
        fireEffectTimer.cancel();
    }

    /** 保持同一个英雄机实例，清理上一局状态。 */
    public void resetForNewGame() {
        cancelFireEffect();
        specialBullets.clear();
        refreshConfiguration();
        hp = maxHp;
        isValid = true;
        speedX = 0;
        speedY = 0;
        setLocation(Main.WINDOW_WIDTH / 2,
                Main.WINDOW_HEIGHT - ImageManager.HERO_IMAGE.getHeight());
        enableStraightShoot();
    }
}
