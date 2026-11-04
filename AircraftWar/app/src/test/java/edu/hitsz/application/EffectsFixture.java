package edu.hitsz.application;

import edu.hitsz.aircraft.*;
import edu.hitsz.audio.AudioManager;
import edu.hitsz.bullet.BaseBullet;
import edu.hitsz.effects.CombatEffects;
import edu.hitsz.prop.AbstractProp;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/** 测试/预览使用真实游戏逻辑，屏蔽真实音频设备。 */
final class EffectsFixture implements AutoCloseable {
    final SilentAudio audio = new SilentAudio();
    final Game game;

    EffectsFixture() throws Exception {
        this(Difficulty.NORMAL);
    }

    EffectsFixture(Difficulty difficulty) throws Exception {
        this(difficulty, score -> {});
    }

    EffectsFixture(Difficulty difficulty, IntConsumer onGameOver) throws Exception {
        game = new Game(difficulty, onGameOver, audio) {
            protected GameParameters parametersAtLevel(int level) {
                return new GameParameters(5, .2, .1, .05, 100000, 100000, 100000, 8, 1, 0);
            }
            protected int bossHpForSpawn(int number) { return 300; }
        };
        set("parameters", game.parametersAtLevel(0));
        HeroAircraft.getInstance().setLocation(256, 670);
    }

    Object get(String name) throws Exception {
        Field field = Game.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(game);
    }
    void set(String name, Object value) throws Exception {
        Field field = Game.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(game, value);
    }
    void call(String name) throws Exception {
        Method method = Game.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(game);
    }
    @SuppressWarnings("unchecked") List<AbstractAircraft> enemies() throws Exception {
        return (List<AbstractAircraft>) get("enemyAircrafts");
    }
    @SuppressWarnings("unchecked") List<BaseBullet> heroBullets() throws Exception {
        return (List<BaseBullet>) get("heroBullets");
    }
    @SuppressWarnings("unchecked") List<BaseBullet> enemyBullets() throws Exception {
        return (List<BaseBullet>) get("enemyBullets");
    }
    @SuppressWarnings("unchecked") List<AbstractProp> props() throws Exception {
        return (List<AbstractProp>) get("props");
    }
    CombatEffects effects() throws Exception { return (CombatEffects) get("combatEffects"); }
    public void close() { game.stop(); }

    static final class SilentAudio extends AudioManager {
        final List<String> events = new ArrayList<>();
        boolean closed;
        boolean backgroundStopped;
        @Override public synchronized void playEffect(String path) { events.add(path); }
        @Override public synchronized void playBackground(String path) { }
        @Override public synchronized void stopBackground() { backgroundStopped = true; }
        @Override public synchronized void close() { closed = true; super.close(); }
    }
}
