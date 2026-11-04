package edu.hitsz.application;

import java.util.function.IntConsumer;

public final class HardGame extends Game {
    public HardGame(IntConsumer onGameOver) {
        super(Difficulty.HARD, onGameOver);
    }

    @Override
    protected GameParameters parametersAtLevel(int level) {
        return config.parameters(Difficulty.HARD, level, 0);
    }

    @Override
    protected int bossHpForSpawn(int spawnNumber) {
        return config.bossHp(Difficulty.HARD, spawnNumber);
    }
}
