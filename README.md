# AircraftWar 飞机大战

软件构造课程实验项目，以飞机大战为案例学习 Java、面向对象建模、设计模式、文件读写、单元测试、图形界面和多线程。

当前主要开发版本位于 [`AircraftWar`](AircraftWar)。`JavaSwing/AircraftWar-base` 为保留的 Swing 基础版本。本文记录截至 **2026-11-04** 的开发状态。

## 技术栈

| 技术                 | 当前配置与用途                                                                                            |
| -------------------- | --------------------------------------------------------------------------------------------------------- |
| Java                 | Java toolchain 固定为 **JDK 21**                                                                    |
| JavaFX               | **21.0.2**，使用 `controls`、`graphics`、`swing`、`media` 模块，绘制游戏并通过 `JFXPanel` 嵌入 Swing |
| Gradle Wrapper       | **9.2.1**，负责依赖管理、编译、运行和测试                                                           |
| Gradle Kotlin DSL    | 使用 `build.gradle.kts` 配置构建                                                                        |
| JUnit Jupiter        | **5.12.1**，测试英雄机和成绩文件 DAO                                                                |
| PlantUML             | 维护继承关系和设计模式类图                                                                                |
| Java NIO / Java Time | 使用 `Files`、`Path` 读写成绩文件，使用 `LocalDateTime` 记录时间                                    |
| Guava                | **33.4.6-jre**，构建模板中保留的依赖                                                                |

课程指导书中的环境要求与当前工程配置存在差异：指导书使用较新的 JDK 和 IntelliJ IDEA，当前工程实际使用 JDK 21 toolchain，也可通过 VS Code 和命令行开发。

## 目录结构

```text
AircraftWar
│   ├── docs/gradle_base.md     # Gradle 命令参考
│   └── AircraftWar/            # 当前开发工程，Gradle 命令在此执行
│       ├── gradlew / gradlew.bat
│       ├── gradle/             # Wrapper 和依赖版本配置
│       ├── settings.gradle.kts
│       ├── uml/                # PlantUML 源文件
│       │   └── export/         # 已导出的类图
│       └── app/
│           ├── build.gradle.kts
│           └── src/
│               ├── main/java/edu/hitsz/
│               │   ├── application/  # 启动、游戏循环、图片管理、鼠标控制、难度
│               │   ├── basic/        # 飞行物公共父类
│               │   ├── aircraft/     # 英雄机与各类敌机
│               │   ├── bullet/       # 子弹
│               │   ├── prop/         # 道具
│               │   ├── factory/      # 道具与敌机工厂
│               │   ├── strategy/     # 射击策略
│               │   ├── observer/     # 炸弹与冰冻的观察者通知
│               │   ├── effects/      # 冰冻蒙板、爆炸动画与画面抖动
│               │   ├── config/       # 固定预设、自定义配置、校验与存储
│               │   ├── dao/          # 成绩对象、DAO 接口、文件实现与排行业务
│               │   ├── ui/           # 首页、设置、排行榜及内部对话层
│               │   └── audio/        # JavaFX 音乐与音效管理
│               ├── main/resources/  # 图片与音频资源
│               └── test/java/       # JUnit 测试
└── JavaSwing/AircraftWar-base/  # Swing 基础版本
```

## 启动方法

### 环境准备

安装完整的 JDK，建议安装 JDK 21，并确认命令行可找到 Java：

```bash
java -version
javac -version
```

Gradle 通过 toolchain 查找 JDK 21；若本机没有，已配置的 Foojay resolver 会尝试自动下载。首次构建还需下载 Gradle、JavaFX 和测试依赖，因此需要网络，耗时可能较长。安装了其他版本的 JDK，也不代表本机已经具备工程指定的 JDK 21 toolchain。

无需另外安装全局 Gradle，也无需手动把 JavaFX SDK 加入 classpath。

### macOS / Linux

从本 README 所在目录进入 Gradle 工程：

```bash
cd AircraftWar
bash ./gradlew :app:run
```

若希望直接执行 Wrapper，可先赋予执行权限：

```bash
chmod +x gradlew
./gradlew :app:run
```

