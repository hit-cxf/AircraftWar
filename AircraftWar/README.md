# 飞机大战：软件构造课程结项工程

本目录是可独立构建的完整 Gradle 工程。源码、资源、测试、设计模式类图与 Wrapper 均已包含。

## 技术栈

Java toolchain 21、JavaFX 21.0.2（controls / graphics / swing / media）、Swing、Gradle Wrapper 9.2.1、JUnit Jupiter 5.12.1、PlantUML。

实际开发工具为 VS Code，构建以本工程的 JDK 21 toolchain 为准。首次构建需要联网下载 Gradle、JavaFX 等依赖；本机没有 JDK 21 时 Foojay 会尝试下载对应 toolchain。无需另装全局 Gradle。

## 启动与测试

macOS 启动时默认使用 Swing 兼容渲染，处理内建屏上移动鼠标引发的卡顿；修改后需退出旧进程再启动。JavaFX 游戏与音频引擎保持不变。

解压后进入有 `gradlew` 的目录：

```bash
# macOS / Linux
bash ./gradlew :app:run
bash ./gradlew :app:test
bash ./gradlew :app:build
```

Windows PowerShell：

```powershell
.\gradlew.bat :app:run
.\gradlew.bat :app:test
```

入口为 `edu.hitsz.application.Main`。不要直接执行 `javac Main.java`，JavaFX 依赖及资源由 Gradle 统一管理。测试报告在执行测试后生成于 `app/build/reports/tests/test/index.html`。

## 操作

- 首页中央点击“开始游戏”进入难度选择；移动/拖动鼠标控制英雄机，自动射击。首页右上角齿轮打开全局音频设置，自定义模式旁的齿轮打开独立玩法设置。
- 加血、散射、环射、炸弹和冰冻道具由碰撞触发。散射持续 10 秒，重复拾取刷新时限；环射在拾取时立即齐射一圈 20 发子弹，之后继续原弹道，并保留散射剩余时间。
- 冰冻显示蓝色蒙板，受冻结或减速影响的敌机和敌弹覆盖冰霜；冰霜与各自的效果状态同步。炸弹带有冲击波、画面抖动和爆炸音效，所有类型敌机坠毁时播放爆炸动画。
- 游戏结束后冻结最后画面，停止背景音乐并播放结束音效，内部姓名输入层覆盖该画面；确认保存或取消关闭后才进入排行榜，页面与首页同为 512×768，可删除选中记录或返回菜单重开。
- 成绩文件为程序工作目录下 `scores/easy.tsv`、`normal.tsv`、`hard.tsv`。通过 `:app:run` 启动时位于 `app/scores/`，首次保存时自动创建。本包不包含个人历史成绩。
- 普通背景与 Boss 音乐支持循环和切换，命中、拾取、爆炸触发音效；坠毁后停止背景音乐，结束音效自然播完；返回菜单或关闭窗口时释放本局音频。
- Esc / 右上角按钮暂停；窗口失焦自动暂停，手动继续后恢复。散射、冰冻、Boss 蓄力和音乐同步暂停。
- Boss 入场预警、血条与半血二阶段，英雄生命条、散射倒计时、命中闪白火花、受伤红色边缘及回血飘字已接入。
- 自定义模式仅显示本局结果和重玩入口，不询问姓名，不写入或访问排行榜。
- 姓名输入、删除确认与设置错误提示统一使用窗口内遮罩弹窗；Enter 确认、Esc 取消，输入校验失败时保留姓名。
- 音频使用 JavaFX 官方引擎：首页预加载并复用 `AudioClip` 短音效，`MediaPlayer` 管理普通/Boss 音乐的循环、暂停与音量；坠毁后播放结束音效，退出时停止音效并 `dispose()` 背景播放器。无自写音频线程或 PCM 混音。
- 声卡操作不会阻塞游戏线程。卡顿恢复后每帧最多补算 5 个逻辑步，避免追算积压导致画面长时间无响应。

## 两类设置与配置文件

- **全局设置**：背景音乐、战斗音效的音量与独立静音；保存后在本次运行的各局间保留。
- **自定义设置**：57 项可调玩法数值，含英雄/各类敌机生命、双方伤害与弹道、道具时长、掉落、生成、Boss 和递进参数；保存后下一局自定义模式生效。
- 简单、普通、困难始终使用 `app/src/main/resources/config/game-defaults.properties` 中的固定预设，不受自定义覆盖影响。
- 自定义覆盖文件为工作目录 `config/custom.properties`（用 Gradle 运行时在 `app/config/`），首次保存时创建，仅写与默认不同的字段。
- 自定义纵向速度为 `min(上限, base + score × rate)`，单位像素/40ms逻辑步，支持小数；更新场上存活敌机但不会解除冰冻，Boss 使用独立水平速度。
- `ConfigKey` 描述字段，`GameConfig` 校验并保存不可变快照，`GameConfigStore` 负责原子持久化和开局选择。非法数值或矛盾范围无法保存。

