package edu.hitsz.application;

import java.nio.file.Path;

/** 游戏难度及其独立的成绩文件。 */
public enum Difficulty {
    EASY("简单", "easy.tsv"),
    NORMAL("普通", "normal.tsv"),
    HARD("困难", "hard.tsv"),
    CUSTOM("自定义", null);

    private final String displayName;
    private final String scoreFileName;

    Difficulty(String displayName, String scoreFileName) {
        this.displayName = displayName;
        this.scoreFileName = scoreFileName;
    }

    public String displayName() {
        return displayName;
    }

    public Path scoreFile(Path scoresDirectory) {
        if (this == CUSTOM) {
            throw new IllegalStateException("自定义模式不记录排行榜");
        }
        return scoresDirectory.resolve(scoreFileName);
    }
}
