package edu.hitsz.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Objects;

/** 动态首页，入口与全局设置分别处理。 */
public final class HomePanel extends BackgroundPanel {
    public HomePanel(Runnable start, Runnable settings) {
        Objects.requireNonNull(start);
        Objects.requireNonNull(settings);
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(22, 28, 24, 28));
        JPanel top = GameTheme.transparent(new BorderLayout());
        top.add(GameTheme.label("FLIGHT / 01", 12, GameTheme.MUTED), BorderLayout.WEST);
        JButton gear = GameTheme.gearButton("打开设置");
        gear.addActionListener(e -> settings.run());
        top.add(gear, BorderLayout.EAST);
        add(top, BorderLayout.NORTH);
        JPanel center = GameTheme.transparent(new GridBagLayout());
        JPanel hero = GameTheme.transparent(null);
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.add(GameTheme.label("AIRCRAFT WAR", 15, GameTheme.GOLD));
        hero.add(Box.createVerticalStrut(12));
        hero.add(new HomeTitle());
        hero.add(Box.createVerticalStrut(14));
        hero.add(GameTheme.label("穿过云海，迎接下一场挑战", 15, GameTheme.MUTED));
        hero.add(Box.createVerticalStrut(48));
        JButton begin = GameTheme.button("开始游戏  →", true);
        begin.setAlignmentX(Component.CENTER_ALIGNMENT);
        begin.setMaximumSize(new Dimension(280, 58));
        begin.setPreferredSize(new Dimension(280, 58));
        begin.addActionListener(e -> start.run());
        hero.add(begin);
        hero.add(Box.createVerticalStrut(112));
        center.add(hero);
        add(center, BorderLayout.CENTER);
        add(GameTheme.label("鼠标操控 · 自动射击 · Esc 暂停", 12, new Color(218, 235, 232)),
                BorderLayout.SOUTH);
    }
}
