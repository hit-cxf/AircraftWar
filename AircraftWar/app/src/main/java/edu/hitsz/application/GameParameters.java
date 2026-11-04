package edu.hitsz.application;

/** 一阶段的游戏参数；周期单位为固定更新步（每步 40 ms）。 */
public record GameParameters(int enemyMax, double eliteChance, double plusChance,
        double proChance, int heroShootCycle, int enemyShootCycle,
        int spawnCycle, double enemySpeed, double hpMultiplier, int bossThreshold) {
    public GameParameters {
        if (enemyMax <= 0 || heroShootCycle <= 0 || enemyShootCycle <= 0
                || spawnCycle <= 0 || enemySpeed <= 0 || hpMultiplier <= 0
                || eliteChance < 0 || plusChance < 0 || proChance < 0
                || eliteChance + plusChance + proChance > 1 || bossThreshold < 0
                || !Double.isFinite(enemySpeed) || !Double.isFinite(hpMultiplier)
                || !Double.isFinite(eliteChance + plusChance + proChance)) {
            throw new IllegalArgumentException("游戏难度参数无效");
        }
    }
}
