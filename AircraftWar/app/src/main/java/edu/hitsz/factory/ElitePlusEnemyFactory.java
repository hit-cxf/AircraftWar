package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.ElitePlusEnemy;

public class ElitePlusEnemyFactory implements EnemyFactory {
    @Override
    public AbstractAircraft create(int x, int y, int speedX, int speedY, int hp) {
        return new ElitePlusEnemy(x, y, speedX, speedY, hp);
    }
}
