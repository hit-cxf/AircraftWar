package edu.hitsz.ui;

import edu.hitsz.audio.AudioSettings;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/** 首页全局设置仅管理音量与静音，不暴露自定义玩法参数。 */
public final class GlobalSettingsPanel extends BackgroundPanel {
    private final Runnable back;
    private final Map<AudioSettings.Channel, JSlider> volumes = new EnumMap<>(AudioSettings.Channel.class);
    private final Map<AudioSettings.Channel, JCheckBox> mutes = new EnumMap<>(AudioSettings.Channel.class);
    private final JLabel status = GameTheme.label("修改后点击保存；取消会放弃未保存的修改。", 12, GameTheme.MUTED);

    public GlobalSettingsPanel(Runnable back) {
        this.back = Objects.requireNonNull(back);
        setLayout(new BorderLayout(0, 24));
        setBorder(new EmptyBorder(24, 28, 28, 28));
        JPanel header = GameTheme.transparent(new BorderLayout());
        JButton returnButton = GameTheme.button("‹ 返回首页", false);
        returnButton.setPreferredSize(new Dimension(128, 42));
        returnButton.addActionListener(e -> cancel());
        header.add(returnButton, BorderLayout.WEST);
        header.add(GameTheme.label("GLOBAL SETTINGS", 12, GameTheme.GOLD), BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        JPanel center = GameTheme.transparent(new GridBagLayout());
        JPanel stack = GameTheme.transparent(null);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setPreferredSize(new Dimension(400, 470));
        stack.add(GameTheme.title("全局设置", 32));
        stack.add(Box.createVerticalStrut(20));
        JPanel card = GameTheme.card();
        card.setLayout(new BorderLayout());
        card.add(buildAudio());
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        stack.add(card);
        center.add(stack);
        add(center, BorderLayout.CENTER);
        JPanel footer = GameTheme.transparent(new BorderLayout(0, 12));
        footer.add(status, BorderLayout.NORTH);
        JPanel buttons = GameTheme.transparent(new GridLayout(1, 3, 10, 0));
        JButton defaults = GameTheme.button("恢复默认", false);
        defaults.addActionListener(e -> {
            fillAudio(new AudioSettings.Snapshot(0.60, 0.75, false, false));
            status.setText("默认音量已填入，点击保存后生效。");
        });
        JButton cancel = GameTheme.button("取消", false);
        cancel.addActionListener(e -> cancel());
        JButton save = GameTheme.button("保存设置", true);
        save.addActionListener(e -> {
            volumes.forEach((channel, slider) -> AudioSettings.global().setVolume(channel, slider.getValue() / 100.0));
            mutes.forEach((channel, checkbox) -> AudioSettings.global().setMuted(channel, checkbox.isSelected()));
            status.setForeground(GameTheme.ACCENT);
            status.setText("音量设置已生效，本次运行中跨局保留。");
        });
        buttons.add(defaults); buttons.add(cancel); buttons.add(save);
        footer.add(buttons, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        reload();
    }

    public void reload() {
        fillAudio(AudioSettings.global().snapshot());
        status.setForeground(GameTheme.MUTED);
        status.setText("修改后点击保存；取消会放弃未保存的修改。");
    }

    private void fillAudio(AudioSettings.Snapshot snapshot) {
        volumes.forEach((channel, slider) -> slider.setValue((int) Math.round(snapshot.volume(channel) * 100)));
        mutes.forEach((channel, checkbox) -> checkbox.setSelected(snapshot.muted(channel)));
    }

    private void cancel() { reload(); back.run(); }

    private JPanel buildAudio() {
        JPanel panel = GameTheme.transparent(null);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(new EmptyBorder(24, 12, 12, 12));
        panel.add(GameTheme.label("调整属于你的飞行声场", 17, GameTheme.TEXT));
        panel.add(Box.createVerticalStrut(8));
        panel.add(GameTheme.label("保存后生效，本次运行中跨局保留", 12, GameTheme.MUTED));
        panel.add(Box.createVerticalStrut(26));
        panel.add(audioRow("背景音乐", AudioSettings.Channel.MUSIC));
        panel.add(Box.createVerticalStrut(28));
        panel.add(audioRow("战斗音效", AudioSettings.Channel.EFFECTS));
        panel.add(Box.createVerticalGlue());
        return panel;
    }

    private JPanel audioRow(String label, AudioSettings.Channel channel) {
        JPanel row = GameTheme.transparent(new BorderLayout(8, 8));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        JPanel top = GameTheme.transparent(new BorderLayout());
        JLabel title = GameTheme.label(label, 16, GameTheme.TEXT);
        JLabel percent = GameTheme.label("", 14, GameTheme.GOLD);
        top.add(title, BorderLayout.WEST);
        top.add(percent, BorderLayout.EAST);
        row.add(top, BorderLayout.NORTH);
        JSlider slider = new JSlider(0, 100);
        slider.setOpaque(false);
        slider.setPreferredSize(new Dimension(240, 34));
        slider.setUI(new BasicSliderUI(slider) {
            @Override public void paintTrack(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int y = trackRect.y + trackRect.height / 2 - 3;
                g.setColor(GameTheme.LINE);
                g.fillRoundRect(trackRect.x, y, trackRect.width, 6, 6, 6);
                g.setColor(GameTheme.ACCENT);
                g.fillRoundRect(trackRect.x, y, Math.max(0, xPositionForValue(slider.getValue()) - trackRect.x), 6, 6, 6);
                g.dispose();
            }
            @Override public void paintThumb(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(GameTheme.GOLD);
                g.fillOval(thumbRect.x, thumbRect.y + (thumbRect.height - 16) / 2, 16, 16);
                g.dispose();
            }
            @Override protected Dimension getThumbSize() { return new Dimension(16, 24); }
        });
        slider.addChangeListener(e -> percent.setText(slider.getValue() + "%"));
        slider.getAccessibleContext().setAccessibleName(label + "音量");
        JCheckBox mute = new JCheckBox("静音");
        mute.setOpaque(false);
        mute.setForeground(GameTheme.TEXT);
        mute.setFont(GameTheme.font(14, Font.PLAIN));
        volumes.put(channel, slider);
        mutes.put(channel, mute);
        row.add(slider, BorderLayout.CENTER);
        row.add(mute, BorderLayout.EAST);
        return row;
    }

}