## 实验开发记录

1. **系统分析与单例**：建立飞行物、飞机、子弹、道具继承体系；英雄机私有构造并通过 `getInstance()` 获取唯一实例；完善敌机、图片映射、道具移动与碰撞。
2. **简单工厂与工厂方法**：`PropFactory` 按 `PropType` 创建道具；`EnemyFactory` 及各具体工厂创建五类敌机。
3. **策略模式**：`AbstractAircraft.shoot()` 委托 `ShootStrategy`；独立封装不射击、单发、双发、散射、环射等弹道，并由火力道具切换。
4. **DAO 与测试**：`ScoreRecord`、`ScoreDao`、`FileScoreDao` 分离成绩与文件访问；按难度独立存储；JUnit 测试英雄机、文件往返与数据隔离。
5. **Swing 与多线程**：`CardLayout` 切换各页面，`JFXPanel` 嵌入 JavaFX，`RankingPanel` 展示排行榜，`DialogHost` 处理内部姓名输入和确认；`Runnable` 实现火力计时，JavaFX `AudioClip` / `MediaPlayer` 管理音频播放；关闭、重开时清理状态与线程。
6. **观察者与模板方法**：`PropEffectObserver` 定义两类通知，`AbstractEffectProp` 管理观察者；拾取时 `PropEffectAction` 注册存活对象、通知并统计新击毁数量。`Game.action()` 为 final 模板方法，`EasyGame`、`NormalGame`、`HardGame` 通过子类钩子实现初始参数、递进和 Boss 规则。

### 道具规则

| 对象 | 炸弹 | 冰冻 |
| --- | --- | --- |
| 普通敌机 | 坠毁 | 永久静止 |
| 精英敌机 | 坠毁 | 静止 4 秒 |
| 精锐敌机 | 坠毁 | 静止 3 秒 |
| 王牌敌机 | 扣 60 血，可击毁 | 速度降至 50%，持续 5 秒 |
| Boss | 无影响 | 无影响 |
| 敌弹 | 清除 | 静止 5 秒 |

每架敌机沿用 10 分；炸弹击毁不掉落道具，只对本次新击毁目标计分。冰冻限制移动，不额外改变射击策略。王牌伤害和减速比例是任务书允许自行确定的数值。

### 难度规则

- 简单：初始参数固定，无 Boss。
- 普通：每 20 秒缩短生成周期、提高后续新生成敌机速度和血量，双方射击周期不变；Boss 每次 300 血。
- 困难：除上述递进外，提高敌机射击频率、降低英雄机射击频率；Boss 血量为 `450 + 150 × (召唤次数 - 1)`。
- 普通和困难模式按每 100 分的阈值允许多次召唤，最多一架 Boss 同时存在。阶段和 Boss 参数会输出到控制台。
- 生成周期、射击周期和速度设有上下限，完整参数见三个子类。

## 目录与类图

- `app/src/main/java/edu/hitsz/`：application、aircraft、basic、bullet、prop、factory、strategy、observer、effects、config、dao、audio、ui。
- `app/src/main/resources/`：全部图片与音频。
- `app/src/test/java/`：覆盖数据、英雄机、道具计时、观察者、难度规则及特效状态与游戏集成的测试。
- `uml/`：PlantUML 源码；`uml/export/`：当前 PNG 类图。
- 期末重点类图：`ScoreDao.puml`、`Observer.puml`、`Template.puml`。
- 原仓库 `submission/`：提交核验清单、录屏提纲与此前上传包；本轮新功能待本人核验后统一替换提交。

原仓库的课件、参考示例与旧 Swing 基础版本不属于运行依赖，未放入本提交包。录屏由本人完成后上传，最终由本人核验并手动提交。

2026-11-04 的体验与自定义优化目前仅在本地核验，尚未替换在线作业草稿或已上传压缩包。素材来源见 `app/src/main/resources/images/effects/ASSETS.md` 和 `images/ui/ASSETS.md`。在原仓库运行 `bash ./gradlew :app:experiencePreview` 可生成 `output/experience/overview.png` 界面总览，`effectsPreview` 保留三项基础特效预览。
