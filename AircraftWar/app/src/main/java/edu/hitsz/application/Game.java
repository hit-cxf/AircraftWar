package edu.hitsz.application;

import edu.hitsz.aircraft.*;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.factory.PropFactory;
import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.prop.AbstractProp;
import edu.hitsz.prop.AbstractEffectProp;
import edu.hitsz.prop.BombProp;
import edu.hitsz.prop.FreezeProp;
import edu.hitsz.effects.CombatEffects;
import edu.hitsz.effects.CombatEffectsRenderer;
import edu.hitsz.effects.GameHudRenderer;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;
import edu.hitsz.config.ConfigKey;
import static edu.hitsz.config.ConfigKey.*;
import edu.hitsz.prop.PropType;
import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;

import edu.hitsz.factory.EnemyFactory;
import edu.hitsz.factory.BossEnemyFactory;
import edu.hitsz.factory.EliteEnemyFactory;
import edu.hitsz.factory.ElitePlusEnemyFactory;
import edu.hitsz.factory.EliteProEnemyFactory;
import edu.hitsz.factory.MobEnemyFactory;
import edu.hitsz.audio.AudioManager;

import java.util.LinkedList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * 游戏主面板，游戏启动
 * @author hitsz
 */
public abstract class Game extends Pane {

    private int backGroundTop = 0;

    //游戏逻辑更新间隔
    private final int timeInterval = 40;
    protected final GameConfig config;

    private final HeroAircraft heroAircraft;
    private final IntConsumer onGameOver;
    private final List<AbstractAircraft> enemyAircrafts;
    private final List<BaseBullet> heroBullets;
    private final List<BaseBullet> enemyBullets;
    private final List<AbstractProp> props;

    private GameParameters parameters;
    private int enemySpawnCounter;
    private int heroShootCounter;
    private int enemyShootCounter;
    private int elapsedTicks;
    private int difficultyLevel;
    private int bossSpawnCount;
    private int nextBossScore;
    private BossEnemy activeBoss;
    private boolean bossIncoming;
    private int bossWarningTicks;
    private final int bossWarningDurationTicks;
    private final Difficulty difficulty;

    //当前玩家分数
    private int score = 0;

    //游戏结束标志
    private boolean gameOverFlag = false;
    private boolean paused;
    private boolean pausedByFocusLoss;
    private double visualTime;

    //画布和画笔
    private final Canvas canvas;
    private final GraphicsContext gc;

    //JavaFX 提供的动画定时器
    private AnimationTimer timer;
    // 保留 40ms 固定逻辑步；每次绘制前最多补算 5 步（200ms）。
    private final FixedStepClock frameClock = new FixedStepClock(timeInterval * 1_000_000L, 5);

    // 背景
    private final Image backgroundImage;

    // BGM
    private final AudioManager audioManager;
    private final CombatEffects combatEffects = new CombatEffects();
    private final CombatEffectsRenderer effectsRenderer = new CombatEffectsRenderer();
    private final GameHudRenderer hudRenderer = new GameHudRenderer();
    private boolean bombExplodedThisTick;

    public Game(Difficulty difficulty, IntConsumer onGameOver) {
        this(difficulty, onGameOver, new AudioManager());
    }

