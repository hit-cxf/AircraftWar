package edu.hitsz.application;

import edu.hitsz.aircraft.*;
import edu.hitsz.dao.*;
import edu.hitsz.prop.*;
import edu.hitsz.ui.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.*;

/** 直接渲染项目组件的离屏预览；不操作桌面，不写成绩或配置。 */
public final class ExperiencePreview {
    public static void main(String[] args) throws Exception {
        Path output = Path.of(args[0]);
        Files.createDirectories(output);
        SwingUtilities.invokeAndWait(() -> {
            try { renderMenus(output); }
            catch (Exception error) { throw new RuntimeException(error); }
        });
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(ready::countDown);
        ready.await();
        try {
            FutureTask<Void> job = new FutureTask<>(() -> { renderBattle(output); return null; });
            Platform.runLater(job);
            job.get(30, TimeUnit.SECONDS);
            SwingUtilities.invokeAndWait(() -> {
                try { renderEndDialog(output); }
                catch (Exception error) { throw new RuntimeException(error); }
            });
        } finally { Platform.exit(); }
        contactSheet(output);
        System.out.println("Experience preview: " + output.toAbsolutePath());
    }

    private static void renderMenus(Path output) throws Exception {
        GameTheme.install();
        writePanel(new HomePanel(() -> {}, () -> {}), output.resolve("home.png"), 512, 768);
        writePanel(new DifficultySelectionPanel(mode -> {}, () -> {}, () -> {}),
                output.resolve("difficulty.png"), 512, 768);
        SettingsPanel custom = new SettingsPanel(() -> {});
        custom.selectSection("英雄机");
        writePanel(custom, output.resolve("hero-settings.png"), 512, 768);
        custom.selectSection("自定义难度");
        writePanel(custom, output.resolve("custom-settings.png"), 512, 768);
        writePanel(new GlobalSettingsPanel(() -> {}), output.resolve("global-settings.png"), 512, 768);
        ScoreDao dao = demoDao();
        RankingPanel ranking = new RankingPanel(dao, Difficulty.NORMAL, 180,
                LocalDateTime.of(2026, 11, 4, 14, 30), () -> {});
        DialogHost rankingHost = new DialogHost(ranking);
        ranking.reload();
        writePanel(rankingHost, output.resolve("ranking.png"), 512, 768);
        find(ranking, JTable.class).setRowSelectionInterval(0, 0);
        findButton(ranking, "删除选中记录").doClick();
        writePanel(rankingHost, output.resolve("ranking-delete.png"), 512, 768);
        rankingHost.dismiss();
        CustomResultPanel result = new CustomResultPanel(230, () -> {}, () -> {});
        writePanel(result, output.resolve("custom-result.png"), 512, 768);
    }

    private static ScoreDao demoDao() {
        return new ScoreDao() {
            public List<ScoreRecord> findAll() { return List.of(
                    new ScoreRecord("示例飞行员", 320, LocalDateTime.of(2026, 11, 4, 14, 30)),
                    new ScoreRecord("演示记录", 180, LocalDateTime.of(2026, 11, 4, 14, 31))); }
            public void save(ScoreRecord record) { throw new UnsupportedOperationException(); }
            public boolean delete(ScoreRecord record) { throw new UnsupportedOperationException(); }
        };
    }

    private static void renderEndDialog(Path output) throws Exception {
        CardLayout cards = new CardLayout();
        JPanel pages = new JPanel(cards);
        pages.add(new JLabel(new ImageIcon(ImageIO.read(output.resolve("game-over.png").toFile()))), "game");
        RankingPanel ranking = new RankingPanel(demoDao(), Difficulty.NORMAL, 180,
                LocalDateTime.of(2026, 11, 4, 14, 30), () -> {});
        pages.add(ranking, "ranking");
        cards.show(pages, "game");
        DialogHost host = new DialogHost(pages);
        host.setSize(512, 768);
        ranking.promptToSave(() -> {
            cards.show(pages, "ranking");
            ranking.onShown();
        });
        writePanel(host, output.resolve("ranking-name.png"), 512, 768);
        host.dismiss();
    }

