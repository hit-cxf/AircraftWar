package edu.hitsz.dao;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RankingServiceTest {
    @TempDir Path directory;

    @Test
    void sortsByScoreThenTimeAndDeletesTheSelectedRecord() {
        var service = new RankingService(new FileScoreDao(directory.resolve("normal.tsv")));
        var time = LocalDateTime.of(2026, 10, 27, 12, 0);
        service.save("低分", 10, time);
        service.save(" 后来同分 ", 30, time.plusSeconds(1));
        service.save("先到同分", 30, time);
        assertEquals(List.of("先到同分", "后来同分", "低分"),
                service.ranking().stream().map(ScoreRecord::getPlayerName).toList());
        assertTrue(service.delete(service.ranking().get(1)));
        assertEquals(List.of("先到同分", "低分"),
                service.ranking().stream().map(ScoreRecord::getPlayerName).toList());
    }

    @Test
    void rejectsNamesThatWouldCorruptTheScoreFile() {
        var service = new RankingService(new FileScoreDao(directory.resolve("normal.tsv")));
        for (String name : List.of("", "   ", "a\tb", "a\nb", "a".repeat(31))) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.save(name, 10, LocalDateTime.now()));
        }
        assertTrue(service.ranking().isEmpty());
    }
}
