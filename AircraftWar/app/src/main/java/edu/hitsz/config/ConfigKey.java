package edu.hitsz.config;

import java.util.Locale;

/** 配置字段、编辑范围与显示元数据。玩法默认值只定义于 game-defaults.properties。 */
public enum ConfigKey {
    HERO_HP("英雄机生命值", "英雄机", 1, 10000, 10),
    HERO_POWER("英雄子弹威力", "英雄机", 1, 1000, 1),
    HERO_BULLET_SPEED("英雄直射速度", "英雄机", 1, 40, 1),
    HERO_SCATTER_COUNT("英雄散射数量", "英雄机", 1, 15, 1),
    HERO_SCATTER_SPREAD("英雄散射水平间隔", "英雄机", 1, 12, 1),
    HERO_RING_COUNT("英雄环射数量", "英雄机", 3, 64, 1),
    HERO_RING_SPEED("英雄环射速度", "英雄机", 1, 40, 1),
    ENEMY_POWER("敌机子弹威力", "敌机与子弹", 1, 1000, 1),
    ENEMY_BULLET_SPEED("敌机直射速度", "敌机与子弹", 1, 40, 1),
    ENEMY_SCATTER_COUNT("敌机散射数量", "敌机与子弹", 1, 15, 1),
    ENEMY_SCATTER_SPREAD("敌机散射水平间隔", "敌机与子弹", 1, 12, 1),
    ENEMY_RING_COUNT("敌机环射数量", "敌机与子弹", 3, 64, 1),
    ENEMY_RING_SPEED("敌机环射速度", "敌机与子弹", 1, 40, 1),
    ENEMY_DOUBLE_OFFSET("敌机双射水平偏移", "敌机与子弹", 1, 40, 1),
    MOB_HP("普通敌机基础生命", "敌机与子弹", 1, 10000, 1),
    ELITE_HP("精英敌机基础生命", "敌机与子弹", 1, 10000, 1),
    PLUS_HP("精英Plus基础生命", "敌机与子弹", 1, 10000, 1),
    PRO_HP("精英Pro基础生命", "敌机与子弹", 1, 10000, 1),
    ENEMY_HORIZONTAL_SPEED("敌机水平速度", "敌机与子弹", 1, 20, 1),
    BOSS_HORIZONTAL_SPEED("Boss水平速度", "敌机与子弹", 1, 20, 1),
    KILL_SCORE("击毁敌机得分", "道具与战斗", 1, 10000, 1),
    DROP_CHANCE("精英敌机道具掉落率", "道具与战斗", 0, 1, 0.01),
    BOSS_DROP_COUNT("Boss掉落数量", "道具与战斗", 1, 12, 1),
    PROP_SPEED("道具下落速度", "道具与战斗", 1, 20, 1),
    HEAL_AMOUNT("回血道具恢复值", "道具与战斗", 1, 10000, 1),
    FIRE_DURATION_MS("散射持续时间（毫秒）", "道具与战斗", 40, 120000, 40),
    ELITE_FREEZE_MS("精英敌机冻结时间（毫秒）", "道具与战斗", 40, 30000, 40),
    PLUS_FREEZE_MS("精英Plus冻结时间（毫秒）", "道具与战斗", 40, 30000, 40),
    PRO_SLOW_MS("精英Pro减速时间（毫秒）", "道具与战斗", 40, 30000, 40),
    PRO_SLOW_SCALE("精英Pro减速后倍率", "道具与战斗", 0.05, 0.95, 0.05),
    BULLET_FREEZE_MS("敌弹冻结时间（毫秒）", "道具与战斗", 40, 30000, 40),
    PRO_BOMB_DAMAGE("炸弹对精英Pro伤害", "道具与战斗", 1, 10000, 1),
    BOSS_WARNING_MS("Boss登场预警时间（毫秒）", "道具与战斗", 40, 10000, 40),
    BOSS_PHASE_RATIO("Boss二阶段生命比例", "道具与战斗", 0.05, 0.95, 0.05),
    BOSS_CHARGE_MS("Boss二阶段蓄力时间（毫秒）", "道具与战斗", 40, 10000, 40),
    LEVEL_DURATION_MS("阶段提升间隔（毫秒）", "道具与战斗", 40, 600000, 40),
    EASY_ENEMY_MAX("同屏敌机上限", "简单难度", 1, 30, 1),
    EASY_ELITE_CHANCE("精英敌机概率", "简单难度", 0, 1, 0.01),
    EASY_PLUS_CHANCE("精英Plus概率", "简单难度", 0, 1, 0.01),
    EASY_PRO_CHANCE("精英Pro概率", "简单难度", 0, 1, 0.01),
    EASY_HERO_SHOOT_MS("英雄射击周期（毫秒）", "简单难度", 40, 10000, 40),
    EASY_ENEMY_SHOOT_MS("敌机射击周期（毫秒）", "简单难度", 40, 10000, 40),
    EASY_SPAWN_MS("敌机生成周期（毫秒）", "简单难度", 40, 10000, 40),
    EASY_BASE_SPEED("敌机基础速度", "简单难度", 0.1, 40, 0.1),
    EASY_SPEED_RATE("每分增加速度", "简单难度", 0, 1, 0.001),
    EASY_HP_MULTIPLIER("敌机生命倍率", "简单难度", 0.1, 20, 0.1),
    EASY_BOSS_THRESHOLD("Boss生成分数间隔（0关闭）", "简单难度", 0, 100000, 10),
    EASY_BOSS_HP("Boss基础生命", "简单难度", 1, 100000, 10),
    EASY_BOSS_HP_STEP("每次Boss生命增加", "简单难度", 0, 100000, 10),
    EASY_SPAWN_STEP_MS("每阶段生成周期减少（毫秒，0关闭）", "简单难度", 0, 10000, 40),
    EASY_SPAWN_MIN_MS("最短生成周期（毫秒）", "简单难度", 40, 10000, 40),
    EASY_HERO_SHOOT_STEP_MS("每阶段英雄射击周期增加（毫秒，0关闭）", "简单难度", 0, 10000, 40),
    EASY_HERO_SHOOT_MAX_MS("最长英雄射击周期（毫秒）", "简单难度", 40, 10000, 40),
    EASY_ENEMY_SHOOT_STEP_MS("每阶段敌机射击周期减少（毫秒，0关闭）", "简单难度", 0, 10000, 40),
    EASY_ENEMY_SHOOT_MIN_MS("最短敌机射击周期（毫秒）", "简单难度", 40, 10000, 40),
    EASY_SPEED_STEP("每阶段速度增加", "简单难度", 0, 10, 0.1),
    EASY_SPEED_MAX("敌机速度上限", "简单难度", 0.1, 40, 0.1),
    EASY_HP_STEP("每阶段生命倍率增加", "简单难度", 0, 2, 0.05),
    NORMAL_ENEMY_MAX("同屏敌机上限", "普通难度", 1, 30, 1),
    NORMAL_ELITE_CHANCE("精英敌机概率", "普通难度", 0, 1, 0.01),
    NORMAL_PLUS_CHANCE("精英Plus概率", "普通难度", 0, 1, 0.01),
    NORMAL_PRO_CHANCE("精英Pro概率", "普通难度", 0, 1, 0.01),
    NORMAL_HERO_SHOOT_MS("英雄射击周期（毫秒）", "普通难度", 40, 10000, 40),
    NORMAL_ENEMY_SHOOT_MS("敌机射击周期（毫秒）", "普通难度", 40, 10000, 40),
    NORMAL_SPAWN_MS("敌机生成周期（毫秒）", "普通难度", 40, 10000, 40),
    NORMAL_BASE_SPEED("敌机基础速度", "普通难度", 0.1, 40, 0.1),
    NORMAL_SPEED_RATE("每分增加速度", "普通难度", 0, 1, 0.001),
    NORMAL_HP_MULTIPLIER("敌机生命倍率", "普通难度", 0.1, 20, 0.1),
    NORMAL_BOSS_THRESHOLD("Boss生成分数间隔（0关闭）", "普通难度", 0, 100000, 10),
    NORMAL_BOSS_HP("Boss基础生命", "普通难度", 1, 100000, 10),
    NORMAL_BOSS_HP_STEP("每次Boss生命增加", "普通难度", 0, 100000, 10),
    NORMAL_SPAWN_STEP_MS("每阶段生成周期减少（毫秒，0关闭）", "普通难度", 0, 10000, 40),
    NORMAL_SPAWN_MIN_MS("最短生成周期（毫秒）", "普通难度", 40, 10000, 40),
    NORMAL_HERO_SHOOT_STEP_MS("每阶段英雄射击周期增加（毫秒，0关闭）", "普通难度", 0, 10000, 40),
    NORMAL_HERO_SHOOT_MAX_MS("最长英雄射击周期（毫秒）", "普通难度", 40, 10000, 40),
    NORMAL_ENEMY_SHOOT_STEP_MS("每阶段敌机射击周期减少（毫秒，0关闭）", "普通难度", 0, 10000, 40),
    NORMAL_ENEMY_SHOOT_MIN_MS("最短敌机射击周期（毫秒）", "普通难度", 40, 10000, 40),
    NORMAL_SPEED_STEP("每阶段速度增加", "普通难度", 0, 10, 0.1),
    NORMAL_SPEED_MAX("敌机速度上限", "普通难度", 0.1, 40, 0.1),
    NORMAL_HP_STEP("每阶段生命倍率增加", "普通难度", 0, 2, 0.05),
    HARD_ENEMY_MAX("同屏敌机上限", "困难难度", 1, 30, 1),
    HARD_ELITE_CHANCE("精英敌机概率", "困难难度", 0, 1, 0.01),
    HARD_PLUS_CHANCE("精英Plus概率", "困难难度", 0, 1, 0.01),
    HARD_PRO_CHANCE("精英Pro概率", "困难难度", 0, 1, 0.01),
    HARD_HERO_SHOOT_MS("英雄射击周期（毫秒）", "困难难度", 40, 10000, 40),
    HARD_ENEMY_SHOOT_MS("敌机射击周期（毫秒）", "困难难度", 40, 10000, 40),
    HARD_SPAWN_MS("敌机生成周期（毫秒）", "困难难度", 40, 10000, 40),
    HARD_BASE_SPEED("敌机基础速度", "困难难度", 0.1, 40, 0.1),
    HARD_SPEED_RATE("每分增加速度", "困难难度", 0, 1, 0.001),
    HARD_HP_MULTIPLIER("敌机生命倍率", "困难难度", 0.1, 20, 0.1),
    HARD_BOSS_THRESHOLD("Boss生成分数间隔（0关闭）", "困难难度", 0, 100000, 10),
    HARD_BOSS_HP("Boss基础生命", "困难难度", 1, 100000, 10),
    HARD_BOSS_HP_STEP("每次Boss生命增加", "困难难度", 0, 100000, 10),
    HARD_SPAWN_STEP_MS("每阶段生成周期减少（毫秒，0关闭）", "困难难度", 0, 10000, 40),
    HARD_SPAWN_MIN_MS("最短生成周期（毫秒）", "困难难度", 40, 10000, 40),
    HARD_HERO_SHOOT_STEP_MS("每阶段英雄射击周期增加（毫秒，0关闭）", "困难难度", 0, 10000, 40),
    HARD_HERO_SHOOT_MAX_MS("最长英雄射击周期（毫秒）", "困难难度", 40, 10000, 40),
    HARD_ENEMY_SHOOT_STEP_MS("每阶段敌机射击周期减少（毫秒，0关闭）", "困难难度", 0, 10000, 40),
    HARD_ENEMY_SHOOT_MIN_MS("最短敌机射击周期（毫秒）", "困难难度", 40, 10000, 40),
    HARD_SPEED_STEP("每阶段速度增加", "困难难度", 0, 10, 0.1),
    HARD_SPEED_MAX("敌机速度上限", "困难难度", 0.1, 40, 0.1),
    HARD_HP_STEP("每阶段生命倍率增加", "困难难度", 0, 2, 0.05),
    CUSTOM_ENEMY_MAX("同屏敌机上限", "自定义难度", 1, 30, 1),
    CUSTOM_ELITE_CHANCE("精英敌机概率", "自定义难度", 0, 1, 0.01),
    CUSTOM_PLUS_CHANCE("精英Plus概率", "自定义难度", 0, 1, 0.01),
    CUSTOM_PRO_CHANCE("精英Pro概率", "自定义难度", 0, 1, 0.01),
    CUSTOM_HERO_SHOOT_MS("英雄射击周期（毫秒）", "自定义难度", 40, 10000, 40),
    CUSTOM_ENEMY_SHOOT_MS("敌机射击周期（毫秒）", "自定义难度", 40, 10000, 40),
    CUSTOM_SPAWN_MS("敌机生成周期（毫秒）", "自定义难度", 40, 10000, 40),
    CUSTOM_BASE_SPEED("敌机基础速度", "自定义难度", 0.1, 40, 0.1),
    CUSTOM_SPEED_RATE("每分增加速度", "自定义难度", 0, 1, 0.001),
    CUSTOM_HP_MULTIPLIER("敌机生命倍率", "自定义难度", 0.1, 20, 0.1),
    CUSTOM_BOSS_THRESHOLD("Boss生成分数间隔（0关闭）", "自定义难度", 0, 100000, 10),
    CUSTOM_BOSS_HP("Boss基础生命", "自定义难度", 1, 100000, 10),
    CUSTOM_BOSS_HP_STEP("每次Boss生命增加", "自定义难度", 0, 100000, 10),
    CUSTOM_SPAWN_STEP_MS("每阶段生成周期减少（毫秒，0关闭）", "自定义难度", 0, 10000, 40),
    CUSTOM_SPAWN_MIN_MS("最短生成周期（毫秒）", "自定义难度", 40, 10000, 40),
    CUSTOM_HERO_SHOOT_STEP_MS("每阶段英雄射击周期增加（毫秒，0关闭）", "自定义难度", 0, 10000, 40),
    CUSTOM_HERO_SHOOT_MAX_MS("最长英雄射击周期（毫秒）", "自定义难度", 40, 10000, 40),
    CUSTOM_ENEMY_SHOOT_STEP_MS("每阶段敌机射击周期减少（毫秒，0关闭）", "自定义难度", 0, 10000, 40),
    CUSTOM_ENEMY_SHOOT_MIN_MS("最短敌机射击周期（毫秒）", "自定义难度", 40, 10000, 40),
    CUSTOM_SPEED_MAX("敌机速度上限", "自定义难度", 0.1, 40, 0.1),
    CUSTOM_HP_STEP("每阶段生命倍率增加", "自定义难度", 0, 2, 0.05);

    private final String label;
    private final String section;
    private final double min;
    private final double max;
    private final double step;

    ConfigKey(String label, String section, double min, double max, double step) {
        this.label = label;
        this.section = section;
        this.min = min;
        this.max = max;
        this.step = step;
    }

    public String key() { return name().toLowerCase(Locale.ROOT); }
    public String label() { return label; }
    public String section() { return section; }
    public double min() { return min; }
    public double max() { return max; }
    public double step() { return step; }

    /** step 为整数的字段采用整数值；step 是界面调节粒度，不限制毫秒必须整除 40。 */
    public boolean integral() { return step >= 1; }

    /** 公共玩法字段只在自定义模式生效；三种标准难度的预设只读。 */
    public boolean customEditable() {
        return !name().startsWith("EASY_") && !name().startsWith("NORMAL_")
                && !name().startsWith("HARD_");
    }

    public static ConfigKey fromKey(String key) {
        for (ConfigKey candidate : values()) {
            if (candidate.key().equals(key)) return candidate;
        }
        throw new IllegalArgumentException("未知配置项：" + key);
    }
}
