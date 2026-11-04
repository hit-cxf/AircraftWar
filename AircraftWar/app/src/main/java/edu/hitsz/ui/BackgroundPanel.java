package edu.hitsz.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.HierarchyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;

/** 真实天空插画配合轻微漂浮与光点；不可见时自动停止动画。 */
public class BackgroundPanel extends JPanel {
    private static final BufferedImage SKY = loadSky();
    private final Timer animation;
    private double phase;

    public BackgroundPanel() {
        setOpaque(true);
        setBackground(GameTheme.BACKGROUND);
        animation = new Timer(33, e -> {
            phase += 0.033;
            repaint();
        });
        addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
                if (isShowing()) animation.start(); else animation.stop();
            }
        });
    }

    private static BufferedImage loadSky() {
        try (var stream = BackgroundPanel.class.getResourceAsStream("/images/ui/sky-home.png")) {
            return stream == null ? null : ImageIO.read(stream);
        } catch (IOException error) {
            return null;
        }
    }

    @Override public void removeNotify() {
        animation.stop();
        super.removeNotify();
    }

    @Override protected void paintComponent(Graphics graphics) {
        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (SKY != null) {
            double scale = Math.max((getWidth() + 12.0) / SKY.getWidth(),
                    (getHeight() + 12.0) / SKY.getHeight());
            int width = (int) Math.ceil(SKY.getWidth() * scale);
            int height = (int) Math.ceil(SKY.getHeight() * scale);
            int x = (getWidth() - width) / 2 + (int) Math.round(Math.sin(phase * 0.16) * 3);
            int y = (getHeight() - height) / 2 + (int) Math.round(Math.cos(phase * 0.13) * 3);
            g.drawImage(SKY, x, y, width, height, null);
        }
        g.setPaint(new GradientPaint(0, 0, new Color(3, 14, 27, 35),
                0, getHeight(), new Color(3, 14, 27, 65)));
        g.fillRect(0, 0, getWidth(), getHeight());
        for (int i = 0; i < 22; i++) {
            double seed = i * 0.61803398875;
            double x = ((seed % 1.0) * getWidth() + Math.sin(phase * 0.22 + i) * 12);
            double y = (((i * 0.381966 + 1 - phase * (0.003 + i % 3 * 0.001)) % 1 + 1) % 1) * getHeight();
            int alpha = (int) (25 + 45 * (0.5 + 0.5 * Math.sin(phase + i * 1.7)));
            g.setColor(new Color(237, 220, 170, alpha));
            int size = i % 4 == 0 ? 3 : 2;
            g.fillOval((int) x, (int) y, size, size);
        }
        g.dispose();
    }
}