    /** 支持在集成测试中替换音频输出，不影响正式构造入口。 */
    protected Game(Difficulty difficulty, IntConsumer onGameOver, AudioManager audioManager) {
        this.audioManager = java.util.Objects.requireNonNull(audioManager);
        this.config = GameConfigStore.activate(difficulty);
        this.bossWarningDurationTicks = (int) Math.ceil(config.value(BOSS_WARNING_MS) / timeInterval);
        this.onGameOver = onGameOver;
        this.difficulty = difficulty;

        this.backgroundImage = switch (difficulty) {
            case EASY -> ImageManager.BACKGROUND_IMAGE;
            case NORMAL -> ImageManager.NORMAL_BACKGROUND_IMAGE;
            case HARD -> ImageManager.HARD_BACKGROUND_IMAGE;
            case CUSTOM -> ImageManager.NORMAL_BACKGROUND_IMAGE;
        };

        setPrefSize(Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
        setPickOnBounds(true);

        canvas = new Canvas(Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
        gc = canvas.getGraphicsContext2D();
        getChildren().add(canvas);

        heroAircraft = HeroAircraft.getInstance();
        heroAircraft.resetForNewGame();

        enemyAircrafts = new LinkedList<>();
        heroBullets = new LinkedList<>();
        enemyBullets = new LinkedList<>();
        props = new LinkedList<>();

        // 启动英雄机鼠标监听（移动/拖拽即跟随）
        new HeroController(this, heroAircraft, () -> !paused && !gameOverFlag);
        setFocusTraversable(true);
        // HUD 为固定操作区，鼠标经过它时不把英雄机传送到屏幕顶部。
        addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (event.getY() <= 96) event.consume();
        });
        addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (event.getY() <= 96) event.consume();
        });
        addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE && !gameOverFlag) {
                if (paused) resume(); else pause();
                event.consume();
            }
        });
        addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
            requestFocus();
            if (gameOverFlag) return;
            if (paused && hudRenderer.isResumeButton(event.getX(), event.getY(),
                    Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT)) {
                resume();
            } else if (!paused && hudRenderer.isPauseButton(event.getX(), event.getY(), Main.WINDOW_WIDTH)) {
                pause();
            }
        });

    }

    /**
     * 游戏启动入口，执行游戏逻辑
     */
    public final void action() {
        if (timer != null || gameOverFlag) {
            return;
        }
        // 模板方法：初始化可变参数 -> 启动公共循环 -> 启动背景音乐。
        parameters = parametersAtLevel(0);
        nextBossScore = parameters.bossThreshold();
        printDifficulty();
        startGameLoop();
        audioManager.playBackground("/videos/bgm.wav");
    }

    protected abstract GameParameters parametersAtLevel(int level);

    protected abstract int bossHpForSpawn(int spawnNumber);

    private void startGameLoop() {
        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                FixedStepClock.Frame frame = frameClock.advance(now, !paused && !gameOverFlag);
                if (!paused && !gameOverFlag) {
                    double dt = Math.min(frame.elapsedSeconds(), 0.1);
                    combatEffects.update(dt);
                    visualTime += dt;
                }
                for (int step = 0; step < frame.steps() && !gameOverFlag && !paused; step++) {
                    tick();
                }
                //重绘界面
                render();
            }
        };
        timer.start();
    }

    private void tick() {
        if (paused || gameOverFlag) return;
        bombExplodedThisTick = false;
        heroBullets.addAll(heroAircraft.drainSpecialBullets());
        updateDifficulty();
        spawnEnemy();
        spawnBoss();
        if (activeBoss != null) activeBoss.updateCombat(timeInterval / 1000.0);

        // 飞机射出子弹
        shootAction();
        // 子弹移动
        bulletsMoveAction();
        // 飞机移动
        aircraftMoveAction();
        // 道具移动
        propsMoveAction();
        // 撞击检测
        crashCheckAction();
        // 拾取当帧收取环射，不等待普通射击周期，也不覆盖持续弹道。
        heroBullets.addAll(heroAircraft.drainSpecialBullets());
        // 后处理
        postProcessAction();
        // 游戏结束检查
        checkResultAction();
    }

    //***********************
    //      Action 各部分
    //***********************
    private void updateDifficulty() {
        elapsedTicks++;
        int progressionTicks = (int) Math.ceil(config.value(LEVEL_DURATION_MS) / timeInterval);
        if (elapsedTicks % progressionTicks == 0) {
            parameters = parametersAtLevel(++difficultyLevel);
            printDifficulty();
        }
        if (difficulty == Difficulty.CUSTOM
                || config.value(ConfigKey.valueOf(difficulty.name() + "_SPEED_RATE")) > 0) {
            parameters = config.parameters(difficulty, difficultyLevel, score);
            for (AbstractAircraft enemy : enemyAircrafts) {
                if (!(enemy instanceof BossEnemy)) {
                    enemy.setVelocity(enemy.getVelocityX(), parameters.enemySpeed());
                }
            }
        }
    }

    private void printDifficulty() {
        System.out.printf("[%s] 阶段%d：敌机上限%d，生成周期%dms，速度%.2f，血量倍率%.2f，英雄射击%dms，敌机射击%dms%n",
                difficulty.displayName(), difficultyLevel, parameters.enemyMax(),
                parameters.spawnCycle() * timeInterval, parameters.enemySpeed(), parameters.hpMultiplier(),
                parameters.heroShootCycle() * timeInterval, parameters.enemyShootCycle() * timeInterval);
    }

    private void spawnEnemy() {
        if (++enemySpawnCounter < parameters.spawnCycle()) {
            return;
        }
        enemySpawnCounter = 0;
        long ordinaryCount = enemyAircrafts.stream().filter(enemy -> !(enemy instanceof BossEnemy)).count();
        if (ordinaryCount >= parameters.enemyMax()) {
            return;
        }
        double chance = Math.random();
        EnemyFactory factory;
        Image image;
        int hp;
        int speedX = 0;
        if (chance < parameters.proChance()) {
            factory = new EliteProEnemyFactory();
            image = ImageManager.ELITE_PRO_ENEMY_IMAGE;
            hp = config.integer(PRO_HP);
            speedX = (Math.random() < 0.5 ? 1 : -1) * config.integer(ENEMY_HORIZONTAL_SPEED);
        } else if (chance < parameters.proChance() + parameters.plusChance()) {
            factory = new ElitePlusEnemyFactory();
            image = ImageManager.ELITE_PLUS_ENEMY_IMAGE;
            hp = config.integer(PLUS_HP);
            speedX = (Math.random() < 0.5 ? 1 : -1) * config.integer(ENEMY_HORIZONTAL_SPEED);
        } else if (chance < parameters.proChance() + parameters.plusChance() + parameters.eliteChance()) {
            factory = new EliteEnemyFactory();
            image = ImageManager.ELITE_ENEMY_IMAGE;
            hp = config.integer(ELITE_HP);
        } else {
            factory = new MobEnemyFactory();
            image = ImageManager.MOB_ENEMY_IMAGE;
            hp = config.integer(MOB_HP);
        }
        int halfWidth = (int) image.getWidth() / 2;
        int x = halfWidth + (int) (Math.random() * Math.max(1, Main.WINDOW_WIDTH - 2 * halfWidth));
        AbstractAircraft enemy = factory.create(x, Math.max(1, (int) image.getHeight() / 2),
                speedX, (int) Math.ceil(parameters.enemySpeed()), (int) Math.ceil(hp * parameters.hpMultiplier()));
        enemy.setVelocity(speedX, parameters.enemySpeed());
        enemyAircrafts.add(enemy);
    }

    private void spawnBoss() {
        if (parameters.bossThreshold() == 0 || activeBoss != null) {
            return;
        }
        if (!bossIncoming) {
            if (score >= nextBossScore) {
                bossIncoming = true;
                bossWarningTicks = bossWarningDurationTicks;
            }
            return;
        }
        if (--bossWarningTicks > 0) return;
        bossIncoming = false;
        int hp = bossHpForSpawn(++bossSpawnCount);
        activeBoss = (BossEnemy) new BossEnemyFactory().create(Main.WINDOW_WIDTH / 2, 230,
                config.integer(BOSS_HORIZONTAL_SPEED), 0, hp);
        enemyAircrafts.add(activeBoss);
        nextBossScore += parameters.bossThreshold();
        audioManager.playBackground("/videos/bgm_boss.wav");
        System.out.printf("[%s] 第%d次 Boss，血量%d，下次分数阈值%d%n",
                difficulty.displayName(), bossSpawnCount, hp, nextBossScore);
    }

    private void shootAction() {
        if (++heroShootCounter >= parameters.heroShootCycle()) {
            heroShootCounter = 0;
            heroBullets.addAll(heroAircraft.shoot());
        }
        if (++enemyShootCounter >= parameters.enemyShootCycle()) {
            enemyShootCounter = 0;
            for (AbstractAircraft enemy : enemyAircrafts) {
                if (!enemy.notValid()) {
                    enemyBullets.addAll(enemy.shoot());
                }
            }
        }
    }

    private void bulletsMoveAction() {
        for (BaseBullet bullet : heroBullets) {
            bullet.forward();
        }
        for (BaseBullet bullet : enemyBullets) {
            bullet.forward();
        }
    }

    private void aircraftMoveAction() {
        for (AbstractAircraft enemyAircraft : enemyAircrafts) {
            enemyAircraft.forward();
        }
    }

    private void propsMoveAction() {
        for (AbstractProp prop : props) {
            prop.forward();
        } 
    }

    /**
     * 碰撞检测：
     * 1. 敌机攻击英雄
     * 2. 英雄攻击/撞击敌机
     * 3. 英雄获得补给
     */
    private void crashCheckAction() {
        // 敌机子弹攻击英雄
        for (BaseBullet bullet: enemyBullets) {
            if(!bullet.notValid() && heroAircraft.crash(bullet)) {
                heroAircraft.decreaseHp(bullet.getPower());
                combatEffects.heroDamaged();
                combatEffects.addHit(heroAircraft.getLocationX(), heroAircraft.getLocationY());
                bullet.vanish();
            }
        }

        // 英雄子弹攻击敌机
        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) {
                continue;
            }
            for (AbstractAircraft enemyAircraft : enemyAircrafts) {
                if (enemyAircraft.notValid()) {
                    // 已被其他子弹击毁的敌机，不再检测
                    // 避免多个子弹重复击毁同一敌机的判定
                    continue;
                }
                if (enemyAircraft.crash(bullet)) {
                    // 播放撞击音效
                    audioManager.playEffect("/videos/bullet_hit.wav");
                    // 敌机撞击到英雄机子弹
                    // 敌机损失一定生命值
                    enemyAircraft.decreaseHp(bullet.getPower());
                    combatEffects.flash(enemyAircraft);
                    combatEffects.addHit(bullet.getLocationX(), bullet.getLocationY());
                    bullet.vanish();
                    if (enemyAircraft.notValid()) {
                        // 获得分数，产生道具补给
                        score += config.integer(KILL_SCORE);
                        if (enemyAircraft instanceof BossEnemy) {
                            PropType[] types = PropType.values();
                            int count = config.integer(BOSS_DROP_COUNT);
                            for (int i = 0; i < count; i++) {
                                PropType type = types[(int) (Math.random() * types.length)];
                                props.add(PropFactory.create(
                                    type,
                                    Math.max(25, Math.min(Main.WINDOW_WIDTH - 25,
                                            enemyAircraft.getLocationX() + (int) ((i - (count - 1) / 2.0) * 50))),
                                    enemyAircraft.getLocationY()
                                ));
                            }
                        } else if ((enemyAircraft instanceof EliteEnemy 
                                || enemyAircraft instanceof ElitePlusEnemy
                                || enemyAircraft instanceof EliteProEnemy)
                                && Math.random() < config.value(DROP_CHANCE)) {
                            int x = enemyAircraft.getLocationX();
                            int y = enemyAircraft.getLocationY();
                            
                            PropType[] availableTypes;
                            if (enemyAircraft instanceof EliteProEnemy) {
                                availableTypes = PropType.values();
                            } else if (enemyAircraft instanceof ElitePlusEnemy) {
                                availableTypes = new PropType[]{
                                    PropType.BLOOD, PropType.FIRE, PropType.SUPER_FIRE, PropType.BOMB
                                };
                            } else {
                                availableTypes = new PropType[]{
                                    PropType.BLOOD, PropType.FIRE, PropType.SUPER_FIRE
                                };
                            }

                            PropType type = availableTypes[(int) (Math.random() * availableTypes.length)];
                            props.add(PropFactory.create(type, x, y));
                        }
                    }
                    break; // 一颗子弹只命中一架敌机。
                }
            }
        }

        // 英雄机 与 敌机 相撞，均损毁
        for(AbstractAircraft enemyAircraft: enemyAircrafts) {
            if (!enemyAircraft.notValid()) {
                if (enemyAircraft.crash(heroAircraft) || heroAircraft.crash(enemyAircraft)) {
                    enemyAircraft.vanish();
                    heroAircraft.decreaseHp(Integer.MAX_VALUE);
                    combatEffects.heroDamaged();
                }
            }
        }
    
        // 我方获得道具，道具生效
        for(AbstractProp prop: props) {
            if (!prop.notValid() && !heroAircraft.notValid()) {
                if(heroAircraft.crash(prop)) {
                    prop.vanish();
                    int previousHp = heroAircraft.getHp();
                    if (prop instanceof AbstractEffectProp effectProp) {
                        // 道具击毁只计分，不进入子弹击毁时的掉落流程。
                        int destroyed = PropEffectAction.activate(effectProp, heroAircraft,
                                enemyAircrafts, enemyBullets);
                        score += destroyed * config.integer(KILL_SCORE);
                    } else {
                        prop.activate(heroAircraft);
                    }
                    int recovered = heroAircraft.getHp() - previousHp;
                    if (recovered > 0) {
                        combatEffects.addRecovery(heroAircraft.getLocationX(),
                                heroAircraft.getLocationY() - 35, recovered);
                    }
                    audioManager.playEffect("/videos/get_supply.wav");
                    if (prop instanceof BombProp) {
                        bombExplodedThisTick = true;
                        combatEffects.triggerBomb(heroAircraft.getLocationX(), heroAircraft.getLocationY());
                        combatEffects.addExplosion(heroAircraft.getLocationX(), heroAircraft.getLocationY(), 200);
                        audioManager.playEffect("/videos/bomb_explosion.wav");
                    } else if (prop instanceof FreezeProp) {
                        combatEffects.triggerFreeze();
                    }
                }
            }
        }
    }

    public boolean isPaused() { return paused; }

    /** 暂停保留所有战斗状态，恢复时不补算停留在其他窗口的时间。 */
    public void pause() { setPaused(true, false); }

    public void pauseForFocusLoss() { setPaused(true, true); }

    public void resume() { setPaused(false, false); }

    private void setPaused(boolean value, boolean focusLoss) {
        if (gameOverFlag || paused == value) return;
        paused = value;
        pausedByFocusLoss = value && focusLoss;
        frameClock.reset();
        for (AbstractAircraft enemy : enemyAircrafts) enemy.setEffectsPaused(value);
        for (BaseBullet bullet : enemyBullets) bullet.setEffectsPaused(value);
        if (value) heroAircraft.pauseFireEffect(); else heroAircraft.resumeFireEffect();
        audioManager.setPaused(value);
        render();
    }

    /**
     * 后处理：
     * 1. 删除无效的子弹
     * 2. 删除无效的敌机
     * 3. 删除无效的道具
     */
    private void postProcessAction() {
        boolean hasDestroyedEnemy = false;
        boolean bossDestroyed = false;
        for (AbstractAircraft enemy : enemyAircrafts) {
            // 仅为屏幕内坠毁生成特效，正常飞出下边界不算爆炸。
            if (enemy.notValid() && enemy.getLocationY() >= 0
                    && enemy.getLocationY() < Main.WINDOW_HEIGHT) {
                double size = Math.max(72, Math.min(350,
                        Math.max(enemy.getWidth(), enemy.getHeight()) * 1.7));
                combatEffects.addExplosion(enemy.getLocationX(), enemy.getLocationY(), size);
                hasDestroyedEnemy = true;
                bossDestroyed |= enemy instanceof BossEnemy;
            }
        }
        if (hasDestroyedEnemy && !bombExplodedThisTick) {
            audioManager.playEffect(bossDestroyed ? "/videos/bomb_explosion.wav"
                    : "/videos/enemy_explosion.wav");
        }
        if (activeBoss != null && activeBoss.notValid()) {
            activeBoss = null;
            if (heroAircraft.getHp() > 0) {
                audioManager.playBackground("/videos/bgm.wav");
            }
        }
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyAircrafts.removeIf(AbstractFlyingObject::notValid);
        props.removeIf(AbstractFlyingObject::notValid);
    }


    /**
     * 检查游戏结束，冻结最终帧并通知界面先显示姓名输入层。
     */
    private void checkResultAction(){
        // 游戏结束检查英雄机是否存活
        if (!gameOverFlag && heroAircraft.getHp() <= 0) {
            gameOverFlag = true;
            if (timer != null) timer.stop();
            audioManager.stopBackground();
            audioManager.playEffect("/videos/game_over.wav");
            System.out.println("Game Over!");
            heroAircraft.cancelFireEffect();
            // 输入层出现前先绘制最终状态；关闭输入层之前不再推进动画。
            render();
            onGameOver.accept(score);
        }
    }

    /** 主窗口关闭或返回菜单时调用；必须在 JavaFX 线程执行。 */
    public void stop() {
        gameOverFlag = true;
        if (timer != null) {
            timer.stop();
        }
        heroAircraft.cancelFireEffect();
        combatEffects.clear();
        audioManager.close();
    }

    //***********************
    //      Render 各部分
    //***********************

    private void render() {
        // 背景滚动
        gc.clearRect(0, 0, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
        gc.setFill(Color.rgb(10, 23, 39));
        gc.fillRect(0, 0, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
        gc.save();
        // 抖动只作用于世界绘制，鼠标坐标、碰撞和 HUD 保持稳定。
        gc.translate(combatEffects.shakeX(), combatEffects.shakeY());

        gc.drawImage(backgroundImage, 0, backGroundTop - Main.WINDOW_HEIGHT);
        gc.drawImage(backgroundImage, 0, backGroundTop);
        if (!paused && !gameOverFlag) this.backGroundTop += 1;
        if (this.backGroundTop >= Main.WINDOW_HEIGHT) {
            this.backGroundTop = 0;
        }

        effectsRenderer.paintFreezeMask(gc, combatEffects.freezeOpacity(),
                Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);

        // 先绘制子弹，后绘制飞机
        paintImageWithPositionRevised(enemyBullets);
        paintImageWithPositionRevised(heroBullets);
        paintImageWithPositionRevised(enemyAircrafts);
        if (activeBoss != null) effectsRenderer.paintBossCharge(gc, activeBoss, visualTime);

        // 绘制道具
        paintImageWithPositionRevised(props);

        effectsRenderer.paintExplosions(gc, combatEffects);
        effectsRenderer.paintHits(gc, combatEffects);

        // 绘制英雄机
        gc.drawImage(
                ImageManager.HERO_IMAGE,
                heroAircraft.getLocationX() - ImageManager.HERO_IMAGE.getWidth() / 2.0,
                heroAircraft.getLocationY() - ImageManager.HERO_IMAGE.getHeight() / 2.0
        );

        gc.restore();
        effectsRenderer.paintDamageOverlay(gc, combatEffects, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
        paintScoreAndLife();
        if (bossIncoming) {
            hudRenderer.paintBossWarning(gc, 1 - bossWarningTicks / (double) bossWarningDurationTicks,
                    Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
        }
        if (paused) hudRenderer.paintPause(gc, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT, pausedByFocusLoss);
    }

    private void paintImageWithPositionRevised(List<? extends AbstractFlyingObject> objects) {
        if (objects.isEmpty()) {
            return;
        }

        for (AbstractFlyingObject object : objects) {
            Image image = object.getImage();
            if (image == null) {
                throw new IllegalStateException(objects.getClass().getName() + " has object without image: " + object.getClass().getName());
            }
            gc.drawImage(
                    image,
                    object.getLocationX() - image.getWidth() / 2.0,
                    object.getLocationY() - image.getHeight() / 2.0
            );
            effectsRenderer.paintHitFlash(gc, object, combatEffects.flashOpacity(object));
            effectsRenderer.paintFrost(gc, object);
        }
    }

    private void paintScoreAndLife() {
        int affected = 0;
        boolean permanent = false;
        for (AbstractAircraft enemy : enemyAircrafts) {
            if (!enemy.notValid() && enemy.hasMovementEffect()) {
                affected++;
                permanent |= enemy instanceof MobEnemy;
            }
        }
        hudRenderer.paint(gc, score, difficulty, heroAircraft, activeBoss, affected,
                permanent, visualTime, Main.WINDOW_WIDTH, Main.WINDOW_HEIGHT);
    }

}