### Windows PowerShell

```powershell
cd AircraftWar
.\gradlew.bat :app:run
```

入口类为 `edu.hitsz.application.Main`（Swing 主窗口入口）。在 VS Code 中可通过 Gradle 扩展执行 `app` 的 `run` 任务，或直接使用终端中的上述命令。应使用 Gradle 启动整个工程；Code Runner 生成的 `javac Main.java && java Main` 单文件命令无法正确处理 JavaFX 依赖、包结构和资源。

### 操作方式与当前行为

- 启动后进入动态首页，点击“开始游戏”选择简单、普通、困难或自定义模式；同一窗口切换页面。
- 首页右上角齿轮打开全局设置，只管理音乐、音效音量与静音；保存后生效，本次运行跨局保留。
- 自定义按钮右侧齿轮打开独立玩法设置；标准三难度使用固定预设，不受自定义修改影响。
- 移动或拖动鼠标控制英雄机，英雄机自动射击。
- 击毁敌机获得分数，部分敌机掉落道具；达到分数阈值后生成 Boss。
- 加血道具恢复生命值；普通火力道具切换散射，持续 10 秒，重复拾取刷新时限。超级火力道具立即发射一圈 20 发子弹，随后继续原来的直射或散射，保留散射剩余时间。
- 炸弹和冰冻通过观察者通知当前敌机及敌弹。炸弹击毁只计分、不掉落道具；冻结与减速会按对象类型到期恢复。
- 冰冻时显示蓝色蒙板，受影响对象覆盖透明冰霜；冰霜随各自的冻结或减速状态消失，普通敌机的永久冻结会持续显示。
- 炸弹触发冲击波、短暂画面抖动和爆炸音效；所有类型敌机在屏幕内被击毁或碰撞坠毁时显示爆炸动画，正常飞出屏幕时直接清理。
- 背景音乐循环播放；Boss 出场切换专属音乐，击毁后恢复普通音乐；命中、拾取和爆炸触发对应音效。英雄机坠毁时停止背景音乐并播放结束音效，返回菜单或关闭窗口时释放本局音频。
- 游戏结束时保留静止的最后画面，内部姓名输入层先覆盖该画面；保存成功或取消后才进入当前难度排行榜，页面与首页同为 512×768。可删除选中记录或返回菜单开始下一局。
- 自定义模式结束后只展示本局结果，可重玩或返回选择，不询问姓名、不保存或访问排行榜。
- 按 Esc 或点击右上角暂停按钮暂停；切换窗口会自动暂停，回来后按 Esc 或点击“继续战斗”恢复。散射、冻结、Boss 蓄力、难度递进及音乐均同步暂停。
- HUD 展示生命条、散射倒计时及受冻结影响的敌机数量；Boss 入场有预警与血条，半血蓄力后交替散射和环射。命中时有闪白与火花，受伤时边缘闪红，回血显示实际恢复值。
- 姓名输入、删除确认及错误提示均由主窗口内部的遮罩层显示，不打开额外窗口；Enter 确认、Esc 取消，姓名无效时保留输入供修正。

## 编译与测试

以下命令均在 `AircraftWar` 目录执行；Windows 将 `bash ./gradlew` 替换为 `.\gradlew.bat`。

```bash
# 仅编译 Java 源码
bash ./gradlew :app:compileJava

# 编译并处理资源
bash ./gradlew :app:classes

# 执行单元测试
bash ./gradlew :app:test

# 强制重新运行测试，避免直接复用上次结果
bash ./gradlew :app:test --rerun-tasks

# 完整构建，包含测试
bash ./gradlew :app:build
```

测试报告：[`app/build/reports/tests/test/index.html`](AircraftWar/app/build/reports/tests/test/index.html)，执行测试后生成。

依赖已下载齐全时，可在命令末尾添加 `--offline`。首次运行或缺少依赖时，应允许 Gradle 联网下载。

## 成绩存储与难度

