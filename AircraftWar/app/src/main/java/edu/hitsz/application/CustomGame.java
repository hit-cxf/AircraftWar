package edu.hitsz.application;

import java.util.function.IntConsumer;

/** 自定义曲线仍复用 Game 的模板主流程。 */
public final class CustomGame extends Game {
    public CustomGame(IntConsumer onGameOver) {
        super(Difficulty.CUSTOM, onGameOver);
    }

    @Override
    protected GameParameters parametersAtLevel(int level) {
        return config.parameters(Difficulty.CUSTOM, level, 0);
    }

    @Override
    protected int bossHpForSpawn(int spawnNumber) {
        return config.bossHp(Difficulty.CUSTOM, spawnNumber);
    }
}
