package edu.hitsz.factory;

import edu.hitsz.aircraft.AbstractAircraft;
import edu.hitsz.aircraft.MobEnemy;

public class MobEnemyFactory implements EnemyFactory {
    @Override
    public AbstractAircraft create(int x, int y, int speedX, int speedY, int hp) {
        return new MobEnemy(x, y, speedX, speedY, hp);
    }
}