package edu.hitsz.dao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 排行榜业务规则，界面与控制台可共同使用。 */
public class RankingService {
    private final ScoreDao dao;

    public RankingService(ScoreDao dao) {
        this.dao = dao;
    }

    public List<ScoreRecord> ranking() {
        List<ScoreRecord> records = new ArrayList<>(dao.findAll());
        records.sort(Comparator.comparingInt(ScoreRecord::getScore).reversed()
                .thenComparing(ScoreRecord::getRecordedAt));
        return records;
    }

    public void save(String name, int score, LocalDateTime endedAt) {
        String playerName = name == null ? "" : name.strip();
        if (playerName.isEmpty() || playerName.length() > 30
                || playerName.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("请输入 1～30 个字符的姓名，不能包含制表符或换行。 ");
        }
        dao.save(new ScoreRecord(playerName, score, endedAt));
    }

    public boolean delete(ScoreRecord record) {
        return dao.delete(record);
    }
}
