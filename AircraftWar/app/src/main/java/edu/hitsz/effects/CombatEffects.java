package edu.hitsz.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * 只维护表现层的时间线，不改变血量、位置、碰撞或计分。
 * 由 JavaFX 线程每帧 update，渲染器读取不可修改的快照。
 */
public final class CombatEffects {
    public static final double EXPLOSION_DURATION_SECONDS = 0.72;
    public static final double SHOCKWAVE_DURATION_SECONDS = 0.50;
    public static final double SHAKE_DURATION_SECONDS = 0.35;
    public static final double FREEZE_DURATION_SECONDS = 5.0;
    private static final double FREEZE_FADE_IN_SECONDS = 0.16;
    private static final double FREEZE_FADE_OUT_SECONDS = 1.0;

    public record Explosion(double x, double y, double size, double progress) { }

    /** size 为冲击波扩张到终点时的最大半径。 */
    public record Shockwave(double x, double y, double size, double progress) { }
    public record Hit(double x, double y, double progress) { }
    public record Recovery(double x, double y, int amount, double progress) { }

    private final List<Burst> explosions = new ArrayList<>();
    private final List<Burst> shockwaves = new ArrayList<>();
    private final List<Burst> hits = new ArrayList<>();
    private final List<Burst> recoveries = new ArrayList<>();
    private final Map<Object, Double> flashAges = new IdentityHashMap<>();
    private double damageAge = 0.45;
    private double freezeAge = -1;
    private double freezeStartOpacity;
    private double shakeAge = SHAKE_DURATION_SECONDS;

    public void triggerFreeze() {
        // 再次拾取刷新时限，并从当前透明度平滑过渡，避免突然闪成透明。
        freezeStartOpacity = freezeOpacity();
        freezeAge = 0;
    }

    public void triggerBomb(double x, double y) {
        requireFinite(x, y);
        shockwaves.add(new Burst(x, y, 420));
        shakeAge = 0;
    }

    public void addExplosion(double x, double y, double size) {
        requireFinite(x, y);
        if (!Double.isFinite(size) || size <= 0) {
            throw new IllegalArgumentException("爆炸尺寸必须是有限正数");
        }
        explosions.add(new Burst(x, y, size));
    }

    public void flash(Object target) {
        flashAges.put(java.util.Objects.requireNonNull(target), 0.0);
    }

    public double flashOpacity(Object target) {
        Double age = flashAges.get(target);
        return age == null ? 0 : Math.max(0, 1 - age / 0.14);
    }

    public void addHit(double x, double y) {
        requireFinite(x, y);
        if (hits.size() >= 128) hits.remove(0);
        hits.add(new Burst(x, y, 1));
    }

    public void heroDamaged() { damageAge = 0; }

    public double damageOpacity() { return Math.max(0, 1 - damageAge / 0.45); }

    public void addRecovery(double x, double y, int amount) {
        requireFinite(x, y);
        if (amount <= 0) return;
        if (recoveries.size() >= 16) recoveries.remove(0);
        recoveries.add(new Burst(x, y, amount));
    }

    public List<Hit> hits() {
        return hits.stream().map(b -> new Hit(b.x, b.y, b.age / 0.24)).toList();
    }

    public List<Recovery> recoveries() {
        return recoveries.stream().map(b -> new Recovery(b.x, b.y, (int) b.size, b.age / 0.9)).toList();
    }

    public void update(double deltaSeconds) {
        if (!Double.isFinite(deltaSeconds) || deltaSeconds < 0) {
            throw new IllegalArgumentException("特效时间增量必须是有限非负数");
        }
        updateBursts(explosions, deltaSeconds, EXPLOSION_DURATION_SECONDS);
        updateBursts(shockwaves, deltaSeconds, SHOCKWAVE_DURATION_SECONDS);
        updateBursts(hits, deltaSeconds, 0.24);
        updateBursts(recoveries, deltaSeconds, 0.9);
        flashAges.replaceAll((object, age) -> age + deltaSeconds);
        flashAges.values().removeIf(age -> age >= 0.14);
        damageAge = Math.min(0.45, damageAge + deltaSeconds);
        shakeAge = Math.min(SHAKE_DURATION_SECONDS, shakeAge + deltaSeconds);
        if (freezeAge >= 0) {
            freezeAge += deltaSeconds;
            if (freezeAge >= FREEZE_DURATION_SECONDS) {
                freezeAge = -1;
                freezeStartOpacity = 0;
            }
        }
    }

    public boolean hasExplosions() {
        return !explosions.isEmpty();
    }

    public List<Explosion> explosions() {
        return explosions.stream().map(burst -> new Explosion(burst.x, burst.y,
                burst.size, burst.age / EXPLOSION_DURATION_SECONDS)).toList();
    }

    public List<Shockwave> shockwaves() {
        return shockwaves.stream().map(burst -> new Shockwave(burst.x, burst.y,
                burst.size, burst.age / SHOCKWAVE_DURATION_SECONDS)).toList();
    }

    public double freezeOpacity() {
        if (freezeAge < 0) {
            return 0;
        }
        if (freezeAge < FREEZE_FADE_IN_SECONDS) {
            return freezeStartOpacity + (1 - freezeStartOpacity)
                    * freezeAge / FREEZE_FADE_IN_SECONDS;
        }
        return Math.min(1, (FREEZE_DURATION_SECONDS - freezeAge) / FREEZE_FADE_OUT_SECONDS);
    }

    public double shakeX() {
        double amplitude = shakeAmplitude();
        return amplitude == 0 ? 0 : amplitude * Math.sin(shakeAge * 91 + 1.2);
    }

    public double shakeY() {
        double amplitude = shakeAmplitude();
        return amplitude == 0 ? 0 : amplitude * Math.cos(shakeAge * 117 + 0.4);
    }

    public void clear() {
        explosions.clear();
        shockwaves.clear();
        hits.clear();
        recoveries.clear();
        flashAges.clear();
        damageAge = 0.45;
        freezeAge = -1;
        freezeStartOpacity = 0;
        shakeAge = SHAKE_DURATION_SECONDS;
    }

    private double shakeAmplitude() {
        double remaining = 1 - shakeAge / SHAKE_DURATION_SECONDS;
        return 8 * remaining * remaining;
    }

    private static void updateBursts(List<Burst> bursts, double delta, double duration) {
        bursts.forEach(burst -> burst.age += delta);
        bursts.removeIf(burst -> burst.age >= duration);
    }

    private static void requireFinite(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)) {
            throw new IllegalArgumentException("特效坐标必须是有限数值");
        }
    }

    private static final class Burst {
        final double x;
        final double y;
        final double size;
        double age;

        Burst(double x, double y, double size) {
            this.x = x;
            this.y = y;
            this.size = size;
        }
    }
}
