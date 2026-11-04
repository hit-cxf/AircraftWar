package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.BossEnemy;

public class BossEnemyFactory implements EnemyFactory {
    @Override
    public AbstractAircraft create(int x, int y, int speedX, int speedY, int hp) {
        return new BossEnemy(x, y, speedX, speedY, hp);
    }
}
