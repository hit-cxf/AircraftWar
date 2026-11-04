package edu.hitsz.config;

import edu.hitsz.application.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;

import static edu.hitsz.config.ConfigKey.*;
import static org.junit.jupiter.api.Assertions.*;

class GameConfigStoreTest {
    @TempDir Path directory;

    @Test
    void missingOverrideUsesDefaultsAndPartialOverrideMergesWithDefaults() throws IOException {
        Path file = directory.resolve("config/custom.properties");
        GameConfigStore.Repository repository = repository(file);
        assertEquals(GameConfigStore.defaults().values(), repository.reload().values());
        Files.createDirectories(file.getParent());
        Files.writeString(file, "hero_hp=2000\ncustom_speed_rate=0.025\n");
        GameConfig read = repository.reload();
        assertEquals(2000, read.integer(HERO_HP));
        assertEquals(.025, read.value(CUSTOM_SPEED_RATE));
        assertEquals(GameConfigStore.defaults().value(MOB_HP), read.value(MOB_HP));
    }

    @Test
    void savePersistsOnlyOverridesPublishesAfterWriteAndCanRestoreDefaults() throws IOException {
        Path file = directory.resolve("config/custom.properties");
        GameConfigStore.Repository repository = repository(file);
        EnumMap<ConfigKey, Double> changed = new EnumMap<>(GameConfigStore.defaults().values());
        changed.put(HERO_HP, 2000.0);
        changed.put(CUSTOM_SPEED_RATE, .025);
        GameConfig config = GameConfig.from(changed);
        repository.save(config);
        assertEquals(config.values(), repository.current().values());
        String written = Files.readString(file);
        assertTrue(written.contains("hero_hp=2000\n"));
        assertTrue(written.contains("custom_speed_rate=0.025\n"));
        assertFalse(written.contains("mob_hp="));
        assertEquals(config.values(), repository(file).reload().values());
        repository.save(GameConfigStore.defaults());
        assertFalse(Files.readString(file).contains("hero_hp="));
        assertEquals(GameConfigStore.defaults().values(), repository(file).reload().values());
        try (var files = Files.list(file.getParent())) {
            assertEquals(1, files.count(), "成功后不能遗留临时文件");
        }
    }

    @Test
    void malformedReloadRetainsPreviousSnapshotAndDoesNotRewriteFile() throws IOException {
        Path file = directory.resolve("custom.properties");
        GameConfigStore.Repository repository = repository(file);
        Files.writeString(file, "hero_hp=2000\n");
        GameConfig good = repository.reload();
        for (String bad : new String[]{"hero_hp=NaN\n", "hero_hp=abc\n", "unknown=42\n",
                "custom_elite_chance=0.9\ncustom_plus_chance=0.9\n", "fire_duration_ms=0\n"}) {
            Files.writeString(file, bad);
            assertThrows(IllegalArgumentException.class, repository::reload);
            assertSame(good, repository.current());
            assertEquals(bad, Files.readString(file));
        }
    }

    @Test
    void failedSaveDoesNotPublishOrDamageExistingContents() throws IOException {
        Path blockedParent = directory.resolve("blocked");
        Files.writeString(blockedParent, "existing file");
        GameConfigStore.Repository repository = repository(blockedParent.resolve("custom.properties"));
        GameConfig original = repository.current();
        EnumMap<ConfigKey, Double> changed = new EnumMap<>(original.values());
        changed.put(HERO_HP, 2000.0);
        assertThrows(IOException.class, () -> repository.save(GameConfig.from(changed)));
        assertSame(original, repository.current());
        assertEquals("existing file", Files.readString(blockedParent));
    }

    @Test
    void directoryInsteadOfOverrideFileDoesNotReplaceSnapshot() throws IOException {
        Path file = directory.resolve("custom.properties");
        Files.createDirectory(file);
        GameConfigStore.Repository repository = repository(file);
        GameConfig original = repository.current();
        assertThrows(IOException.class, repository::reload);
        assertSame(original, repository.current());
    }

    @Test
    void customValuesNeverChangeStandardPresetsAndSaveDoesNotChangeActiveRound() throws IOException {
        Path file = directory.resolve("custom.properties");
        GameConfigStore.Repository repository = repository(file);
        GameConfig defaults = GameConfigStore.defaults();
        assertSame(defaults, repository.active(), "尚未开局时使用固定默认值");
        EnumMap<ConfigKey, Double> changed = new EnumMap<>(defaults.values());
        changed.put(HERO_HP, 2000.0);
        changed.put(ENEMY_POWER, 50.0);
        changed.put(CUSTOM_BASE_SPEED, 9.5);
        GameConfig first = GameConfig.from(changed);
        repository.save(first);
        assertSame(defaults, repository.active(), "保存编辑不能自动激活自定义模式");
        for (Difficulty difficulty : new Difficulty[]{Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD}) {
            assertSame(defaults, repository.forDifficulty(difficulty));
            assertEquals(1000, repository.forDifficulty(difficulty).integer(HERO_HP));
            assertEquals(10, repository.forDifficulty(difficulty).integer(ENEMY_POWER));
        }
        GameConfig round = repository.activate(Difficulty.CUSTOM);
        assertEquals(2000, round.integer(HERO_HP));
        assertEquals(50, round.integer(ENEMY_POWER));
        changed.put(HERO_HP, 3000.0);
        repository.save(GameConfig.from(changed));
        assertSame(round, repository.active());
        assertEquals(2000, repository.active().integer(HERO_HP));
        assertEquals(3000, repository.forDifficulty(Difficulty.CUSTOM).integer(HERO_HP));
        assertEquals(3000, repository.activate(Difficulty.CUSTOM).integer(HERO_HP));
        assertSame(defaults, repository.activate(Difficulty.NORMAL));
        assertSame(defaults, repository.active());
    }

    @Test
    void presetEditsAreRejectedBeforeFileOrSnapshotsChange() throws IOException {
        Path file = directory.resolve("custom.properties");
        GameConfigStore.Repository repository = repository(file);
        repository.save(GameConfigStore.defaults());
        String originalContents = Files.readString(file);
        GameConfig original = repository.current();
        repository.activate(Difficulty.CUSTOM);
        EnumMap<ConfigKey, Double> values = new EnumMap<>(original.values());
        values.put(NORMAL_BASE_SPEED, 9.0);
        assertThrows(IllegalArgumentException.class, () -> repository.save(GameConfig.from(values)));
        assertEquals(originalContents, Files.readString(file));
        assertSame(original, repository.current());
        assertSame(original, repository.active());
        for (String forbidden : new String[]{"easy_base_speed=7\n", "normal_base_speed=8\n",
                "hard_enemy_max=10\n"}) {
            Files.writeString(file, forbidden);
            assertThrows(IllegalArgumentException.class, repository::reload);
            assertSame(original, repository.current());
            assertSame(original, repository.active());
        }
    }

    private static GameConfigStore.Repository repository(Path file) {
        return new GameConfigStore.Repository(file, GameConfigStore.defaults());
    }
}
