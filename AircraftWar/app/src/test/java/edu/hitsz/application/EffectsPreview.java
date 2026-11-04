package edu.hitsz.application;

import edu.hitsz.aircraft.*;
import edu.hitsz.prop.*;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.*;

/** 输出当前真实游戏渲染的静态预览，不打开或控制桌面窗口。 */
public final class EffectsPreview {
    public static void main(String[] args) throws Exception {
        CountDownLatch ready = new CountDownLatch(1);
        Platform.startup(ready::countDown); ready.await();
        try {
            FutureTask<Void> job = new FutureTask<>(() -> { render(Path.of(args[0])); return null; });
            Platform.runLater(job); job.get(30, TimeUnit.SECONDS);
        } finally { Platform.exit(); }
    }

    private static void render(Path output) throws Exception {
        BufferedImage sheet = new BufferedImage(1536, 832, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(14, 24, 40)); g.fillRect(0, 0, 1536, 832);
        g.setColor(Color.WHITE); g.setFont(new Font("PingFang SC", Font.BOLD, 22));
        String[] titles = {"冰冻蒙板与冰霜覆盖", "炸弹冲击波与坠毁爆炸", "一次环射，保留原弹道"};
        for (int mode = 0; mode < 3; mode++) {
            try (EffectsFixture fixture = new EffectsFixture()) {
                Scene scene = new Scene(fixture.game, 512, 768);
                if (mode < 2) {
                    fixture.enemies().add(new MobEnemy(80, 225, 0, 6, 30));
                    fixture.enemies().add(new EliteEnemy(200, 310, 0, 6, 60));
                    fixture.enemies().add(new ElitePlusEnemy(370, 390, 2, 6, 90));
                    fixture.enemies().add(new EliteProEnemy(140, 480, 2, 6, 120));
                    fixture.enemies().add(new BossEnemy(256, 110, 2, 0, 300));
                    fixture.props().add(mode == 0 ? new FreezeProp(256, 670, 0, 0) : new BombProp(256, 670, 0, 0));
                    fixture.call("crashCheckAction"); fixture.call("postProcessAction");
                    fixture.effects().update(mode == 0 ? .3 : .16);
                } else {
                    HeroAircraft.getInstance().enableScatterShoot();
                    fixture.props().add(new SuperFireProp(256, 570, 0, 0));
                    HeroAircraft.getInstance().setLocation(256, 570);
                    fixture.call("tick");
                    for (int i = 0; i < 10; i++) fixture.call("bulletsMoveAction");
                    fixture.heroBullets().addAll(HeroAircraft.getInstance().shoot());
                }
                fixture.game.applyCss(); fixture.game.layout(); fixture.call("render");
                BufferedImage image = SwingFXUtils.fromFXImage(scene.snapshot(new WritableImage(512, 768)), null);
                g.drawImage(image, mode * 512, 64, null);
                g.drawString(titles[mode], mode * 512 + 28, 40);
            }
        }
        g.dispose(); Files.createDirectories(output.getParent()); ImageIO.write(sheet, "png", output.toFile());
        System.out.println("Effects preview: " + output);
    }
}
