package edu.hitsz.dao;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class FileScoreDao implements ScoreDao {
    private final Path filePath;

    public FileScoreDao(Path filePath) {
        this.filePath = filePath;
    }

    @Override
    public List<ScoreRecord> findAll() {
        if (Files.notExists(filePath)) {
            return new ArrayList<>();
        }

        try {
            List<String> lines = Files.readAllLines(filePath, StandardCharsets.UTF_8);
            List<ScoreRecord> records = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String[] fields = lines.get(i).split("\t", -1);
                if (fields.length != 3) {
                    throw new IllegalStateException("成绩文件第 " + (i + 1) + " 行格式错误: " + filePath);
                }
                try {
                    records.add(new ScoreRecord(fields[0], Integer.parseInt(fields[1]),
                            LocalDateTime.parse(fields[2])));
                } catch (RuntimeException e) {
                    throw new IllegalStateException("成绩文件第 " + (i + 1) + " 行格式错误: " + filePath, e);
                }
            }
            return records;
        } catch (IOException e) {
            throw new UncheckedIOException("读取成绩失败: " + filePath, e);
        }
    }

    @Override
    public void save(ScoreRecord record) {
        String line = record.getPlayerName() + "\t"
                + record.getScore() + "\t"
                + record.getRecordedAt() + System.lineSeparator();
        try {
            Path parent = filePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(filePath, line, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("保存成绩失败: " + filePath, e);
        }
    }

    @Override
    public boolean delete(ScoreRecord record) {
        List<ScoreRecord> records = findAll();
        boolean removed = false;
        for (int i = 0; i < records.size(); i++) {
            ScoreRecord item = records.get(i);
            if (item.getPlayerName().equals(record.getPlayerName())
                    && item.getScore() == record.getScore()
                    && item.getRecordedAt().equals(record.getRecordedAt())) {
                records.remove(i);
                removed = true;
                break;
            }
        }
        if (!removed) {
            return false;
        }

        StringBuilder content = new StringBuilder();
        for (ScoreRecord item : records) {
            content.append(item.getPlayerName()).append('\t')
                    .append(item.getScore()).append('\t')
                    .append(item.getRecordedAt()).append(System.lineSeparator());
        }
        try {
            Files.writeString(filePath, content, StandardCharsets.UTF_8,
                    StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("删除成绩失败: " + filePath, e);
        }
        return true;
    }
}
