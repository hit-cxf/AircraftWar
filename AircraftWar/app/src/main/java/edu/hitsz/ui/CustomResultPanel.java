package edu.hitsz.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;

/** 自定义模式的主窗口内结算页，不访问成绩存储或排行榜。 */
public final class CustomResultPanel extends BackgroundPanel {
    private boolean resolved;

    public CustomResultPanel(int score, Runnable replay, Runnable back) {
        Objects.requireNonNull(replay);
        Objects.requireNonNull(back);
        setPreferredSize(new Dimension(512, 768));
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(28, 28, 28, 28));
        add(GameTheme.label("CUSTOM FLIGHT", 12, GameTheme.GOLD), BorderLayout.NORTH);
        JPanel center = GameTheme.transparent(new GridBagLayout());
        JPanel content = GameTheme.card();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setPreferredSize(new Dimension(390, 350));
        content.add(Box.createVerticalStrut(12));
        content.add(GameTheme.label("FLIGHT COMPLETE", 13, GameTheme.GOLD));
        content.add(Box.createVerticalStrut(12));
        content.add(GameTheme.title("本次飞行结束", 28));
        content.add(Box.createVerticalStrut(22));
        JLabel points = GameTheme.title(Integer.toString(score), 54);
        points.setForeground(GameTheme.GOLD);
        content.add(points);
        content.add(GameTheme.label("本局得分 · 自定义模式不记排行", 13, GameTheme.MUTED));
        content.add(Box.createVerticalStrut(30));
        JPanel actions = GameTheme.transparent(new GridLayout(1, 2, 10, 0));
        actions.setMaximumSize(new Dimension(350, 48));
        actions.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton again = GameTheme.button("再来一局", true);
        JButton menu = GameTheme.button("返回选择", false);
        again.addActionListener(e -> finish(replay));
        menu.addActionListener(e -> finish(back));
        actions.add(again);
        actions.add(menu);
        content.add(actions);
        center.add(content);
        add(center, BorderLayout.CENTER);
        add(GameTheme.label("自由调整参数，体验属于你的飞行节奏", 12, GameTheme.TEXT), BorderLayout.SOUTH);
    }

    private void finish(Runnable action) {
        if (resolved) return;
        resolved = true;
        action.run();
    }
}
