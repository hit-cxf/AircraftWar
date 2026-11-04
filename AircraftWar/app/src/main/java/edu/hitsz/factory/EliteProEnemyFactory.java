package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.EliteProEnemy;

public class EliteProEnemyFactory implements EnemyFactory {
    @Override
    public AbstractAircraft create(int x, int y, int speedX, int speedY, int hp) {
        return new EliteProEnemy(x, y, speedX, speedY, hp);
    }
}
