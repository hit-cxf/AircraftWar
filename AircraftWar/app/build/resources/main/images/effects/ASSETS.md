# 本地特效素材

2026-11-04 使用 Codex 内置 `image_gen` 生成，未使用 CLI/API fallback。素材用于本地游戏效果预览；本轮不替换课程平台上的既有提交。

| 文件 | 尺寸 | 用途 |
| --- | --- | --- |
| `frost_overlay.png` | 1254 × 1254，RGBA | 中心留空的蓝白冰晶环；按受影响敌机的边界缩放叠加，中心透明以保留飞机轮廓。 |
| `explosion.png` | 1254 × 1254，RGBA | 单个俯视爆炸峰值图；由程序配合缩放、旋转、淡出、光圈和火花生成短动画。 |

两张图片均保留生成时的真实 alpha，不是将棋盘格画入背景。原始生成文件仍保留在 Codex generated_images 目录。

## 最终生成提示词

### 冰霜覆盖层

Use case: stylized-concept. Asset type: one transparent ice-coating rim overlay sprite for a top-down 2D aircraft shooter. Create a square PNG with REAL ALPHA TRANSPARENCY. This is an OPEN ICE RING, not a full ice disk. A completely transparent EMPTY HOLE occupies the entire central 60% of the canvas width and height; absolutely no white fill, no opaque frost, no translucent film, no blur or shadow in that hole. Only a thin broken ring of pale cyan-white ice crystals along an approximately oval outline from 65% to 85% of canvas size. A few jagged clear blue ice spikes and fine white frost veins cluster around that rim. Transparent empty padding outside. Clear center is mandatory so an underlying airplane remains visible. Style clean semi-realistic hand-painted arcade VFX, crisp highlights, ice edges, a little icy glow, restrained density. No plane, no background, no checkerboard, no text, no watermark. Center must be fully empty and transparent.

### 爆炸

Use case: stylized-concept. Asset type: one transparent 2D explosion sprite for a top-down aircraft shooter. Create a single centered explosive fireball at its peak, seen directly from above. Bright yellow-white small core, rich orange flame petals exploding radially, a modest irregular charcoal smoke edge, a few tiny gold sparks, circular dynamic silhouette. Semi-realistic hand-painted arcade game VFX with crisp luminous edges, readable when scaled to 100px. Actual transparent alpha background, all edges softly dissipating into transparency, generous transparent padding at least 12% each side, keep all sparks within canvas. No aircraft or debris silhouettes, no environment, no shadows on a surface, no text, no logo, no watermark, no checkerboard. ONE single explosion only, not a sprite sheet.

## 短爆炸音效

`../../videos/enemy_explosion.wav` 为本项目按确定性随机种子生成的 0.30 秒单声道 PCM 音效（22050 Hz，16 bit）：低通噪声、下滑低频正弦及指数衰减包络合成，无外部采样。用于常规敌机坠毁；炸弹与 Boss 继续使用项目已有的 `bomb_explosion.wav`。
