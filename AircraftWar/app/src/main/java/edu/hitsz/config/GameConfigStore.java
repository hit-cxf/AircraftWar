package edu.hitsz.config;

import edu.hitsz.application.Difficulty;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/** 固定难度预设 + 自定义模式覆盖文件；编辑快照与本局运行快照相互独立。 */
public final class GameConfigStore {
    private GameConfigStore() { }

    private static final class DefaultsHolder {
        private static final GameConfig VALUE = readDefaults();
    }

    private static final class StoreHolder {
        private static final Repository VALUE = createStore();
    }

    public static GameConfig current() { return StoreHolder.VALUE.current(); }
    public static GameConfig defaults() { return DefaultsHolder.VALUE; }

    /** 标准难度始终返回内置预设；自定义才读取用户编辑值，不改变当前对局。 */
    public static GameConfig forDifficulty(Difficulty difficulty) {
        return StoreHolder.VALUE.forDifficulty(difficulty);
    }

    /** 开始一局时固定快照；后续保存设置只影响下一局自定义模式。 */
    public static GameConfig activate(Difficulty difficulty) {
        return StoreHolder.VALUE.activate(difficulty);
    }

    public static GameConfig active() { return StoreHolder.VALUE.active(); }
    public static Path file() { return Path.of("config", "custom.properties").toAbsolutePath().normalize(); }
    public static void save(GameConfig config) throws IOException { StoreHolder.VALUE.save(config); }
    public static void restoreDefaults() throws IOException { save(defaults()); }

    /** 重读外部编辑过的文件；无效或读取失败时保留当前快照并抛出异常。 */
    public static GameConfig reload() throws IOException { return StoreHolder.VALUE.reload(); }

    private static Repository createStore() {
        Repository store = new Repository(file(), defaults());
        try {
            store.reload();
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("自定义配置读取失败，使用内置默认配置：" + e.getMessage());
        }
        return store;
    }

    private static GameConfig readDefaults() {
        try (InputStream input = GameConfigStore.class.getResourceAsStream("/config/game-defaults.properties")) {
            if (input == null) throw new IllegalStateException("缺少内置玩法配置文件");
            Properties properties = new Properties();
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
            return GameConfig.from(parse(properties));
        } catch (IOException | IllegalArgumentException e) {
            throw new IllegalStateException("内置玩法配置无效", e);
        }
    }

    private static Map<ConfigKey, Double> parse(Properties properties) {
        EnumMap<ConfigKey, Double> values = new EnumMap<>(ConfigKey.class);
        for (String property : properties.stringPropertyNames()) {
            ConfigKey key = ConfigKey.fromKey(property);
            try {
                values.put(key, Double.parseDouble(properties.getProperty(property).trim()));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(key.label() + "不是有效数值", e);
            }
        }
        return values;
    }

    /** 独立存储实例供测试使用临时目录；静态入口只在应用实际调用时访问工作目录。 */
    static final class Repository {
        private final Path file;
        private final GameConfig defaults;
        private final AtomicReference<GameConfig> current;
        private volatile GameConfig active;

        Repository(Path file, GameConfig defaults) {
            this.file = file.toAbsolutePath().normalize();
            this.defaults = Objects.requireNonNull(defaults);
            this.current = new AtomicReference<>(this.defaults);
            this.active = this.defaults;
        }

        GameConfig current() { return current.get(); }
        GameConfig active() { return active; }

        GameConfig forDifficulty(Difficulty difficulty) {
            return Objects.requireNonNull(difficulty).name().equals("CUSTOM") ? current() : defaults;
        }

        GameConfig activate(Difficulty difficulty) {
            GameConfig candidate = forDifficulty(difficulty);
            active = candidate;
            return candidate;
        }

        synchronized GameConfig reload() throws IOException {
            EnumMap<ConfigKey, Double> values = new EnumMap<>(defaults.values());
            if (Files.exists(file)) {
                Properties overrides = new Properties();
                try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                    overrides.load(reader);
                }
                Map<ConfigKey, Double> parsed = parse(overrides);
                for (ConfigKey key : parsed.keySet()) {
                    if (!key.customEditable()) {
                        throw new IllegalArgumentException("标准难度预设不可覆盖：" + key.key());
                    }
                }
                values.putAll(parsed);
            }
            GameConfig candidate = GameConfig.from(values);
            current.set(candidate);
            return candidate;
        }

        synchronized void save(GameConfig config) throws IOException {
            GameConfig candidate = GameConfig.from(config.values());
            for (ConfigKey key : ConfigKey.values()) {
                if (!key.customEditable() && Double.compare(candidate.value(key), defaults.value(key)) != 0) {
                    throw new IllegalArgumentException("标准难度预设不可修改：" + key.key());
                }
            }
            Path parent = file.getParent();
            Files.createDirectories(parent);
            Path temporary = Files.createTempFile(parent, ".custom-", ".properties.tmp");
            try {
                try (Writer writer = Files.newBufferedWriter(temporary, StandardCharsets.UTF_8)) {
                    writer.write("# AircraftWar custom-mode overrides; standard difficulty presets are read-only.\n");
                    for (ConfigKey key : ConfigKey.values()) {
                        double value = candidate.value(key);
                        if (key.customEditable() && Double.compare(value, defaults.value(key)) != 0) {
                            writer.write(key.key() + "=" + format(value) + "\n");
                        }
                    }
                }
                try {
                    Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
                }
                current.set(candidate);
            } finally {
                Files.deleteIfExists(temporary);
            }
        }

        private static String format(double value) {
            return value == Math.rint(value) ? Long.toString((long) value) : Double.toString(value);
        }
    }
}