`ScoreRecord` 保存玩家名、分数和记录时间；`ScoreDao` 声明查询、保存和删除操作；`FileScoreDao` 负责文件读写。文件路径由 `Main` 根据 `Difficulty` 决定，`Game` 在结束时回调通知主窗口，由排行榜界面通过 `RankingService` 和 DAO 保存、排序、删除记录。

| 难度       | 对应成绩文件          |
| ---------- | --------------------- |
| `EASY`   | `scores/easy.tsv`   |
| `NORMAL` | `scores/normal.tsv` |
| `HARD`   | `scores/hard.tsv`   |

难度菜单使用 `Difficulty` 枚举，同时决定背景地图、成绩文件和具体游戏子类。`EasyGame` 参数固定且无 Boss，`NormalGame`、`HardGame` 每 20 秒提升阶段参数，控制台会输出变化。递进影响后续新生成敌机，不会使已受伤敌机回血。

路径相对于程序工作目录。使用上述 `:app:run` 命令时，成绩写入 `AircraftWar/app/scores/`，文件和目录会在首次保存时创建。每行一条记录，三列用制表符分隔：

```text
Player	120	2026-10-27T14:30:00
```

排行榜按得分降序排列，同分时按记录时间升序排列。名次根据排序结果生成，不写入成绩文件。

## 配置与自定义模式

玩法默认值统一位于 [`game-defaults.properties`](AircraftWar/app/src/main/resources/config/game-defaults.properties)。其中包含三个标准模式的固定预设和自定义初值；用户自定义覆盖保存在工作目录的 `config/custom.properties`。通过 `:app:run` 启动时，该文件位于 `AircraftWar/app/config/custom.properties`，首次保存时创建，仅写与默认值不同的字段。

自定义设置共 **57 项**，按英雄机、敌机与子弹、道具与战斗、自定义难度分组。可调整生命值、子弹威力/速度/数量、散射间距、道具效果时长、掉落率、击毁得分、生成与射击周期、Boss 及阶段递进等参数。界面显示各字段范围；概率和、上下限关系与有限数值均经过校验，保存失败时保留草稿和原配置。

自定义普通敌机的纵向速度为 `min(速度上限, base + score × rate)`，包含场上仍存活的敌机，Boss 使用独立水平速度。速度单位是像素/逻辑步，每步 40 ms；支持小数并累计不足一像素的位移。默认 `base=6`、`rate=0.02`，得分 100 时速度为 8。冻结或减速仍在基础速度上生效，速度曲线不会解除冰冻。

`ConfigKey` 只描述字段与编辑范围，设置页据此构建控件；`GameConfig` 是经过校验的不可变快照；`GameConfigStore` 负责固定预设、自定义文件和当前对局快照。只有原子保存成功后才发布新配置；一局中的设置保持不变，下一局自定义模式才读取新值。标准难度始终读取内置预设，自定义模式禁止获取排行榜文件。

## 各次实验开发记录

### 实验一：系统分析及单例模式

- 梳理飞行物继承结构：`AbstractFlyingObject` 下分为飞机、子弹和道具；飞机公共行为由 `AbstractAircraft` 提供。
- 使用 PlantUML 绘制核心实体类图，学习可见性、继承等 UML 表示法。
- 将英雄机 `HeroAircraft` 改为单例，通过私有构造函数和 `getInstance()` 获取唯一实例。
- 完善精英敌机、道具及图片映射，接入道具移动、绘制、碰撞和失效清理流程，并实现加血道具。
- 在迭代中补充强化精英机、王牌敌机和 Boss 等实体，为后续工厂与策略改造提供对象。

对应类图：[核心继承关系](AircraftWar/uml/Inheritence.puml)、[英雄机单例](AircraftWar/uml/singleton.puml)。继承类图已随后续重构更新。

### 实验二：简单工厂及工厂方法模式

- 使用 `PropType` 枚举描述道具类型，由简单工厂 `PropFactory.create()` 统一创建道具。
- 定义 `EnemyFactory` 接口，为普通、精英、强化精英、王牌和 Boss 敌机提供具体工厂。
- 将 `Game` 中的敌机、道具创建逻辑改为调用工厂，集中管理对象构造。
- 接入各类敌机生成、Boss 触发和道具掉落流程。

