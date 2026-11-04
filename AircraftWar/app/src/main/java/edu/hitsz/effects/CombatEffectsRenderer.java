package edu.hitsz.effects;

import edu.hitsz.basic.AbstractFlyingObject;
import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.ui.GameTheme;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.image.Image;
import javafx.scene.paint.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

import java.io.InputStream;

/** 特效只负责绘制，不参与碰撞、计分或原始飞机图片的修改。 */
public final class CombatEffectsRenderer {
    private static final ColorAdjust WHITE_FLASH = new ColorAdjust(0, -1, 1, 0);
    private static class Images {
        static final Image FROST = load("frost_overlay.png");
        static final Image EXPLOSION = load("explosion.png");
    }

    public void paintFreezeMask(GraphicsContext gc, double opacity, double width, double height) {
        if (opacity <= 0) {
            return;
        }
        gc.save();
        gc.setGlobalAlpha(opacity);
        gc.setFill(Color.rgb(75, 175, 240, 0.07));
        gc.fillRect(0, 0, width, height);
        gc.setFill(new RadialGradient(0, 0, width / 2, height / 2, height * 0.72,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.45, Color.rgb(120, 215, 255, 0.02)),
                new Stop(1, Color.rgb(100, 205, 255, 0.38))));
        gc.fillRect(0, 0, width, height);
        gc.setStroke(Color.rgb(190, 240, 255, 0.55));
        gc.setLineWidth(3);
        gc.strokeRect(2, 2, width - 4, height - 4);
        gc.restore();
    }

    public void paintFrost(GraphicsContext gc, AbstractFlyingObject object) {
        if (!object.hasMovementEffect() || object.notValid()) {
            return;
        }
        double width = Math.max(26, object.getWidth() * 1.24);
        double height = Math.max(26, object.getHeight() * 1.24);
        gc.save();
        gc.setGlobalAlpha(0.83);
        gc.drawImage(Images.FROST, object.getLocationX() - width / 2,
                object.getLocationY() - height / 2, width, height);
        gc.restore();
    }

    public void paintExplosions(GraphicsContext gc, CombatEffects effects) {
        for (CombatEffects.Shockwave wave : effects.shockwaves()) {
            double p = wave.progress();
            double radius = 12 + wave.size() * p;
            gc.save();
            gc.setStroke(Color.rgb(255, 219, 133, Math.pow(1 - p, 1.7) * 0.85));
            gc.setLineWidth(2 + (1 - p) * 7);
            gc.strokeOval(wave.x() - radius, wave.y() - radius, radius * 2, radius * 2);
            gc.restore();
        }
        for (CombatEffects.Explosion explosion : effects.explosions()) {
            double p = explosion.progress();
            double size = explosion.size() * (0.48 + 0.9 * Math.sin(p * Math.PI / 2));
            gc.save();
            gc.translate(explosion.x(), explosion.y());
            gc.rotate(p * 22);
            gc.setGlobalAlpha(Math.pow(1 - p, 0.8));
            gc.drawImage(Images.EXPLOSION, -size / 2, -size / 2, size, size);
            gc.setFill(Color.rgb(255, 247, 208, Math.max(0, 0.7 - p * 4)));
            gc.fillOval(-size * 0.12, -size * 0.12, size * 0.24, size * 0.24);
            gc.restore();
        }
    }

    public void paintHitFlash(GraphicsContext gc, AbstractFlyingObject object, double opacity) {
        if (opacity <= 0) return;
        gc.save();
        gc.setEffect(WHITE_FLASH);
        gc.setGlobalAlpha(opacity * 0.9);
        Image image = object.getImage();
        gc.drawImage(image, object.getLocationX() - image.getWidth() / 2,
                object.getLocationY() - image.getHeight() / 2);
        gc.restore();
    }

    public void paintHits(GraphicsContext gc, CombatEffects effects) {
        gc.save();
        for (CombatEffects.Hit hit : effects.hits()) {
            double p = hit.progress();
            gc.setGlobalAlpha(1 - p);
            gc.setStroke(Color.rgb(255, 231, 143));
            gc.setLineWidth(2.2 * (1 - p) + 0.5);
            for (int i = 0; i < 8; i++) {
                double a = Math.PI * 2 * i / 8 + 0.28;
                double r = 4 + p * (18 + i % 3 * 4);
                gc.strokeLine(hit.x() + Math.cos(a) * r * 0.35,
                        hit.y() + Math.sin(a) * r * 0.35,
                        hit.x() + Math.cos(a) * r, hit.y() + Math.sin(a) * r);
            }
        }
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font(GameTheme.FX_FONT_FAMILY, FontWeight.BOLD, 23));
        for (CombatEffects.Recovery recovery : effects.recoveries()) {
            double p = recovery.progress();
            gc.setGlobalAlpha(Math.min(1, (1 - p) * 2.5));
            double y = recovery.y() - 48 * p;
            gc.setStroke(Color.rgb(12, 42, 30));
            gc.setLineWidth(3);
            gc.strokeText("+" + recovery.amount() + " HP", recovery.x(), y);
            gc.setFill(Color.rgb(135, 255, 183));
            gc.fillText("+" + recovery.amount() + " HP", recovery.x(), y);
        }
        gc.restore();
    }

    public void paintDamageOverlay(GraphicsContext gc, CombatEffects effects, double width, double height) {
        double opacity = effects.damageOpacity();
        if (opacity <= 0) return;
        gc.save();
        gc.setFill(new RadialGradient(0, 0, width / 2, height / 2, height * 0.7,
                false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT),
                new Stop(0.42, Color.TRANSPARENT),
                new Stop(1, Color.rgb(240, 35, 60, opacity * 0.74))));
        gc.fillRect(0, 0, width, height);
        gc.restore();
    }

    public void paintBossCharge(GraphicsContext gc, BossEnemy boss, double time) {
        if (!boss.isCharging()) return;
        double p = boss.chargeProgress();
        double radius = Math.max(boss.getWidth(), boss.getHeight()) * (0.7 - p * 0.2);
        gc.save();
        gc.setStroke(Color.rgb(255, 127, 194, 0.65 + Math.sin(time * 22) * 0.2));
        gc.setLineWidth(3);
        gc.strokeOval(boss.getLocationX() - radius, boss.getLocationY() - radius,
                radius * 2, radius * 2);
        gc.setFill(Color.rgb(255, 210, 240, p * 0.65));
        double core = 15 + p * 30;
        gc.fillOval(boss.getLocationX() - core / 2, boss.getLocationY() - core / 2, core, core);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFont(Font.font(GameTheme.FX_FONT_FAMILY, FontWeight.BOLD, 15));
        gc.setFill(Color.rgb(255, 222, 240));
        gc.fillText("二阶段 · 蓄力中", boss.getLocationX(),
                boss.getLocationY() + boss.getHeight() / 2.0 + 24);
        gc.restore();
    }

    private static Image load(String name) {
        String path = "/images/effects/" + name;
        try (InputStream input = CombatEffectsRenderer.class.getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalStateException("特效图片不存在：" + path);
            }
            Image image = new Image(input);
            if (image.isError()) {
                throw new IllegalStateException("特效图片损坏：" + path, image.getException());
            }
            return image;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("无法读取特效图片：" + path, e);
        }
    }
}
