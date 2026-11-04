package edu.hitsz.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Arrays;

/** 菜单、设置与排行榜共用的天空主题。 */
public final class GameTheme {
    public static final Color BACKGROUND = new Color(8, 23, 39);
    public static final Color SURFACE = new Color(17, 39, 57);
    public static final Color TEXT = new Color(237, 246, 246);
    public static final Color MUTED = new Color(157, 186, 196);
    public static final Color ACCENT = new Color(118, 207, 200);
    public static final Color GOLD = new Color(237, 199, 130);
    public static final Color LINE = new Color(58, 91, 105);
    public static final String FONT_FAMILY = chooseFont();
    // JavaFX 对部分系统中文字体集合的字重映射不同，逻辑字体保留可靠的粗细层级。
    public static final String FX_FONT_FAMILY = "SansSerif";

    private GameTheme() { }

    private static String chooseFont() {
        var available = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames());
        for (String family : new String[]{"PingFang SC", "Microsoft YaHei", "Noto Sans CJK SC", "SansSerif"}) {
            if (available.contains(family)) return family;
        }
        return Font.SANS_SERIF;
    }

    public static Font font(float size, int style) {
        return new Font(FONT_FAMILY, style, Math.round(size));
    }

    public static void install() {
        Font normal = font(14, Font.PLAIN);
        for (String key : new String[]{"Label.font", "Button.font", "ToggleButton.font",
                "CheckBox.font", "ComboBox.font", "TextField.font", "FormattedTextField.font",
                "Table.font", "TableHeader.font", "TabbedPane.font", "OptionPane.messageFont",
                "OptionPane.buttonFont", "Spinner.font"}) UIManager.put(key, normal);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("Panel.background", SURFACE);
        UIManager.put("OptionPane.messageForeground", TEXT);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("TabbedPane.background", SURFACE);
        UIManager.put("TabbedPane.foreground", TEXT);
        UIManager.put("TabbedPane.selected", new Color(37, 75, 87));
        UIManager.put("TabbedPane.contentAreaColor", SURFACE);
        UIManager.put("ComboBox.background", SURFACE);
        UIManager.put("ComboBox.foreground", TEXT);
        UIManager.put("TextField.background", BACKGROUND);
        UIManager.put("TextField.foreground", TEXT);
        UIManager.put("TextField.caretForeground", ACCENT);
    }

    public static JLabel label(String text, float size, Color color) {
        JLabel label = new JLabel(text, SwingConstants.CENTER);
        label.setFont(font(size, Font.PLAIN));
        label.setForeground(color);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    public static JLabel title(String text, float size) {
        JLabel label = label(text, size, TEXT);
        label.setFont(font(size, Font.BOLD));
        return label;
    }

    public static JButton button(String text, boolean primary) {
        return new RoundedButton(text, primary);
    }

    public static JButton gearButton(String accessibleName) {
        JButton button = button("", false);
        button.setIcon(new GearIcon());
        button.setPreferredSize(new Dimension(44, 44));
        button.setToolTipText(accessibleName);
        button.getAccessibleContext().setAccessibleName(accessibleName);
        return button;
    }

    public static JPanel card() {
        JPanel card = new JPanel() {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(12, 32, 49, 225));
                g.fillRoundRect(0, 0, getWidth(), getHeight(), 22, 22);
                g.setColor(new Color(106, 172, 179, 65));
                g.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g.dispose();
                super.paintComponent(graphics);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 18, 18, 18));
        return card;
    }

    public static JPanel transparent(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    public static JScrollPane scroll(Component component) {
        JScrollPane scroll = new JScrollPane(component);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(9, 0));
        scroll.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
            @Override protected JButton createDecreaseButton(int orientation) { return emptyButton(); }
            @Override protected JButton createIncreaseButton(int orientation) { return emptyButton(); }
            private JButton emptyButton() {
                JButton button = new JButton();
                button.setPreferredSize(new Dimension(0, 0));
                return button;
            }
            @Override protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
                graphics.setColor(BACKGROUND);
                graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
            }
            @Override protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(isThumbRollover() ? ACCENT : LINE);
                g.fillRoundRect(bounds.x + 1, bounds.y, Math.max(3, bounds.width - 2), bounds.height, 8, 8);
                g.dispose();
            }
        });
        return scroll;
    }

    private static final class RoundedButton extends JButton {
        private final boolean primary;

        private RoundedButton(String text, boolean primary) {
            super(text);
            this.primary = primary;
            setFont(font(primary ? 18 : 14, Font.BOLD));
            setForeground(primary ? BACKGROUND : TEXT);
            setBorder(new EmptyBorder(10, 18, 10, 18));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setPreferredSize(new Dimension(160, 44));
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color fill = primary ? GOLD : SURFACE;
            if (!isEnabled()) fill = new Color(54, 70, 79);
            else if (getModel().isPressed()) fill = primary ? GOLD.darker() : new Color(35, 83, 91);
            else if (getModel().isRollover()) fill = primary ? new Color(250, 216, 159) : new Color(31, 68, 82);
            g.setColor(fill);
            g.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            g.setColor(hasFocus() ? ACCENT : primary ? GOLD : LINE);
            g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 16, 16);
            g.dispose();
            super.paintComponent(graphics);
        }
    }

    private static final class GearIcon implements Icon {
        @Override public int getIconWidth() { return 22; }
        @Override public int getIconHeight() { return 22; }
        @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.translate(x + 11, y + 11);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Path2D gear = new Path2D.Double();
            for (int i = 0; i < 48; i++) {
                double angle = Math.PI * 2 * i / 48;
                double radius = (i % 6 == 1 || i % 6 == 2 || i % 6 == 3) ? 10.5 : 8;
                double px = Math.cos(angle) * radius;
                double py = Math.sin(angle) * radius;
                if (i == 0) gear.moveTo(px, py); else gear.lineTo(px, py);
            }
            gear.closePath();
            g.setColor(TEXT);
            g.fill(gear);
            g.setColor(SURFACE);
            g.fillOval(-4, -4, 8, 8);
            g.dispose();
        }
    }
}
