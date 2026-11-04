package edu.hitsz.effects;

import edu.hitsz.aircraft.BossEnemy;
import edu.hitsz.aircraft.HeroAircraft;
import edu.hitsz.application.Difficulty;
import edu.hitsz.ui.GameTheme;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.TextAlignment;

/** 固定在窗口上的战斗信息，不随镜头抖动。 */
public final class GameHudRenderer {
    private static final Color PANEL = Color.rgb(8, 19, 35, 0.88);
    private static final Color MUTED = Color.rgb(166, 189, 211);
    private static final Color CYAN = Color.rgb(113, 231, 255);

    public void paint(GraphicsContext gc, int score, Difficulty difficulty, HeroAircraft hero,
                      BossEnemy boss, int affected, boolean permanent, double time,
                      double width, double height) {
        gc.save();
        panel(gc, 14, 12, width - 28, 84);
        text(gc, "SCORE", 28, 33, 10, MUTED, false);
        text(gc, String.format("%06d", score), 27, 59, 27, Color.WHITE, true);
        text(gc, difficulty.displayName() + "模式", 28, 80, 11, CYAN, false);
        text(gc, "HULL / 生命", 177, 35, 11, MUTED, false);
        double health = hero.getHp() / (double) hero.getMaxHp();
        Color healthColor = health < 0.25 ? Color.rgb(255, 103, 115) : Color.rgb(109, 238, 183);
        bar(gc, 177, 45, width - 288, 10, health, healthColor);
        text(gc, hero.getHp() + " / " + hero.getMaxHp(), 177, 78, 13, healthColor, true);
        gc.setFill(Color.rgb(46, 67, 90));
        gc.fillRoundRect(width - 78, 24, 50, 30, 10, 10);
        text(gc, "Ⅱ", width - 60, 45, 17, Color.WHITE, true);
        text(gc, "Esc 暂停", width - 85, 78, 11, MUTED, false);

        if (boss != null && !boss.notValid()) {
            panel(gc, 14, 104, width - 28, 48);
            Color accent = boss.isSecondPhase() ? Color.rgb(255, 137, 200) : Color.rgb(255, 180, 102);
            text(gc, boss.isSecondPhase() ? "BOSS · 阶段 02" : "BOSS · 阶段 01", 28, 124, 12, accent, true);
            text(gc, boss.isCharging() ? "蓄力中 · 注意躲避" : "保持移动，寻找空隙", 28, 141, 10, MUTED, false);
            bar(gc, 205, 116, width - 235, 10, boss.getHp() / (double) boss.getMaxHp(), accent);
            gc.setTextAlign(TextAlignment.RIGHT);
            text(gc, boss.getHp() + " / " + boss.getMaxHp(), width - 30, 142, 11, accent, true);
            gc.setTextAlign(TextAlignment.LEFT);
        }

        long remaining = hero.getFireRemainingMillis();
        panel(gc, 14, height - 74, 218, 58);
        if (remaining > 0) {
            double seconds = remaining / 1000.0;
            Color accent = remaining <= 3000 && Math.sin(time * 9) > 0
                    ? Color.rgb(255, 202, 119) : CYAN;
            text(gc, String.format("散射强化    %.1f s", seconds), 28, height - 51, 13, accent, true);
            bar(gc, 28, height - 39, 190, 5, remaining / (double) hero.getFireDurationMillis(), accent);
            text(gc, remaining <= 3000 ? "强化即将结束" : "拾取火力道具刷新时限", 28, height - 23, 10, MUTED, false);
        } else {
            text(gc, "直射 · 自动开火", 28, height - 50, 13, CYAN, true);
            text(gc, "环射道具：立即齐射一圈", 28, height - 28, 10, MUTED, false);
        }
        if (affected > 0) {
            panel(gc, 244, height - 74, width - 258, 58);
            text(gc, "冻结 / 减速 · " + affected + " 架", 258, height - 50, 13,
                    Color.rgb(178, 232, 255), true);
            text(gc, permanent ? "含永久冻结 · 以机身冰霜为准" : "冰霜消失后恢复移动", 258, height - 28, 10, MUTED, false);
        }
        gc.restore();
    }

    public void paintBossWarning(GraphicsContext gc, double progress, double width, double height) {
        gc.save();
        double alpha = 0.78 + Math.sin(progress * Math.PI * 8) * 0.16;
        double top = height * 0.36;
        gc.setFill(Color.rgb(44, 10, 25, alpha));
        gc.fillRect(0, top, width, 110);
        gc.setStroke(Color.rgb(255, 120, 134));
        gc.setLineWidth(2);
        gc.strokeLine(0, top, width, top);
        gc.strokeLine(0, top + 110, width, top + 110);
        gc.setTextAlign(TextAlignment.CENTER);
        text(gc, "W A R N I N G", width / 2, top + 45, 29, Color.rgb(255, 150, 157), true);
        text(gc, "BOSS 即将入场 · 准备躲避弹幕", width / 2, top + 77, 14, Color.WHITE, false);
        gc.restore();
    }

    public void paintPause(GraphicsContext gc, double width, double height, boolean focusLoss) {
        gc.save();
        gc.setFill(Color.rgb(3, 10, 23, 0.72));
        gc.fillRect(0, 0, width, height);
        panel(gc, 62, height / 2 - 112, width - 124, 224);
        gc.setTextAlign(TextAlignment.CENTER);
        text(gc, "已暂停", width / 2, height / 2 - 53, 32, Color.WHITE, true);
        text(gc, focusLoss ? "已切换窗口，为你暂停战斗" : "休息一下，战斗状态已保留", width / 2,
                height / 2 - 17, 14, MUTED, false);
        gc.setFill(Color.rgb(83, 211, 238));
        gc.fillRoundRect(width / 2 - 96, height / 2 + 18, 192, 46, 12, 12);
        text(gc, "继续战斗  /  Esc", width / 2, height / 2 + 47, 16, Color.rgb(7, 29, 48), true);
        text(gc, "散射、冰冻与 Boss 蓄力计时均已暂停", width / 2, height / 2 + 91, 11, MUTED, false);
        gc.restore();
    }

    public boolean isPauseButton(double x, double y, double width) {
        return x >= width - 78 && x <= width - 28 && y >= 24 && y <= 54;
    }

    public boolean isResumeButton(double x, double y, double width, double height) {
        return x >= width / 2 - 96 && x <= width / 2 + 96
                && y >= height / 2 + 18 && y <= height / 2 + 64;
    }

    private static void panel(GraphicsContext gc, double x, double y, double w, double h) {
        gc.setFill(PANEL);
        gc.fillRoundRect(x, y, w, h, 15, 15);
        gc.setStroke(Color.rgb(157, 218, 255, 0.22));
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, w, h, 15, 15);
    }

    private static void bar(GraphicsContext gc, double x, double y, double w, double h,
                            double progress, Color accent) {
        gc.setFill(Color.rgb(255, 255, 255, 0.12));
        gc.fillRoundRect(x, y, w, h, h, h);
        gc.setFill(accent);
        gc.fillRoundRect(x, y, w * Math.max(0, Math.min(1, progress)), h, h, h);
    }

    private static void text(GraphicsContext gc, String value, double x, double y,
                             double size, Color color, boolean bold) {
        gc.setFont(Font.font(GameTheme.FX_FONT_FAMILY, bold ? FontWeight.BOLD : FontWeight.NORMAL, size));
        gc.setFill(color);
        gc.fillText(value, x, y);
    }
}
