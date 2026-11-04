package edu.hitsz.dao;

import java.util.List;

public interface ScoreDao {
    List<ScoreRecord> findAll();
    void save(ScoreRecord record);
    boolean delete(ScoreRecord record);
}
