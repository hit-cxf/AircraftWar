package edu.hitsz.application;

import edu.hitsz.dao.FileScoreDao;
import edu.hitsz.dao.ScoreDao;
import edu.hitsz.ui.DifficultySelectionPanel;
import edu.hitsz.ui.RankingPanel;
import edu.hitsz.ui.HomePanel;
import edu.hitsz.ui.SettingsPanel;
import edu.hitsz.ui.GlobalSettingsPanel;
import edu.hitsz.ui.CustomResultPanel;
import edu.hitsz.ui.DialogHost;
import edu.hitsz.ui.GameTheme;
import edu.hitsz.config.GameConfigStore;
import edu.hitsz.audio.AudioManager;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.function.IntConsumer;

/** Swing 主窗口通过 CardLayout 切换菜单与嵌入的 JavaFX 游戏。 */
public class Main {
    public static final int WINDOW_WIDTH = 512;
    public static final int WINDOW_HEIGHT = 768;

    private final JFrame frame = new JFrame("飞机大战");
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final DialogHost dialogs = new DialogHost(content);
    private final JFXPanel gamePanel = new JFXPanel();
    // 只在 JavaFX 线程读写。
    private Game game;
    // 只在 Swing EDT 读写。
    private boolean closing;
    private JPanel settingsPanel;
    private JPanel resultPanel;

    public static void main(String[] args) {
        // 避免 macOS 混合刷新率双屏下，Swing Metal 提交画面时阻塞鼠标事件。
        // 必须早于 Swing/JavaFX 初始化；显式传入的 JVM 参数仍优先。
        if (System.getProperty("os.name", "").startsWith("Mac")) {
            System.getProperties().putIfAbsent("sun.java2d.metal", "false");
        }
        System.out.println("Aircraft War Start!");
        SwingUtilities.invokeLater(() -> {
            GameTheme.install();
            Main application = new Main();
            AudioManager.preload();
            application.show();
            try {
                GameConfigStore.current();
            } catch (RuntimeException | ExceptionInInitializerError error) {
                application.dialogs.message("配置加载失败", error.getMessage()
                        + "\n请检查工作目录 config/custom.properties。");
            }
        });
    }

    private void show() {
        Platform.setImplicitExit(false);
        content.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        dialogs.setPreferredSize(new Dimension(WINDOW_WIDTH, WINDOW_HEIGHT));
        content.add(new HomePanel(this::showDifficulties, () -> showSettings(false)), "home");
        content.add(new DifficultySelectionPanel(this::startGame, this::showHome,
                () -> showSettings(true)), "menu");
        content.add(gamePanel, "game");
        frame.setContentPane(dialogs);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                shutdown();
            }
        });
        frame.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                Platform.runLater(() -> {
                    if (game != null) game.pauseForFocusLoss();
                });
            }
        });
        frame.pack();
        frame.setResizable(false);
        frame.setLocationRelativeTo(null);
        cards.show(content, "home");
        frame.setVisible(true);
    }

    private void showHome() {
        clearResult();
        dialogs.dismiss();
        cards.show(content, "home");
        frame.setTitle("飞机大战");
    }

    private void showDifficulties() {
        clearResult();
        dialogs.dismiss();
        cards.show(content, "menu");
        frame.setTitle("飞机大战 — 选择难度");
    }

    private void showSettings(boolean custom) {
        clearResult();
        dialogs.dismiss();
        // 每次打开读取当前快照，离开时丢弃未保存草稿。
        if (settingsPanel != null) content.remove(settingsPanel);
        if (custom) {
            SettingsPanel customPanel = new SettingsPanel(this::showDifficulties);
            customPanel.selectSection("自定义难度");
            settingsPanel = customPanel;
        } else {
            settingsPanel = new GlobalSettingsPanel(this::showHome);
        }
        content.add(settingsPanel, "settings");
        cards.show(content, "settings");
        frame.setTitle(custom ? "飞机大战 — 自定义设置" : "飞机大战 — 全局设置");
    }

    private void startGame(Difficulty difficulty) {
        clearResult();
        dialogs.dismiss();
        ScoreDao dao = difficulty == Difficulty.CUSTOM ? null
                : new FileScoreDao(difficulty.scoreFile(Path.of("scores")));
        frame.setTitle("飞机大战 — " + difficulty.displayName());
        cards.show(content, "game");
        Platform.runLater(() -> {
            if (game != null) {
                game.stop();
            }
            IntConsumer finished = score -> {
                LocalDateTime endedAt = LocalDateTime.now();
                SwingUtilities.invokeLater(() -> {
                    if (closing) {
                        return;
                    }
                    if (difficulty == Difficulty.CUSTOM) {
                        showResult(new CustomResultPanel(score,
                                () -> startGame(Difficulty.CUSTOM), this::showMenu), "自定义结果");
                        return;
                    }
                    RankingPanel ranking = new RankingPanel(dao, difficulty,
                            score, endedAt, this::showMenu);
                    // 结果页先隐藏挂载，姓名输入层仍覆盖刚结束的游戏画面。
                    prepareResult(ranking);
                    ranking.promptToSave(() -> {
                        if (closing || resultPanel != ranking) return;
                        showResult(ranking, difficulty.displayName() + "排行榜");
                        ranking.onShown();
                    });
                });
            };
            game = switch (difficulty) {
                case EASY -> new EasyGame(finished);
                case NORMAL -> new NormalGame(finished);
                case HARD -> new HardGame(finished);
                case CUSTOM -> new CustomGame(finished);
            };
            gamePanel.setScene(new Scene(game, WINDOW_WIDTH, WINDOW_HEIGHT));
            game.action();
            game.requestFocus();
            SwingUtilities.invokeLater(() -> {
                gamePanel.requestFocusInWindow();
                if (!frame.isFocused()) {
                    Platform.runLater(() -> { if (game != null) game.pauseForFocusLoss(); });
                }
            });
        });
    }

    private void showResult(JPanel panel, String title) {
        dialogs.dismiss();
        prepareResult(panel);
        cards.show(content, "result");
        frame.setTitle("飞机大战 — " + title);
    }

    private void prepareResult(JPanel panel) {
        if (resultPanel == panel) return;
        clearResult();
        resultPanel = panel;
        content.add(panel, "result");
    }

    private void clearResult() {
        if (resultPanel == null) return;
        content.remove(resultPanel);
        resultPanel = null;
    }

    private void showMenu() {
        if (closing) {
            return;
        }
        clearResult();
        dialogs.dismiss();
        Platform.runLater(() -> {
            if (game != null) {
                game.stop();
                game = null;
            }
            gamePanel.setScene(null);
        });
        frame.setTitle("飞机大战");
        cards.show(content, "menu");
    }

    private void shutdown() {
        if (closing) {
            return;
        }
        closing = true;
        dialogs.dismiss();
        frame.dispose();
        Platform.runLater(() -> {
            if (game != null) {
                game.stop();
            }
            gamePanel.setScene(null);
            Platform.exit();
        });
    }
}