对应类图：[道具简单工厂](AircraftWar/uml/SimpleFactory.puml)、[敌机工厂方法](AircraftWar/uml/EnemyFactoryMethod.puml)。

### 实验三：策略模式

- 定义 `ShootStrategy` 接口，在 `AbstractAircraft` 中持有策略，并由 `shoot()` 委托策略生成子弹。
- 提供不射击、敌机单发、双发、散射、环射，以及英雄机直射、散射、环射策略。
- 将飞机中的弹道算法抽取到策略类，支持运行时替换射击方式。
- 火力道具调用英雄机方法切换策略；限时恢复直射的逻辑留待实验五完成。

对应类图：[射击策略](AircraftWar/uml/Strategy.puml)。

### 实验四：DAO 模式和单元测试

- 新增成绩对象 `ScoreRecord`、数据访问接口 `ScoreDao` 和文件实现 `FileScoreDao`。
- 实现成绩追加保存、读取和删除；文件不存在时查询返回空列表，记录格式错误时报告文件和行号。
- 游戏结束时保存一次成绩，使用默认玩家名，在控制台输出名次、玩家名、得分和时间。
- 新增 `Difficulty`，将三种难度映射到独立成绩文件。
- 编写 `FileScoreDaoTest`，验证文件保存、读取、删除及难度数据隔离。
- 编写 `HeroAircraftTest`，验证单例、父类坐标设置、扣血与英雄机回血行为，并恢复测试修改的单例状态。

对应类图：[排行榜 DAO](AircraftWar/uml/ScoreDao.puml)。

当前状态：代码与自动化测试已完成，最近一次强制运行测试通过。2026-10-27 已实际结束一局游戏，终端正常输出包含玩家名、70 分成绩及时间的排行榜，游戏结束流程手动验收通过。

### 实验五：Swing 和多线程

- 使用一个 Swing `JFrame` 和 `CardLayout`，在难度菜单与游戏页面之间切换。
- 将原选择窗口重构为 `DifficultySelectionPanel`，通过 `Consumer<Difficulty>` 回调选择结果；现有 JavaFX 游戏通过 `JFXPanel` 嵌入主窗口。
- 按难度使用 `bg.jpg`、`bg2.jpg`、`bg3.jpg` 三张地图，保留各难度独立的成绩文件。
- 游戏结束后由 `RankingPanel` 在主窗口内展示排行榜，支持保存、取消、删除选中成绩、刷新名次和返回菜单；`DialogHost` 统一处理内部姓名输入、确认与提示。姓名限制为 1～30 个字符，禁止制表符与换行；每局最多保存一次。
- 将排序、姓名校验和保存、删除规则封装到 `RankingService`，界面不直接处理文件格式。
- 使用 `Runnable` 和后台线程等待 10 秒，之后通过 `Platform.runLater()` 恢复直射。`FireEffectTimer` 使用版本号和中断处理重复拾取，旧回调不会覆盖新效果，结束或重开时取消计时。
- 音频统一使用 JavaFX 官方媒体引擎：`AudioManager` 复用 `AudioClip` 播放短音效，通过 `MediaPlayer` 管理普通/Boss 背景音乐的循环和暂停；解码、混音和播放线程由 JavaFX 管理。原实验的 `MusicThread` 及后续自写 PCM 混音实现已移除。
- 接入普通/Boss 背景音乐切换、命中、拾取与爆炸音效；退出时停止游戏、火力计时并释放媒体播放器。
- 为英雄机增加重开时的状态重置，继续保持同一个单例实例。

自动化测试覆盖 DAO 往返与难度隔离、排行排序和删除、姓名校验、火力计时失效及英雄机重开。实际音量与声卡输出需在本机运行确认。

手动验收步骤：

1. 启动后分别选择三种难度，确认游戏在原主窗口显示且地图不同。
2. 拾取火力道具后等待 10 秒，确认恢复直射；连续拾取后从最后一次重新计时。
3. 检查普通背景、Boss 背景切换与结束停播，以及命中、拾取等事件音效。
4. 结束后输入姓名保存，检查名次；取消输入应不新增记录，空白姓名应提示错误。
5. 选择一行删除并确认，检查表格刷新；重启程序后确认文件记录一致。
6. 返回难度选择并开始下一局，确认英雄机生命值和弹道重置；关闭主窗口后程序退出。

