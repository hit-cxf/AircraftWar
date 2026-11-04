package edu.hitsz.ui;

import edu.hitsz.config.ConfigKey;
import edu.hitsz.config.GameConfigStore;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class SettingsDialogTest {
    @Test
    void invalidNumberKeepsTheDraftAndShowsAnInternalMessageWithoutSaving() throws Exception {
        var file = GameConfigStore.file();
        byte[] before = Files.exists(file) ? Files.readAllBytes(file) : null;
        SwingUtilities.invokeAndWait(() -> {
            var saved = GameConfigStore.current();
            AtomicInteger exits = new AtomicInteger();
            SettingsPanel settings = new SettingsPanel(exits::incrementAndGet);
            DialogHost host = new DialogHost(settings);
            JSpinner hp = spinner(settings, ConfigKey.HERO_HP);
            JSpinner power = spinner(settings, ConfigKey.HERO_POWER);
            JFormattedTextField hpField = ((JSpinner.NumberEditor) hp.getEditor()).getTextField();
            double draftPower = saved.value(ConfigKey.HERO_POWER) == ConfigKey.HERO_POWER.max()
                    ? ConfigKey.HERO_POWER.min() : saved.value(ConfigKey.HERO_POWER) + 1;
            power.setValue(draftPower);
            hpField.setText("not-a-number");
            button(settings, "保存设置").doClick();

            assertTrue(host.isDialogOpen(), "错误应显示在当前页面的遮罩内");
            assertSame(host, DialogHost.find(settings));
            assertSame(saved, GameConfigStore.current());
            assertEquals("not-a-number", hpField.getText());
            assertEquals(draftPower, ((Number) power.getValue()).doubleValue());
            assertEquals(0, exits.get());
            assertNull(SwingUtilities.getWindowAncestor(host), "提示不需要额外的顶层窗口");

            host.dismiss();
            assertFalse(host.isDialogOpen());
            assertEquals("not-a-number", hpField.getText());
            assertEquals(draftPower, ((Number) power.getValue()).doubleValue());
        });
        if (before == null) assertFalse(Files.exists(file));
        else assertArrayEquals(before, Files.readAllBytes(file));
    }

    private static JSpinner spinner(Container parent, ConfigKey key) {
        return components(parent, JSpinner.class).stream().filter(editor ->
                key.label().equals(editor.getAccessibleContext().getAccessibleName()))
                .findFirst().orElseThrow();
    }

    private static JButton button(Container parent, String text) {
        return components(parent, JButton.class).stream()
                .filter(button -> text.equals(button.getText())).findFirst().orElseThrow();
    }

    private static <T extends Component> List<T> components(Container parent, Class<T> type) {
        List<T> result = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) result.add(type.cast(child));
            if (child instanceof Container container) result.addAll(components(container, type));
        }
        return result;
    }
}
