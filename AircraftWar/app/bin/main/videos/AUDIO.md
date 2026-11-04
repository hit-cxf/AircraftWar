# 音频播放与素材处理

运行时使用 JavaFX 官方 `AudioClip`（短音效）与 `MediaPlayer`（背景音乐）。

2026-11-04：将原 PCM 缓存中的前导静音裁剪预先落实到 WAV，保留约1ms起音余量。
仅去掉短音效开头振幅不超过16位量程32的片段，不改音量、采样率、声道或尾部；背景音乐不处理。

| 素材 | 去除开头时长 |
| --- | ---: |
| bullet_hit.wav | 22.426ms |
| get_supply.wav | 84.490ms |
| bomb_explosion.wav | 1.043ms |
| game_over.wav | 81.361ms |
| enemy_explosion.wav | 无需裁剪 |

原始文件保存在仓库 `output/diagnostics/audio-originals/`，一次性处理脚本为
`output/diagnostics/prepare-fx-audio.py`。此脚本不属于游戏启动流程。
