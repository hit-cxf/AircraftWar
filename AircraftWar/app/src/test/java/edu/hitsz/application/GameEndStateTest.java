package edu.hitsz.application;

import edu.hitsz.aircraft.HeroAircraft;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GameEndStateTest {
    @Test
    void deathStopsBackgroundAndPlaysEndingEffectWhileKeepingTheFinalFrame() throws Exception {
        AtomicInteger callbacks = new AtomicInteger();
        AtomicReference<EffectsFixture> current = new AtomicReference<>();
        try (EffectsFixture f = new EffectsFixture(Difficulty.NORMAL, score -> {
            assertTrue(current.get().audio.backgroundStopped);
            assertFalse(current.get().audio.closed, "结束音效应自然播放，不在坠毁时关闭输出");
            callbacks.incrementAndGet();
        })) {
            current.set(f);
            HeroAircraft hero = HeroAircraft.getInstance();
            hero.enableScatterShoot();
            f.set("score", 70);
            f.effects().addExplosion(100, 200, 80);
            f.call("render");
            hero.decreaseHp(Integer.MAX_VALUE);
            f.call("checkResultAction");
            assertEquals(1, callbacks.get());
            assertFalse(f.audio.closed);
            assertTrue(f.audio.events.contains("/videos/game_over.wav"));
            assertEquals(true, f.get("gameOverFlag"));
            assertEquals(0, hero.getFireRemainingMillis());
            assertEquals(1, f.effects().explosions().size(), "最终画面保留，不提前清空特效");
            int ticks = (int) f.get("elapsedTicks");
            int y = hero.getLocationY();
            for (int i = 0; i < 20; i++) f.call("tick");
            f.call("checkResultAction");
            assertEquals(ticks, f.get("elapsedTicks"));
            assertEquals(y, hero.getLocationY());
            assertEquals(1, callbacks.get(), "结束通知只能发出一次");
            assertEquals(1, f.audio.events.stream().filter("/videos/game_over.wav"::equals).count());
            f.game.stop();
            assertTrue(f.audio.closed, "返回菜单或关闭窗口时仍需释放本局音频");
        }
    }
}
