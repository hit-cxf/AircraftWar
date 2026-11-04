package edu.hitsz.ui;

import javax.swing.JLabel;
import javax.swing.SwingConstants;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;

/** 首页专用的几何艺术字；字体随程序打包，绘制保持矢量清晰度。 */
final class HomeTitle extends JLabel {
    private static final String TEXT = "飞机大战";
    private static final Font TYPEFACE = loadTypeface();
    private final Shape lettering;
    private final Rectangle2D bounds;

    HomeTitle() {
        super(TEXT, SwingConstants.CENTER);
        setName("home-title");
        setFont(TYPEFACE.deriveFont(Font.PLAIN, 74f));
        setAlignmentX(CENTER_ALIGNMENT);
        setPreferredSize(new Dimension(340, 82));
        setMinimumSize(new Dimension(200, 60));
        setMaximumSize(new Dimension(340, 82));
        Shape glyphs = getFont().createGlyphVector(new FontRenderContext(null, true, true), TEXT).getOutline();
        lettering = AffineTransform.getShearInstance(-.10, 0).createTransformedShape(glyphs);
        bounds = lettering.getBounds2D();
    }

    @Override protected void paintComponent(Graphics graphics) {
        if (getWidth() <= 20 || getHeight() <= 12) return;
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        double scale = Math.min((getWidth() - 20) / (bounds.getWidth() + 5),
                (getHeight() - 12) / (bounds.getHeight() + 5));
        AffineTransform placement = new AffineTransform();
        placement.translate((getWidth() - bounds.getWidth() * scale) / 2 - 2,
                (getHeight() - bounds.getHeight() * scale) / 2 - 2);
        placement.scale(scale, scale);
        placement.translate(-bounds.getX(), -bounds.getY());
        Shape face = placement.createTransformedShape(lettering);
        Rectangle2D faceBounds = face.getBounds2D();

        // 深青色的短立体侧面与浅金色金属面，呼应首页的海天配色。
        g.setColor(new Color(3, 13, 23, 185));
        g.fill(AffineTransform.getTranslateInstance(2.5, 4).createTransformedShape(face));
        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(39, 100, 111));
        g.draw(face);
        float top = (float) faceBounds.getMinY();
        float bottom = (float) faceBounds.getMaxY();
        g.setPaint(new LinearGradientPaint(0, top, 0, bottom,
                new float[]{0, .42f, .50f, .58f, 1},
                new Color[]{new Color(255, 244, 207), new Color(233, 204, 151),
                        new Color(151, 105, 58), new Color(249, 224, 173), new Color(196, 149, 87)}));
        g.fill(face);
        g.setStroke(new BasicStroke(.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(255, 239, 190, 205));
        g.draw(face);
        g.dispose();
    }

    private static Font loadTypeface() {
        String path = "/fonts/zcool-qingke-huangyou/ZCOOLQingKeHuangYou-Regular.ttf";
        try (InputStream input = HomeTitle.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("缺少首页字体资源");
            return Font.createFont(Font.TRUETYPE_FONT, input);
        } catch (IOException | FontFormatException error) {
            System.err.println("首页艺术字体加载失败，使用系统字体：" + error.getMessage());
            return GameTheme.font(74, Font.BOLD);
        }
    }
}
