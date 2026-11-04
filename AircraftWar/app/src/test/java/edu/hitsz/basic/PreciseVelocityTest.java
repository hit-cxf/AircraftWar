package edu.hitsz.basic;

import edu.hitsz.aircraft.ElitePlusEnemy;
import edu.hitsz.application.Main;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PreciseVelocityTest {
    @Test
    void fractionsAccumulateInBothDirectionsWithoutRoundingTheVelocity() {
        Probe object = new Probe();
        object.setVelocity(.2, -.2);
        advance(object, 4);
        assertEquals(100, object.getLocationX());
        assertEquals(100, object.getLocationY());
        advance(object, 1);
        assertEquals(101, object.getLocationX());
        assertEquals(99, object.getLocationY());
        assertEquals(.2, object.getVelocityX());
        assertEquals(-.2, object.getVelocityY());

        object.setVelocity(-.2, .2);
        advance(object, 5);
        assertEquals(100, object.getLocationX());
        assertEquals(100, object.getLocationY());
        assertEquals(-.2, object.getVelocityX());
        assertEquals(.2, object.getVelocityY());
    }

    @Test
    void elitePlusBouncesAtBothEdgesAndKeepsItsPreciseSpeed() {
        var enemy = new FixedWidthElitePlus();
        int rightEdge = Main.WINDOW_WIDTH - enemy.getWidth() / 2;
        enemy.setLocation(rightEdge - 1, 100);
        enemy.setVelocity(.2, 0);
        advance(enemy, 5);
        assertEquals(rightEdge, enemy.getLocationX());
        assertEquals(-.2, enemy.getVelocityX());
        advance(enemy, 5);
        assertEquals(rightEdge - 1, enemy.getLocationX());

        int leftEdge = enemy.getWidth() / 2;
        enemy.setLocation(leftEdge + 1, 100);
        enemy.setVelocity(-.2, 0);
        advance(enemy, 5);
        assertEquals(leftEdge, enemy.getLocationX());
        assertEquals(.2, enemy.getVelocityX());
        advance(enemy, 5);
        assertEquals(leftEdge + 1, enemy.getLocationX());
    }

    @Test
    void temporaryEffectsPreserveFractionalVelocityAndPendingDisplacement() throws Exception {
        AtomicLong clock = new AtomicLong();
        Probe object = new Probe();
        MovementEffect effect = new MovementEffect(clock::get);
        // 替换时钟驱动的效果对象，测试不依赖线程调度或现实时间流逝。
        Field effectField = AbstractFlyingObject.class.getDeclaredField("movementEffect");
        effectField.setAccessible(true);
        effectField.set(object, effect);
        object.setVelocity(.2, -.2);
        advance(object, 2);
        effect.freeze(1000);
        advance(object, 20);
        assertEquals(100, object.getLocationX());
        assertEquals(100, object.getLocationY());
        assertEquals(.2, object.getVelocityX());
        assertEquals(-.2, object.getVelocityY());
        clock.set(1_000_000_000L);
        advance(object, 3);
        assertEquals(101, object.getLocationX());
        assertEquals(99, object.getLocationY());

        object.setLocation(100, 100);
        effect.slow(1000, .5);
        advance(object, 10);
        assertEquals(101, object.getLocationX());
        assertEquals(99, object.getLocationY());
        assertEquals(.2, object.getVelocityX());
        assertEquals(-.2, object.getVelocityY());
        clock.set(2_000_000_000L);
        advance(object, 5);
        assertEquals(102, object.getLocationX());
        assertEquals(98, object.getLocationY());
    }

    private static void advance(AbstractFlyingObject object, int ticks) {
        for (int i = 0; i < ticks; i++) object.forward();
    }

    private static class Probe extends AbstractFlyingObject {
        Probe() { super(100, 100, 0, 0); }
    }

    private static class FixedWidthElitePlus extends ElitePlusEnemy {
        FixedWidthElitePlus() { super(200, 100, 0, 0, 100); }
        @Override public int getWidth() { return 80; }
    }
}
