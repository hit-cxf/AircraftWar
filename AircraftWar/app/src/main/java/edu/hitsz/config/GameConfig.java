package edu.hitsz.config;

import edu.hitsz.application.Difficulty;
import edu.hitsz.application.GameParameters;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** 一份经过完整校验的不可变玩法配置；一局游戏可持有该快照。 */
public final class GameConfig {
    private static final int LOGIC_STEP_MS = 40;
    private static final String[] MODES = {"EASY", "NORMAL", "HARD", "CUSTOM"};
    private final Map<ConfigKey, Double> values;

    private GameConfig(EnumMap<ConfigKey, Double> values) {
        this.values = Collections.unmodifiableMap(values);
    }

    /** 需要包含所有字段；调用方可先复制 defaults().values() 再修改。 */
    public static GameConfig from(Map<ConfigKey, Double> source) {
        Objects.requireNonNull(source, "配置不能为空");
        if (source.keySet().stream().anyMatch(Objects::isNull)) throw new IllegalArgumentException("配置字段不能为空");
        EnumMap<ConfigKey, Double> copy = new EnumMap<>(ConfigKey.class);
        for (ConfigKey key : ConfigKey.values()) {
            Double value = source.get(key);
            if (value == null) throw new IllegalArgumentException("缺少配置项：" + key.key());
            if (!Double.isFinite(value) || value < key.min() || value > key.max()) {
                throw new IllegalArgumentException(key.label() + "必须为 " + key.min() + " 至 "
                        + key.max() + " 之间的有限数值");
            }
            if (key.integral() && value != Math.rint(value)) {
                throw new IllegalArgumentException(key.label() + "必须为整数");
            }
            if (key.name().endsWith("_STEP_MS") && value > 0 && value < LOGIC_STEP_MS) {
                throw new IllegalArgumentException(key.label() + "应为 0 或至少 40 毫秒");
            }
            copy.put(key, value);
        }
        for (String mode : MODES) validateMode(copy, mode);
        return new GameConfig(copy);
    }

    private static void validateMode(Map<ConfigKey, Double> values, String mode) {
        double probabilities = values.get(modeKey(mode, "ELITE_CHANCE"))
                + values.get(modeKey(mode, "PLUS_CHANCE"))
                + values.get(modeKey(mode, "PRO_CHANCE"));
        if (probabilities > 1) {
            throw new IllegalArgumentException(modeKey(mode, "ELITE_CHANCE").section()
                    + "的三种精英敌机概率之和不能超过 1");
        }
        requireOrder(values, mode, "SPAWN_MIN_MS", "SPAWN_MS");
        requireOrder(values, mode, "HERO_SHOOT_MS", "HERO_SHOOT_MAX_MS");
        requireOrder(values, mode, "ENEMY_SHOOT_MIN_MS", "ENEMY_SHOOT_MS");
        requireOrder(values, mode, "BASE_SPEED", "SPEED_MAX");
    }

    private static void requireOrder(Map<ConfigKey, Double> values, String mode,
            String lower, String upper) {
        ConfigKey a = modeKey(mode, lower);
        ConfigKey b = modeKey(mode, upper);
        if (values.get(a) > values.get(b)) {
            throw new IllegalArgumentException(a.section() + "：" + a.label()
                    + "不能大于" + b.label());
        }
    }

    private static ConfigKey modeKey(String mode, String suffix) {
        return ConfigKey.valueOf(mode + "_" + suffix);
    }

    public double value(ConfigKey key) {
        return values.get(Objects.requireNonNull(key));
    }

    /** 整数字段原值返回；允许小数的字段在需要整数坐标等场景四舍五入。 */
    public int integer(ConfigKey key) { return (int) Math.round(value(key)); }

    public Map<ConfigKey, Double> values() { return values; }

    /** 周期从毫秒向上取整为逻辑步；速度保留小数。 */
    public GameParameters parameters(Difficulty difficulty, int level, int score) {
        String mode = Objects.requireNonNull(difficulty).name();
        int stage = Math.max(0, level);
        double heroMs = Math.min(modeValue(mode, "HERO_SHOOT_MAX_MS"),
                modeValue(mode, "HERO_SHOOT_MS") + stage * modeValue(mode, "HERO_SHOOT_STEP_MS"));
        double enemyMs = Math.max(modeValue(mode, "ENEMY_SHOOT_MIN_MS"),
                modeValue(mode, "ENEMY_SHOOT_MS") - stage * modeValue(mode, "ENEMY_SHOOT_STEP_MS"));
        double spawnMs = Math.max(modeValue(mode, "SPAWN_MIN_MS"),
                modeValue(mode, "SPAWN_MS") - stage * modeValue(mode, "SPAWN_STEP_MS"));
        double speed = modeValue(mode, "BASE_SPEED")
                + Math.max(0, score) * modeValue(mode, "SPEED_RATE")
                + (mode.equals("CUSTOM") ? 0 : stage * modeValue(mode, "SPEED_STEP"));
        speed = Math.min(modeValue(mode, "SPEED_MAX"), speed);
        double hp = Math.min(modeKey(mode, "HP_MULTIPLIER").max(),
                modeValue(mode, "HP_MULTIPLIER") + stage * modeValue(mode, "HP_STEP"));
        return new GameParameters((int) modeValue(mode, "ENEMY_MAX"),
                modeValue(mode, "ELITE_CHANCE"), modeValue(mode, "PLUS_CHANCE"),
                modeValue(mode, "PRO_CHANCE"), ticks(heroMs), ticks(enemyMs), ticks(spawnMs),
                speed, hp, (int) modeValue(mode, "BOSS_THRESHOLD"));
    }

    /** spawnNumber 从 1 开始；生成关闭时返回 0，生命增长不会超过安全编辑上限。 */
    public int bossHp(Difficulty difficulty, int spawnNumber) {
        String mode = Objects.requireNonNull(difficulty).name();
        if (modeValue(mode, "BOSS_THRESHOLD") == 0) return 0;
        double hp = modeValue(mode, "BOSS_HP")
                + Math.max(0L, (long) spawnNumber - 1) * modeValue(mode, "BOSS_HP_STEP");
        return (int) Math.min(modeKey(mode, "BOSS_HP").max(), hp);
    }

    private double modeValue(String mode, String suffix) { return value(modeKey(mode, suffix)); }

    private static int ticks(double milliseconds) {
        return (int) Math.ceil(milliseconds / LOGIC_STEP_MS);
    }
}
