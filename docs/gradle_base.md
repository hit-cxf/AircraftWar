# Gradle 基本用法

项目使用 *Gradle Wrapper*（`gradlew` / `gradlew.bat`）+ *Kotlin DSL*（`build.gradle.kts`）进行构建，项目为多模块结构：根工程 `AircraftWar` 下包含子模块 `app`，支持多平台（Linux/MacOS/Windows）。

如果使用ide，会自动配置，点点点就行，但是本质还是基于下面这些命令👿。

使用`vscode` 或者 `neovim` 可以纯命令实现管理项目，很清爽，几乎只需要你安装jdk，gradlew 默认读取的是你 JAVA_HOME/PATH 中的 jdk😋。

插件方面，`vscode` 和 `neovim` 也很丰富，可以支持plantuml等😋。

推荐永远用 Wrapper `./gradlew`，不要依赖本机安装的 `gradle`。

## 1. 快速开始

在仓库根目录执行：

### Linux / macOS

```bash
./gradlew :app:run
```

### Windows (PowerShell / CMD)

```bat
gradlew.bat :app:run
```

windows 下脚本功能与unix下脚本功能一致，下全部使用unix系统为例，windows 替换其为 `.bat` 即可。


## 2. 任务（Task）是什么？怎么查看？

Gradle 的基本单位是 **Task**（任务），比如 `build`、`test`、`run`。你可以列出可用任务：

```bash
./gradlew tasks
```

只看 `app` 模块的任务：

```bash
./gradlew :app:tasks
```

查看某个任务做什么：

```bash
./gradlew help --task :app:run
./gradlew help --task :app:test
```

## 3. 项目常用命令清单

```bash
./gradlew :app:run # 运行游戏
./gradlew :app:build # 编译 + 打包（会先跑测试，可能较慢）
./gradlew :app:classes # 只编译（不跑测试）
./gradlew :app:test # 运行单元测试（JUnit 5）
./gradlew clean # 清理构建产物
```

## 4. 代码覆盖率报告

在 app/build.gradle.kts 文件的 plugins中引入 jacoco

```kotlin
plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    application

    // Code coverage reports
    jacoco

    // JavaFX support
    id("org.openjfx.javafxplugin") version "0.1.0"
}
```

然后再task中注册，写在文件最后：

```kotlin
tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        csv.required.set(false)
        html.required.set(true)
    }
}
```

此时可以单独执行：

```bash
./gradlew :app:jacocoTestReport
```

可以考虑在test后附加自动生成报告：

```kotlin
tasks.named<Test>("test") {
    // Use JUnit Platform for unit tests.
    useJUnitPlatform()

    // Generate coverage report after running tests
    finalizedBy(tasks.jacocoTestReport)
}
```

此时在 `test` 后会自动触发覆盖率任务

覆盖率 HTML 报告位置（生成后在浏览器打开）：

- `app/build/reports/jacoco/test/html/index.html`

## 5. 多模块命令写法

本项目包含子模块 `app`，所以常用写法是：

- `:app:run`：只运行 `app` 模块的 `run`
- `:app:test`：只跑 `app` 模块的测试

如果你直接写 `./gradlew test`，在多模块情况下可能会跑“所有模块的 test”（以后模块多了会更慢）；推荐显式指定 `:app:`。

## 6. Java / JavaFX 相关说明

### Java 版本

`app` 模块使用 *Java Toolchain = 21*。Gradle 可能会自动下载/选择合适的 JDK（取决于你的网络与本机环境）。

### JavaFX

`app` 通过 `org.openjfx.javafxplugin` 配置 JavaFX（已在构建脚本中声明 `javafx.controls` / `graphics` / `media`）。一般只要使用 `:app:run` 就能正确带上模块与依赖。

## 7. 常见问题排查

### `Permission denied: ./gradlew`

```bash
chmod +x gradlew
```

### 构建失败但信息太少

加上更详细的日志：

```bash
./gradlew :app:run --stacktrace
./gradlew :app:test --info
```

### 下载依赖/JDK 很慢或失败

可能是网络问题或需要配置代理。最简单的处理：

- 换网络/开代理后重试
- 先执行一次 `./gradlew :app:tasks` 触发初始化

### “改了资源/图片/音频，运行时找不到”

项目把 `app/src` 作为资源目录的一部分（为了兼容历史布局），资源打包由 `sourceSets { main { resources { ... }}}` 控制。

如果你新增资源目录/文件后仍不生效：

```bash
./gradlew clean :app:run
```