    private static <T extends Component> T find(Container container, Class<T> type) {
        for (Component child : container.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container nested) {
                T match = find(nested, type);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static JButton findButton(Container container, String text) {
        for (Component child : container.getComponents()) {
            if (child instanceof JButton button && text.equals(button.getText())) return button;
            if (child instanceof Container nested) {
                JButton match = findButton(nested, text);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static void writePanel(Container panel, Path file, int width, int height) throws Exception {
        panel.setSize(width, height);
        layout(panel);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        panel.printAll(g);
        g.dispose();
        ImageIO.write(image, "png", file.toFile());
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) layout(nested);
        }
    }

    private static void renderBattle(Path output) throws Exception {
        try (EffectsFixture f = new EffectsFixture()) {
            Scene scene = new Scene(f.game, 512, 768);
            BossEnemy boss = new BossEnemy(256, 230, 2, 0, 300);
            boss.decreaseHp(180);
            boss.updateCombat(.4);
            f.enemies().add(boss);
            f.set("activeBoss", boss);
            f.set("score", 180);
            f.set("visualTime", 1.5);
            HeroAircraft hero = HeroAircraft.getInstance();
            hero.enableScatterShoot();
            hero.decreaseHp(360);
            EliteEnemy enemy = new EliteEnemy(100, 430, 0, 6, 60);
            f.enemies().add(enemy);
            f.effects().flash(enemy);
            f.effects().addHit(100, 430);
            f.effects().addRecovery(256, 610, 20);
            f.props().add(new FireProp(400, 520, 0, 3));
            snapshot(f, scene, output.resolve("battle.png"));
            f.game.pauseForFocusLoss();
            snapshot(f, scene, output.resolve("pause.png"));
            f.game.resume();
            f.set("bossIncoming", true);
            f.set("bossWarningTicks", 22);
            snapshot(f, scene, output.resolve("warning.png"));
            f.set("bossIncoming", false);
            f.effects().heroDamaged();
            hero.decreaseHp(Integer.MAX_VALUE);
            f.call("checkResultAction");
            snapshot(f, scene, output.resolve("game-over.png"));
        }
    }

    private static void snapshot(EffectsFixture f, Scene scene, Path file) throws Exception {
        f.game.applyCss(); f.game.layout(); f.call("render");
        BufferedImage image = SwingFXUtils.fromFXImage(scene.snapshot(new WritableImage(512, 768)), null);
        ImageIO.write(image, "png", file.toFile());
    }

    private static void contactSheet(Path output) throws Exception {
        String[] files = {"home", "difficulty", "custom-settings", "battle", "pause", "ranking"};
        String[] labels = {"动态首页", "难度选择与独立齿轮", "自定义数值设置", "战斗 HUD 与 Boss 二阶段", "暂停与继续", "标准模式排行榜"};
        BufferedImage sheet = new BufferedImage(1536, 1664, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(8, 23, 39)); g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setFont(GameTheme.font(20, Font.BOLD));
        for (int i = 0; i < files.length; i++) {
            int x = i % 3 * 512;
            int y = i / 3 * 832;
            g.setColor(Color.WHITE); g.drawString(labels[i], x + 20, y + 40);
            BufferedImage image = ImageIO.read(output.resolve(files[i] + ".png").toFile());
            double scale = Math.min(512.0 / image.getWidth(), 768.0 / image.getHeight());
            int w = (int) Math.round(image.getWidth() * scale);
            int h = (int) Math.round(image.getHeight() * scale);
            g.drawImage(image, x + (512 - w) / 2, y + 64, w, h, null);
        }
        g.dispose();
        ImageIO.write(sheet, "png", output.resolve("overview.png").toFile());
        BufferedImage results = new BufferedImage(1536, 832, BufferedImage.TYPE_INT_RGB);
        Graphics2D rg = results.createGraphics();
        rg.setColor(new Color(8, 23, 39)); rg.fillRect(0, 0, 1536, 832);
        rg.setFont(GameTheme.font(20, Font.BOLD));
        String[] names = {"ranking", "ranking-name", "ranking-delete"};
        String[] titles = {"主窗口内的排行榜", "内部姓名输入", "内部删除确认"};
        for (int i = 0; i < names.length; i++) {
            rg.setColor(Color.WHITE); rg.drawString(titles[i], i * 512 + 20, 40);
            rg.drawImage(ImageIO.read(output.resolve(names[i] + ".png").toFile()), i * 512, 64, null);
        }
        rg.dispose();
        ImageIO.write(results, "png", output.resolve("results-overview.png").toFile());
        String[] flowNames = {"game-over", "ranking-name", "ranking"};
        String[] flowTitles = {"坠毁后立即静止并停声", "保存弹窗覆盖结束画面", "关闭弹窗后进入排行榜"};
        Graphics2D fg = results.createGraphics();
        fg.setColor(new Color(8, 23, 39)); fg.fillRect(0, 0, 1536, 832);
        fg.setFont(GameTheme.font(20, Font.BOLD));
        for (int i = 0; i < flowNames.length; i++) {
            fg.setColor(Color.WHITE); fg.drawString(flowTitles[i], i * 512 + 20, 40);
            fg.drawImage(ImageIO.read(output.resolve(flowNames[i] + ".png").toFile()), i * 512, 64, null);
        }
        fg.dispose();
        ImageIO.write(results, "png", output.resolve("results-flow.png").toFile());
    }
}
