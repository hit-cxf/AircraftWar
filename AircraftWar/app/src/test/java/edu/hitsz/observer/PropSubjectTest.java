package edu.hitsz.observer;

import edu.hitsz.prop.BombProp;
import edu.hitsz.prop.FreezeProp;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PropSubjectTest {
    @Test
    void registrationRemovalAndNotificationUseOnlyObserverInterface() {
        Counter observer = new Counter();
        BombProp bomb = new BombProp(0, 0, 0, 0);
        bomb.addObserver(observer);
        bomb.addObserver(observer);
        bomb.activate(null);
        assertEquals(1, observer.bombs);
        assertEquals(0, observer.freezes);
        bomb.removeObserver(observer);
        bomb.activate(null);
        assertEquals(1, observer.bombs);

        FreezeProp freeze = new FreezeProp(0, 0, 0, 0);
        freeze.addObserver(observer);
        freeze.activate(null);
        assertEquals(1, observer.freezes);
    }

    private static class Counter implements PropEffectObserver {
        int bombs;
        int freezes;
        public void onBomb() { bombs++; }
        public void onFreeze() { freezes++; }
    }
}
