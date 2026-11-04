package edu.hitsz.ui;

import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CustomResultPanelTest {
    @Test
    void replayAndBackAreMutuallyExclusiveAndRunOnce() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger replay = new AtomicInteger();
            AtomicInteger back = new AtomicInteger();
            CustomResultPanel panel = new CustomResultPanel(230,
                    replay::incrementAndGet, back::incrementAndGet);
            button(panel, "再来一局").doClick(0);
            button(panel, "再来一局").doClick(0);
            button(panel, "返回选择").doClick(0);
            assertEquals(1, replay.get());
            assertEquals(0, back.get());
            CustomResultPanel another = new CustomResultPanel(230,
                    replay::incrementAndGet, back::incrementAndGet);
            button(another, "返回选择").doClick(0);
            button(another, "返回选择").doClick(0);
            assertEquals(1, back.get());
        });
    }

    private static JButton button(Container parent, String text) {
        for (Component component : parent.getComponents()) {
            if (component instanceof JButton button && text.equals(button.getText())) return button;
            if (component instanceof Container child) {
                JButton found = button(child, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}
