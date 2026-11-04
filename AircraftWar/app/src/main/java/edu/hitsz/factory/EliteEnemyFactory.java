package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.EliteEnemy;

public class EliteEnemyFactory implements EnemyFactory {
    @Override
    public AbstractAircraft create(int x, int y, int speedX, int speedY, int hp) {
        return new EliteEnemy(x, y, speedX, speedY, hp);
    }
}
