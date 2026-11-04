package edu.hitsz.ui;

import edu.hitsz.application.Difficulty;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** 难度选择；自定义模式的齿轮独立打开设置，不触发开始游戏。 */
public class DifficultySelectionPanel extends BackgroundPanel {
    public DifficultySelectionPanel(Consumer<Difficulty> selected) {
        this(selected, () -> { }, () -> { });
    }

    public DifficultySelectionPanel(Consumer<Difficulty> selected, Runnable back, Runnable customSettings) {
        Objects.requireNonNull(selected);
        setLayout(new BorderLayout(0, 20));
        setBorder(new EmptyBorder(24, 28, 28, 28));
        JPanel navigation = GameTheme.transparent(new BorderLayout());
        JButton backButton = GameTheme.button("‹ 返回首页", false);
        backButton.setPreferredSize(new Dimension(128, 42));
        backButton.addActionListener(e -> back.run());
        navigation.add(backButton, BorderLayout.WEST);
        navigation.add(GameTheme.label("SELECT FLIGHT", 12, GameTheme.GOLD), BorderLayout.EAST);
        add(navigation, BorderLayout.NORTH);

        JPanel center = GameTheme.transparent(new GridBagLayout());
        JPanel menu = GameTheme.transparent(null);
        menu.setLayout(new BoxLayout(menu, BoxLayout.Y_AXIS));
        menu.setPreferredSize(new Dimension(360, 510));
        menu.add(GameTheme.title("选择飞行难度", 30));
        menu.add(Box.createVerticalStrut(10));
        menu.add(GameTheme.label("每一次起飞，都有新的可能", 14, GameTheme.MUTED));
        menu.add(Box.createVerticalStrut(28));
        List<JButton> buttons = new ArrayList<>();
        for (Difficulty difficulty : Difficulty.values()) {
            boolean custom = difficulty.name().equals("CUSTOM");
            JPanel row = GameTheme.transparent(new BorderLayout(10, 0));
            row.setMaximumSize(new Dimension(360, 66));
            row.setPreferredSize(new Dimension(360, 66));
            JButton button = GameTheme.button("<html><div style='text-align:left'><b>"
                    + difficulty.displayName() + "</b><br><span style='font-size:10px'>"
                    + description(difficulty) + "</span></div></html>", false);
            button.setHorizontalAlignment(SwingConstants.LEFT);
            button.setFont(GameTheme.font(17, Font.BOLD));
            button.setBorder(new EmptyBorder(8, 20, 8, 16));
            button.getAccessibleContext().setAccessibleName("开始" + difficulty.displayName() + "模式");
            button.addActionListener(e -> {
                buttons.forEach(b -> b.setEnabled(false));
                try { selected.accept(difficulty); }
                finally { SwingUtilities.invokeLater(() -> buttons.forEach(b -> b.setEnabled(true))); }
            });
            buttons.add(button);
            row.add(button, BorderLayout.CENTER);
            if (custom) {
                JButton gear = GameTheme.gearButton("编辑自定义模式参数");
                gear.setPreferredSize(new Dimension(54, 66));
                gear.addActionListener(e -> customSettings.run());
                row.add(gear, BorderLayout.EAST);
            }
            menu.add(row);
            menu.add(Box.createVerticalStrut(12));
        }
        menu.add(Box.createVerticalStrut(14));
        menu.add(GameTheme.label("移动鼠标控制英雄机 · 窗口失焦自动暂停", 12, GameTheme.MUTED));
        center.add(menu);
        add(center, BorderLayout.CENTER);
    }

    private static String description(Difficulty difficulty) {
        return switch (difficulty.name()) {
            case "EASY" -> "轻松起飞，熟悉天空";
            case "NORMAL" -> "迎战 Boss，挑战逐步升级";
            case "HARD" -> "密集弹幕，考验飞行技巧";
            case "CUSTOM" -> "按自己的节奏，定制战斗参数";
            default -> "开始新的飞行";
        };
    }
}