### 实验六：观察者模式与模板方法模式

- `PropEffectObserver` 定义 `onBomb()` 与 `onFreeze()`；五类敌机和敌弹实现对应响应。`AbstractEffectProp` 管理观察者列表，`BombProp` 与 `FreezeProp` 负责发出通知。
- `PropEffectAction` 在拾取时注册当前存活对象，通知后移除。仅统计本次新击毁的敌机，沿用每架 10 分；不会调用子弹击毁时的道具掉落流程。
- `MovementEffect` 通过单调时钟保存效果时限和移动倍率，原始速度不被覆盖；重复冰冻刷新时限，减速保留不足一个像素的位移。
- `Game` 改为抽象父类，`action()` 为 `final` 模板方法，统一初始化、公共游戏循环和背景音乐。三个子类实现 `parametersAtLevel()` 和 `bossHpForSpawn()`，参数由不可变的 `GameParameters` 表示。
- 英雄机与敌机使用独立射击计数器；普通、困难模式每 20 秒递进，生成、碰撞、道具和渲染流程保持共用。
- 普通、困难模式均可按分数阈值多次生成 Boss，最多一架同时存在；简单模式禁用 Boss。

道具规则：

| 对象     | 炸弹               | 冰冻                        |
| -------- | ------------------ | --------------------------- |
| 普通敌机 | 坠毁               | 永久静止                    |
| 精英敌机 | 坠毁               | 静止 4 秒                   |
| 精锐敌机 | 坠毁               | 静止 3 秒                   |
| 王牌敌机 | 扣 60 血，可被击毁 | 移动速度降至 50%，持续 5 秒 |
| Boss     | 不受影响           | 不受影响                    |
| 敌弹     | 清除               | 静止 5 秒                   |

冰冻约束移动，不额外改变射击策略；仍可与英雄机发生碰撞。任务书未指定王牌炸弹伤害与减速比例，上表为本项目选定值。

难度初始参数（周期单位为毫秒）：

| 模式 | 敌机上限 | 精英/精锐/王牌概率 | 英雄射击 | 敌机射击 | 敌机生成 | 速度 | 血量倍率 | Boss                           |
| ---- | -------- | ------------------ | -------- | -------- | -------- | ---- | -------- | ------------------------------ |
| 简单 | 3        | 15% / 5% / 2%      | 400      | 1200     | 1200     | 6    | 0.7      | 无                             |
| 普通 | 5        | 20% / 10% / 5%     | 480      | 960      | 960      | 8    | 1.0      | 每 100 分，300 血              |
| 困难 | 7        | 25% / 18% / 12%    | 400      | 720      | 720      | 10   | 1.4      | 每 100 分，450 血起，每次 +150 |

普通模式每阶段缩短生成周期 80 ms、速度 +1、血量倍率 +0.15。困难模式每阶段缩短生成周期与敌机射击周期各 40 ms、英雄射击周期增加 40 ms、速度 +2、血量倍率 +0.2。生成和射击周期、速度设有上下限，避免长时间运行后出现非正周期。详细边界见三个子类的代码。

对应类图：[观察者模式](AircraftWar/uml/Observer.puml)、[模板方法模式](AircraftWar/uml/Template.puml)。

期末提交：完整代码 ZIP、在线报告与类图、少于 2 分钟的展示视频。报告草稿与代码包放在 `submission/`，录屏由本人完成后上传。

### 2026-11-04：本地体验优化，待核验

