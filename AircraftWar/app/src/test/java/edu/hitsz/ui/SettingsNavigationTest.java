package edu.hitsz.ui;

import edu.hitsz.application.Difficulty;
import edu.hitsz.audio.AudioSettings;
import edu.hitsz.config.ConfigKey;
import edu.hitsz.config.GameConfigStore;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class SettingsNavigationTest {
    @Test
    void customGearOpensSettingsWithoutStartingAGame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicInteger settings = new AtomicInteger();
            AtomicReference<Difficulty> selected = new AtomicReference<>();
            DifficultySelectionPanel panel = new DifficultySelectionPanel(selected::set,
                    () -> {}, settings::incrementAndGet);
            button(panel, "编辑自定义模式参数").doClick();
            assertEquals(1, settings.get());
            assertNull(selected.get());
            button(panel, "开始自定义模式").doClick();
            assertEquals(Difficulty.CUSTOM, selected.get());
        });
    }

    @Test
    void customAndGlobalSettingsExposeDifferentControls() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            SettingsPanel custom = new SettingsPanel(() -> {});
            GlobalSettingsPanel global = new GlobalSettingsPanel(() -> {});
            assertEquals(Arrays.stream(ConfigKey.values()).filter(ConfigKey::customEditable).count(),
                    components(custom, JSpinner.class).size());
            assertTrue(components(custom, JSlider.class).isEmpty());
            assertTrue(components(global, JSpinner.class).isEmpty());
            assertEquals(2, components(global, JSlider.class).size());
            JComboBox<?> sections = components(custom, JComboBox.class).getFirst();
            for (int i = 0; i < sections.getItemCount(); i++) {
                assertFalse(List.of("简单难度", "普通难度", "困难难度").contains(sections.getItemAt(i)));
            }
        });
    }

    @Test
    void cancellingADraftDoesNotPublishGameplayOrAudioChanges() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            var config = GameConfigStore.current();
            var audio = AudioSettings.global().snapshot();
            AtomicInteger back = new AtomicInteger();
            SettingsPanel custom = new SettingsPanel(back::incrementAndGet);
            components(custom, JSpinner.class).getFirst().setValue(2.0);
            button(custom, "取消").doClick();
            assertSame(config, GameConfigStore.current());
            GlobalSettingsPanel global = new GlobalSettingsPanel(back::incrementAndGet);
            components(global, JSlider.class).getFirst().setValue(5);
            button(global, "取消").doClick();
            assertEquals(audio, AudioSettings.global().snapshot());
            assertEquals(2, back.get());
        });
    }

    private static JButton button(Container parent, String name) {
        return components(parent, JButton.class).stream().filter(button ->
                name.equals(button.getText()) || name.equals(button.getToolTipText())
                        || name.equals(button.getAccessibleContext().getAccessibleName()))
                .findFirst().orElseThrow();
    }

    private static <T extends Component> List<T> components(Container parent, Class<T> type) {
        List<T> matches = new ArrayList<>();
        for (Component child : parent.getComponents()) {
            if (type.isInstance(child)) matches.add(type.cast(child));
            if (child instanceof Container container) matches.addAll(components(container, type));
        }
        return matches;
    }
}
