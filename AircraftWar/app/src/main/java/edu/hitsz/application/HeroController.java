package edu.hitsz.application;

import edu.hitsz.aircraft.HeroAircraft;

import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import java.util.function.BooleanSupplier;

/**
 * 英雄机控制类
 * 监听鼠标，控制英雄机的移动
 * @author hitsz
 */
public class HeroController {
    private final HeroAircraft heroAircraft;
    private final BooleanSupplier inputEnabled;

    public HeroController(Node inputNode, HeroAircraft heroAircraft){
        this(inputNode, heroAircraft, () -> true);
    }

    public HeroController(Node inputNode, HeroAircraft heroAircraft, BooleanSupplier inputEnabled) {
        this.heroAircraft = heroAircraft;
        this.inputEnabled = inputEnabled;

        inputNode.addEventHandler(MouseEvent.MOUSE_MOVED, this::handleMouse);
        inputNode.addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouse);
    }

    private void handleMouse(MouseEvent e) {
        if (!inputEnabled.getAsBoolean()) return;
        double x = e.getX();
        double y = e.getY();
        if (x < heroAircraft.getWidth() / 2
            || x > Main.WINDOW_WIDTH - heroAircraft.getWidth() / 2
            || y < heroAircraft.getHeight() / 2
            || y > Main.WINDOW_HEIGHT - heroAircraft.getHeight() / 2) {
            return;
        }
        heroAircraft.setLocation(x, y);
    }


}
