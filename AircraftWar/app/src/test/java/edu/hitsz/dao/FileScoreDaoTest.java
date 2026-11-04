package edu.hitsz.dao;

import edu.hitsz.application.Difficulty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileScoreDaoTest {
    @TempDir
    Path tempDir;

    @Test
    void savesReadsAndDeletesRecords() {
        ScoreDao dao = new FileScoreDao(tempDir.resolve("normal.tsv"));
        ScoreRecord first = new ScoreRecord("Player", 100, LocalDateTime.of(2026, 10, 22, 12, 0));
        ScoreRecord second = new ScoreRecord("Player", 200, LocalDateTime.of(2026, 10, 22, 12, 1));

        assertTrue(dao.findAll().isEmpty());
        dao.save(first);
        dao.save(second);
        assertEquals(2, dao.findAll().size());
        assertEquals(200, dao.findAll().get(1).getScore());

        assertTrue(dao.delete(first));
        assertEquals(1, dao.findAll().size());
        assertEquals(200, dao.findAll().get(0).getScore());
        assertFalse(dao.delete(first));
    }

    @Test
    void keepsEachDifficultyInItsOwnFile() {
        ScoreDao easy = new FileScoreDao(Difficulty.EASY.scoreFile(tempDir));
        ScoreDao normal = new FileScoreDao(Difficulty.NORMAL.scoreFile(tempDir));
        ScoreDao hard = new FileScoreDao(Difficulty.HARD.scoreFile(tempDir));

        easy.save(new ScoreRecord("Player", 10, LocalDateTime.of(2026, 10, 27, 12, 0)));
        hard.save(new ScoreRecord("Player", 30, LocalDateTime.of(2026, 10, 27, 12, 1)));

        assertEquals(10, easy.findAll().get(0).getScore());
        assertTrue(normal.findAll().isEmpty());
        assertEquals(30, hard.findAll().get(0).getScore());
    }
}