- 环射改为拾取当帧的一次性齐射，不覆盖原射击策略，也不重置散射计时。
- 新增 `CombatEffects` 管理独立的动画状态，`CombatEffectsRenderer` 绘制冰冻蒙板、冰霜、冲击波和爆炸；抖动只影响游戏画面，HUD 和碰撞坐标保持稳定。
- 使用生成的透明冰霜、爆炸图片，以及程序合成的短促爆炸音效。素材来源和提示词见 [ASSETS.md](AircraftWar/app/src/main/resources/images/effects/ASSETS.md)。
- 集成测试覆盖立即环射、冻结状态、炸弹计分和全部敌机爆炸；可执行 `bash ./gradlew :app:effectsPreview` 重新生成仓库 `output/effects-preview.png` 静态预览。
- 本轮仅更新本地工程；已上传代码包和在线作业草稿暂不替换，等待本人核验。

### 2026-11-04：完整体验与自定义模式

- 新增动态天空首页，统一难度选择、设置与结算页的背景、按钮及字体层级；背景原图和提示词见 [UI 素材说明](AircraftWar/app/src/main/resources/images/ui/ASSETS.md)。
- 增加 Boss 血条、入场预警、二阶段蓄力与交替弹道；增加生命条、火力倒计时、命中/受伤与回血反馈。
- 支持手动暂停、窗口失焦暂停及音量设置；后台火力计时与临时移动状态均支持保留剩余时间。
- 全局音频设置和自定义数值设置分别路由；新增 `CustomGame` 复用模板主流程，自定义结果不进入 DAO 排行榜。
- 默认玩法数值集中到配置文件，设置页按元数据构建；不增加额外配置框架。标准模式固定预设与自定义覆盖分别管理。
- 可在 Gradle 工程运行 `bash ./gradlew :app:experiencePreview`，生成仓库 `output/experience/` 下的实际组件离屏预览。运行预览不会写入成绩或自定义配置。
- 排行榜与自定义结果改为主窗口内的等尺寸页面；内部对话层拦截背景操作，取消或切页时关闭，确认后才执行保存或删除。预览中的 `results-overview.png` 展示排行榜、姓名输入和删除确认。
- 坠毁时立即停止逻辑、动画及背景音乐，播放结束音效并保留最终游戏画面；姓名弹窗关闭后才进入排行榜，校验/写入失败时仍保留输入。`results-flow.png` 展示这一流程。
- 音频最终切换为 JavaFX `AudioClip` + `MediaPlayer`，短音效在首页阶段预加载，背景播放器每局复用；暂停时保留背景位置并停止短音效，坠毁时允许结束音效自然播放。删除 Java Sound 设备管理、自写线程、PCM 缓存及混音代码。
- 固定步循环每帧最多补算 5 步，避免一次设备卡顿引发长时间追帧。旧音频链路的卡死及 PCM 测量保存在 `output/diagnostics/`，不代表当前官方引擎的延迟。当前实现与检查记录见 `output/diagnostics/javafx-audio.md`。

## UML 与课程资料

- [实验指导书](docs/instructions/)
- [课程课件](docs/ppt/)
- [PlantUML 源文件](AircraftWar/uml/)
- [已导出的类图](AircraftWar/uml/export/)
- [Gradle 命令参考](docs/gradle_base.md)

使用 VS Code PlantUML 扩展打开 `.puml` 文件可预览、导出类图。源文件使用 `!pragma layout smetana` 布局。

## 常见启动问题

- **macOS 内建屏移动鼠标时卡顿**：已在启动入口默认使用 Swing 兼容渲染，绕过双屏混合刷新率下的 Metal 提交停顿；完全退出后重新运行即可生效。诊断记录见 [display-stutter.md](output/diagnostics/display-stutter.md)。

- **`permission denied: ./gradlew`**：使用 `bash ./gradlew :app:run`，或执行 `chmod +x gradlew` 后再运行。
- **`程序包 javafx...不存在`**：通常是使用了单文件编译；改用本工程的 Gradle `:app:run` 任务。
- **找不到 JDK 21**：安装 JDK 21，或允许 toolchain 自动下载；可用 `bash ./gradlew -q javaToolchains` 检查识别结果。
- **JDK 下载锁超时**：检查是否同时启动了多个构建或 IDE 导入任务，先结束重复任务，再重试。
- **`Resource not found`**：检查资源是否位于 `app/src/main/resources/`，以及代码中的路径、目录和文件名大小写是否一致。
