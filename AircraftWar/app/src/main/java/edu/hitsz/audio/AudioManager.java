package edu.hitsz.audio;

import javafx.application.Platform;
import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** JavaFX 官方引擎负责解码、混音与播放；播放器状态只在 JavaFX 线程操作。 */
public class AudioManager implements AutoCloseable {
    private static final String[] EFFECT_PATHS = {
            "/videos/bullet_hit.wav", "/videos/get_supply.wav", "/videos/bomb_explosion.wav",
            "/videos/enemy_explosion.wav", "/videos/game_over.wav"
    };
    // 短音效跨局复用，AudioClip 自身支持重叠播放；仅由 JavaFX 线程访问。
    private static final Map<String, AudioClip> EFFECTS = new HashMap<>();
    private final AudioSettings settings;
    private final Map<String, MediaPlayer> backgrounds = new HashMap<>();
    private final Set<AudioClip> usedEffects = new HashSet<>();
    private final AtomicBoolean closed = new AtomicBoolean();
    private volatile boolean requested;
    private MediaPlayer background;
    private boolean paused;

    public AudioManager() { this(AudioSettings.global()); }

    public AudioManager(AudioSettings settings) {
        this.settings = Objects.requireNonNull(settings);
    }

    /** JavaFX 初始化后在首页预加载短音效，正式播放时复用 AudioClip。 */
    public static void preload() {
        onFx(() -> {
            for (String path : EFFECT_PATHS) {
                try { effect(path); }
                catch (RuntimeException error) { report(path, error); }
            }
        });
    }

    public void playBackground(String resourcePath) {
        execute(() -> {
            MediaPlayer next = backgrounds.computeIfAbsent(resourcePath, path -> {
                MediaPlayer player = new MediaPlayer(new Media(source(path)));
                player.setCycleCount(MediaPlayer.INDEFINITE);
                player.setOnError(() -> report(path, player.getError()));
                return player;
            });
            if (background != next) {
                if (background != null) background.stop();
                background = next;
            }
            background.setVolume(settings.snapshot().effectiveVolume(AudioSettings.Channel.MUSIC));
            if (!paused) background.play();
        });
    }

    public void stopBackground() {
        execute(() -> {
            if (background != null) background.stop();
            background = null;
        });
    }

    public void setPaused(boolean paused) {
        execute(() -> {
            if (this.paused == paused) return;
            this.paused = paused;
            if (paused) usedEffects.forEach(AudioClip::stop);
            if (background == null) return;
            if (paused) background.pause();
            else {
                background.setVolume(settings.snapshot().effectiveVolume(AudioSettings.Channel.MUSIC));
                background.play();
            }
        });
    }

    public void playEffect(String resourcePath) {
        execute(() -> {
            double volume = settings.snapshot().effectiveVolume(AudioSettings.Channel.EFFECTS);
            if (paused || volume == 0) return;
            AudioClip clip = effect(resourcePath);
            usedEffects.add(clip);
            clip.play(volume);
        });
    }

    private void execute(Runnable command) {
        if (closed.get()) return;
        requested = true;
        onFx(() -> {
            if (closed.get()) return;
            try { command.run(); }
            catch (RuntimeException error) { report("播放", error); }
        });
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true) || !requested) return;
        onFx(() -> {
            usedEffects.forEach(AudioClip::stop);
            usedEffects.clear();
            backgrounds.values().forEach(MediaPlayer::dispose);
            backgrounds.clear();
            background = null;
        });
    }

    private static AudioClip effect(String path) {
        return EFFECTS.computeIfAbsent(path, key -> new AudioClip(source(key)));
    }

    private static String source(String path) {
        Objects.requireNonNull(path, "音频路径不能为空");
        URL resource = AudioManager.class.getResource(path.startsWith("/") ? path : "/" + path);
        if (resource == null) throw new IllegalArgumentException("音频资源不存在：" + path);
        return resource.toExternalForm();
    }

    private static void onFx(Runnable command) {
        if (Platform.isFxApplicationThread()) command.run();
        else Platform.runLater(command);
    }

    private static void report(String path, Throwable error) {
        System.err.println("音频播放失败：" + path + "（" + error + "）");
    }
}
