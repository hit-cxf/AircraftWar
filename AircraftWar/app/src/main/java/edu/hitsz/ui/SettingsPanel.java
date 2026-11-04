package edu.hitsz.ui;

import edu.hitsz.config.ConfigKey;
import edu.hitsz.config.GameConfig;
import edu.hitsz.config.GameConfigStore;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicSpinnerUI;
import java.awt.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** 所有玩法字段从元数据构建；编辑草稿在保存前不会改变当前游戏配置。 */
public final class SettingsPanel extends BackgroundPanel {
    private final Runnable back;
    private final Map<ConfigKey, JSpinner> editors = new EnumMap<>(ConfigKey.class);
    private final JComboBox<String> sectionSelector = new JComboBox<>();
    private final JPanel sections = GameTheme.transparent(new CardLayout());
    private final JLabel status = GameTheme.label("修改后点击保存；返回或取消会放弃未保存的修改。", 12, GameTheme.MUTED);

    public SettingsPanel(Runnable back) {
        this.back = Objects.requireNonNull(back);
        setLayout(new BorderLayout(0, 16));
        setBorder(new EmptyBorder(20, 22, 22, 22));
        JPanel header = GameTheme.transparent(null);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        JPanel nav = GameTheme.transparent(new BorderLayout());
        JButton backButton = GameTheme.button("‹ 返回", false);
        backButton.setPreferredSize(new Dimension(94, 40));
        backButton.addActionListener(e -> discardAndBack());
        nav.add(backButton, BorderLayout.WEST);
        nav.add(GameTheme.label("CUSTOM FLIGHT", 12, GameTheme.GOLD), BorderLayout.EAST);
        header.add(nav);
        header.add(Box.createVerticalStrut(18));
        header.add(GameTheme.title("自定义设置", 30));
        header.add(Box.createVerticalStrut(8));
        JLabel explanation = GameTheme.label("<html><div style='width:420px;text-align:center'>"
                + "仅自定义生效，标准难度使用固定预设。<br>自定义不记排行，保存后下一局生效。"
                + "</div></html>", 12, GameTheme.MUTED);
        header.add(explanation);
        add(header, BorderLayout.NORTH);

        JPanel body = GameTheme.card();
        body.setLayout(new BorderLayout());
        body.setBorder(new EmptyBorder(12, 12, 12, 12));
        body.add(buildGameplay(), BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        JPanel footer = GameTheme.transparent(new BorderLayout(0, 10));
        status.setHorizontalAlignment(SwingConstants.LEFT);
        status.setFont(GameTheme.font(11, Font.PLAIN));
        footer.add(status, BorderLayout.NORTH);
        JPanel actions = GameTheme.transparent(new GridLayout(1, 3, 10, 0));
        JButton defaults = GameTheme.button("恢复默认", false);
        JButton cancel = GameTheme.button("取消", false);
        JButton save = GameTheme.button("保存设置", true);
        defaults.addActionListener(e -> {
            fill(GameConfigStore.defaults());
            message("默认值已填入，点击保存后生效。", false);
        });
        cancel.addActionListener(e -> discardAndBack());
        save.addActionListener(e -> saveDraft());
        actions.add(defaults);
        actions.add(cancel);
        actions.add(save);
        footer.add(actions, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
        reload();
    }

    private JPanel buildGameplay() {
        JPanel panel = GameTheme.transparent(new BorderLayout(0, 10));
        panel.setBorder(new EmptyBorder(12, 5, 4, 5));
        Map<String, JPanel> groups = new LinkedHashMap<>();
        for (ConfigKey key : ConfigKey.values()) {
            if (!key.customEditable()) continue;
            JPanel group = groups.computeIfAbsent(key.section(), section -> {
                sectionSelector.addItem(section);
                JPanel content = new SettingsColumn();
                JLabel hint = GameTheme.label(section.contains("难度")
                        ? "<html><div style='width:265px'>速度 = 基础速度 + 得分 × 变化率"
                        + "（受速度上限限制）。阶段可调整生命倍率和射击/生成周期；速度仅按分数公式变化。</div></html>"
                        : "<html><div style='width:265px'>英雄机、敌机与道具参数也仅在自定义模式生效；数值范围显示在下方。</div></html>",
                        11, GameTheme.MUTED);
                hint.setHorizontalAlignment(SwingConstants.LEFT);
                hint.setAlignmentX(Component.LEFT_ALIGNMENT);
                content.add(hint);
                content.add(Box.createVerticalStrut(10));
                return content;
            });
            group.add(numericRow(key));
        }
        groups.forEach((section, content) -> {
            content.add(Box.createVerticalGlue());
            JScrollPane scroll = GameTheme.scroll(content);
            scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            sections.add(scroll, section);
        });
        sectionSelector.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() { return arrowButton(SwingConstants.SOUTH); }
        });
        sectionSelector.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focused) {
                super.getListCellRendererComponent(list, value, index, selected, focused);
                setBackground(selected ? new Color(35, 83, 91) : GameTheme.SURFACE);
                setForeground(GameTheme.TEXT);
                setBorder(new EmptyBorder(7, 12, 7, 12));
                return this;
            }
        });
        sectionSelector.setFont(GameTheme.font(15, Font.BOLD));
        sectionSelector.setForeground(GameTheme.TEXT);
        sectionSelector.setBackground(GameTheme.SURFACE);
        sectionSelector.setPreferredSize(new Dimension(200, 36));
        sectionSelector.addActionListener(e -> ((CardLayout) sections.getLayout())
                .show(sections, String.valueOf(sectionSelector.getSelectedItem())));
        panel.add(sectionSelector, BorderLayout.NORTH);
        panel.add(sections, BorderLayout.CENTER);
        return panel;
    }

    private JPanel numericRow(ConfigKey key) {
        JPanel row = GameTheme.transparent(new BorderLayout(10, 0));
        row.setBorder(new EmptyBorder(9, 0, 9, 0));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        row.setPreferredSize(new Dimension(360, 80));
        row.setMinimumSize(new Dimension(0, 80));
        JPanel labels = GameTheme.transparent(new BorderLayout(0, 3));
        JLabel name = GameTheme.label("<html><div style='width:165px'>" + key.label() + "</div></html>",
                14, GameTheme.TEXT);
        name.setHorizontalAlignment(SwingConstants.LEFT);
        String range = "范围 " + number(key.min()) + "～" + number(key.max());
        JLabel limits = GameTheme.label(range, 11, GameTheme.MUTED);
        limits.setHorizontalAlignment(SwingConstants.LEFT);
        labels.add(name, BorderLayout.CENTER);
        labels.add(limits, BorderLayout.SOUTH);
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(
                GameConfigStore.current().value(key), key.min(), key.max(), key.step()));
        spinner.setPreferredSize(new Dimension(112, 38));
        spinner.setMaximumSize(new Dimension(112, 38));
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, key.step() >= 1 ? "0" : "0.###");
        spinner.setEditor(editor);
        spinner.setUI(new BasicSpinnerUI() {
            @Override protected Component createNextButton() {
                JButton button = arrowButton(SwingConstants.NORTH);
                installNextButtonListeners(button);
                return button;
            }
            @Override protected Component createPreviousButton() {
                JButton button = arrowButton(SwingConstants.SOUTH);
                installPreviousButtonListeners(button);
                return button;
            }
        });
        JFormattedTextField field = editor.getTextField();
        field.setFont(GameTheme.font(15, Font.BOLD));
        field.setBackground(GameTheme.BACKGROUND);
        field.setForeground(GameTheme.TEXT);
        field.setCaretColor(GameTheme.ACCENT);
        field.setSelectionColor(new Color(48, 108, 117));
        field.setBorder(new EmptyBorder(6, 6, 6, 6));
        spinner.setBorder(BorderFactory.createLineBorder(GameTheme.LINE));
        spinner.setToolTipText(key.label() + "；" + range + "；配置名 " + key.key());
        name.setToolTipText(key.label());
        spinner.getAccessibleContext().setAccessibleName(key.label());
        JPanel control = GameTheme.transparent(new GridBagLayout());
        control.add(spinner);
        editors.put(key, spinner);
        row.add(labels, BorderLayout.CENTER);
        row.add(control, BorderLayout.EAST);
        return row;
    }

    /** 路由进入时重新读取已保存值，避免跨局保留旧草稿。 */
    public void selectSection(String section) {
        reload();
        sectionSelector.setSelectedItem("自定义模式".equals(section) ? "自定义难度" : section);
    }

    public void reload() {
        fill(GameConfigStore.current());
        message("修改后点击保存；返回或取消会放弃未保存的修改。", false);
    }

    private void fill(GameConfig config) {
        editors.forEach((key, spinner) -> {
            spinner.setValue(config.value(key));
            // 数值未变时模型不会发事件，也要清除尚未提交的无效文本。
            if (spinner.getEditor() instanceof JSpinner.NumberEditor editor) {
                editor.getTextField().setValue(config.value(key));
            }
        });
    }

    private void discardAndBack() {
        reload();
        back.run();
    }

    private void saveDraft() {
        try {
            Map<ConfigKey, Double> values = new EnumMap<>(GameConfigStore.current().values());
            for (var entry : editors.entrySet()) {
                try { entry.getValue().commitEdit(); }
                catch (ParseException error) {
                    sectionSelector.setSelectedItem(entry.getKey().section());
                    entry.getValue().requestFocusInWindow();
                    throw new IllegalArgumentException(entry.getKey().label() + "请输入有效数值。", error);
                }
                values.put(entry.getKey(), ((Number) entry.getValue().getValue()).doubleValue());
            }
            GameConfigStore.save(GameConfig.from(values));
            message("已保存。下一局自定义模式将使用这些参数。", true);
        } catch (IOException | RuntimeException error) {
            message("保存失败，请检查提示并修正；当前修改仍保留。", false);
            DialogHost.find(this).message("无法保存设置", error.getMessage());
        }
    }

    private void message(String text, boolean success) {
        status.setForeground(success ? GameTheme.ACCENT : GameTheme.MUTED);
        status.setText("<html><div style='width:420px'>" + text + "</div></html>");
    }

    private static String number(double value) {
        return new DecimalFormat("0.###").format(value);
    }

    private static JButton arrowButton(int direction) {
        JButton button = new BasicArrowButton(direction, GameTheme.SURFACE,
                GameTheme.LINE, GameTheme.ACCENT, GameTheme.SURFACE);
        button.setBorder(BorderFactory.createEmptyBorder());
        button.setPreferredSize(new Dimension(20, 18));
        return button;
    }

    private static final class SettingsColumn extends JPanel implements Scrollable {
        private SettingsColumn() {
            setOpaque(false);
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) { return Math.max(24, visible.height - 24); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}
