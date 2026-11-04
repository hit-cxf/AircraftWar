package edu.hitsz.application;

import java.util.function.IntConsumer;

public final class NormalGame extends Game {
    public NormalGame(IntConsumer onGameOver) {
        super(Difficulty.NORMAL, onGameOver);
    }

    @Override
    protected GameParameters parametersAtLevel(int level) {
        return config.parameters(Difficulty.NORMAL, level, 0);
    }

    @Override
    protected int bossHpForSpawn(int spawnNumber) {
        return config.bossHp(Difficulty.NORMAL, spawnNumber);
    }
}
