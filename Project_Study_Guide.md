# ArcheryLog 项目学习与演示指南 (ArcheryLog Project Study & Demo Guide)

---

## 1. 项目简介 (Project Overview)

**ArcheryLog** 是一款专为射箭运动员和爱好者设计的专业训练记录与分析工具。它将传统的纸笔记录数字化，利用现代移动开发技术解决了训练数据难记录、难统计、难分析的痛点。

### 核心亮点：
- **交互式记录**：通过可视化的电子靶面，实现“所见即所得”的落点记录。
- **专业化统计**：支持按距离、场地类型进行筛选，自动生成训练趋势图。
- **全本地化存储**：基于 Room 数据库，完全支持离线操作，数据安全稳定。
- **跨平台审美**：采用现代化的深色模式设计，流畅的 Compose 动画效果。

---

## 2. 技术架构解读 (Technical Architecture)

该项目采用了 Android 最前沿的开发模式，是目前行业内“最佳实践”的体现：

### 核心技术栈 (Tech Stack)
- **语言**：Kotlin (简洁、安全、支持协程)
- **UI 框架**：Jetpack Compose (声明式 UI，现代且高效)
- **异步处理**：Kotlin Coroutines & Flow (响应式编程，解决并发问题)
- **本地数据库**：Room Database (对 SQLite 的完美抽象，支持 SQL 查询)
- **架构模式**：MVVM (Model-View-ViewModel) + Clean Architecture
- **依赖注入/生命周期控制**：Android ViewModel & Lifecycle

### 架构优势说明：
- **解耦性强**：View 只负责展示，逻辑由 ViewModel 处理，数据源由 Repository 统一管理。
- **性能优异**：UI 更新完全由数据流 (Flow)驱动，避免了传统的 UI 刷新的复杂操作。
- **可复用性**：自定义组件（如 TargetFace）可以在多个屏幕中无缝复用。

---

## 3. 面试演示流程 (Interview Demo Flow)

建议按照以下顺序从“面”到“点”层层递进：

### 第一阶段：项目起始与基本盘 (Foundation)
1. **启动演示**：打开应用，演示 **Login/Signup** 界面。讲解点：虽然是移动端本地应用，但实现了完善的用户账户系统。
2. **记录中心**：主页展示以往训练列表。讲解点：通过 `Flow` 动态订阅数据库，实现了 UI 对底层状态的响应。

### 第二阶段：核心难点攻克 (The Wow Factor)
1. **交互靶面 (TargetFace)**：点击进入一轮训练。演示在靶面上随意点击记录坐标。
2. **讲解点**（重点）：靶面是使用 `Canvas API` **原生手绘**的，而非贴图。通过向量计算，精准识别用户点击在几环。展示 **Undo/Redo** 的逻辑严密性。

### 第三阶段：深度分析与扩展 (Advanced Features)
1. **数据分析 (Statistics)**：切换到统计页面展示曲线图。
2. **讲解点**：强调图表的平滑曲线是通过三次贝塞尔曲线算法 (`cubicTo`) 自己绘制的，这能够体现深入的底层开发能力。

### 第四阶段：商业化落地 (Polish & Scalability)
1. **多语言 (L10n)**：在设置中切换语言。讲解点：项目不仅功能齐备，还具备了走向国际市场的可扩展性基础。

---

## 4. 八股文关联点 (Key Interview Q&A)

- **为什么选 Compose?** 声明式 UI，状态驱动，减少了代码碎片化，能够大幅缩短 UI 开发周期。
- **如何处理离线数据?** 使用 Room 提供的 Room Database，它基于 SQLite 提供了类型安全且同步性能极高的封装。
- **如何处理异步任务?** 全部使用 Coroutines。比如数据库读写都在 `Dispatchers.IO` 下运行，界面始终保持 60 帧以上的流畅度。

---

## 5. 如何进一步学习与下载

1. **导出为 PDF**：您可以将此文档复制到 VS Code 或 Typora 等编辑器，选择 `Export to PDF`。
2. **源码对应关系**：
    - UI 逻辑及状态：`com.example.archerylog.ui.ArcheryViewModel`
    - 自定义绘图逻辑：`com.example.archerylog.ui.screens.TargetFace.kt`
    - 数据库交互：`com.example.archerylog.data.*`
