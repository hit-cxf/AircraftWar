package edu.hitsz.application;

import java.util.function.IntConsumer;

public final class EasyGame extends Game {
    public EasyGame(IntConsumer onGameOver) {
        super(Difficulty.EASY, onGameOver);
    }

    @Override
    protected GameParameters parametersAtLevel(int level) {
        return config.parameters(Difficulty.EASY, level, 0);
    }

    @Override
    protected int bossHpForSpawn(int spawnNumber) {
        return config.bossHp(Difficulty.EASY, spawnNumber);
    }
}
