# 第七章：从 Vibe Coding 到 Spec-Driven Development——OpenSpec 实战

[← 上一章：总结与展望](./06-总结与展望.md) | [返回目录](./00-目录.md)

---

## 7. 从 Vibe Coding 到 Spec-Driven Development

前六章记录了用 Claude Code 做 Vibe Coding 的完整旅程——从零搭建全栈应用，到积累了 4 万行代码。Vibe Coding 带来了惊人的开发速度，但随着项目规模的增长，也暴露出一个结构性问题：

> **AI 写代码很快，但"快速跑偏"同样是 AI 的强项。**

当你用一句话描述需求时，AI 可能理解了 70%，然后以极快的速度向那个方向狂奔 2 小时，最后你发现核心逻辑不对，不得不推倒重来。

这不只是理论。在引入 SDD 之前，我在 Finance 项目中遇到的真实痛点是：

- **流程不规范**：需要主动提醒 AI 先整理需求再动手，否则直接跳到代码
- **设计文档缺失**：实现结束后才意识到架构有问题，回头改代价很高
- **代码质量不稳定**：同样的需求，不同 session 生成的代码质量差异很大
- **测试经常被遗漏**：Vibe Coding 倾向于"先跑起来再说"，测试变成了可选项
- **调试耗时**：因为没有清晰的任务边界，bug 定位困难，和 AI 来回沟通效率低

这一章记录的是一次方法论升级的实验：在 Finance 项目中引入 **Spec-Driven Development（规格驱动开发）**，使用 **OpenSpec** 工具完成三个新功能的开发，并与之前的 Vibe Coding 对比实际效果。

---

### 7.1 什么是 Spec-Driven Development

#### 核心思想

Spec-Driven Development（SDD）的核心理念是：**先达成共识，再写代码**。

在传统 Vibe Coding 中，流程是：

```
想法 → 一句 Prompt → AI 开始写代码 → 边做边改
```

而 SDD 的流程是：

```
想法 → 结构化提案（Proposal）→ 任务清单（Tasks）→ AI 按清单实现 → 归档规范（Archive）
```

区别不在于工具，而在于**决策发生的时机**。SDD 强制把所有重要决策（功能边界、技术方案、验收标准）放到写代码之前，用文档固化共识，让 AI 在有约束的空间内自主执行。

#### OpenSpec 是什么

OpenSpec 是一个轻量级的 AI 工作流 CLI 工具，专为 SDD 设计。它的核心是一套标准化的项目结构和三个命令：

```
openspec/
├── specs/           # 当前全量规范（事实来源）
│   └── <capability>/
│       └── spec.md
└── changes/         # 进行中的变更
    ├── <change-id>/
    │   ├── proposal.md   # 为什么做、做什么、影响范围
    │   ├── design.md     # 技术方案
    │   ├── tasks.md      # 分解的实现清单
    │   └── specs/        # 仅包含 delta（新增/修改）的规范片段
    └── archive/          # 已归档的历史变更
```

**安装与初始化**：

```bash
npm install -g @fission-ai/openspec@latest
openspec --version

cd your-project
openspec init
```

---

### 7.2 OpenSpec 的三阶段工作流

![OpenSpec 工作流](./images/openspec_flow.svg)

*图：OpenSpec 三阶段工作流 — Propose → Apply → Archive*

OpenSpec 在 Claude Code 中以三个 Skills 的形式使用：

#### 阶段一：/opsx:propose（提案）

输入：一句话描述或草稿需求
输出：`proposal.md`（Why/What/Scope）、`design.md`（技术方案）、`tasks.md`（分解任务清单）、`specs/`（delta 规范）

这一阶段的价值是把模糊的想法转化为**可执行的合同**。AI 在这里扮演的是架构师和产品经理的角色，你扮演的是审查者。

**关键动作**：审查 tasks.md，检查任务分解是否合理，验收标准是否清晰。**不满意就在 propose 阶段修改，比在实现中途推倒重来便宜得多。**

#### 阶段二：/opsx:apply（实施）

输入：审查通过的 tasks.md
输出：按清单逐项实现的代码、测试、配置变更

AI 按 `tasks.md` 的顺序逐项执行，每完成一项标记 `[x]`。你可以随时暂停、检查进度、修正方向，然后继续。

**关键动作**：不要在实施过程中频繁插入新需求。如果需求变了，先更新 proposal，再继续 apply——这是 SDD 的纪律。

#### 阶段三：/opsx:archive（归档）

输入：完成的变更目录
输出：delta spec 合并回 `openspec/specs/`，change 目录移入 `archive/`

归档使规范库（`specs/`）始终代表"当前系统的真实状态"，成为下一个变更的起点。

---

### 7.3 config.yaml：OpenSpec 的"项目记忆"

做完第一个 feature 之后，我意识到漏掉了一个重要的准备工作：配置 `openspec/config.yaml`。理想情况下应该在第一个 feature 开始前完成，但亡羊补牢同样有效——从第二个 feature 开始，它就开始发挥作用。

这个文件是 OpenSpec 的 `CLAUDE.md`——它告诉 AI 这个项目的技术栈、编码规范、约定、以及历史上踩过的坑。

**初始化 config.yaml**：

```
请帮我更新 openspec 目录下的 config.yaml，参考根目录的 CLAUDE.md 获取技术栈、约定和代码风格，参考 README.md 获取领域知识，按 config.yaml 中的示例格式填写。
```

AI 生成后，我又做了两个专项补充：

**补充测试策略**：

```
请在 config.yaml 中补充测试策略，这是一个全栈应用，测试需要包含后端 API 测试和前端 UI 测试。
```

**补充历史错误（防止重犯）**：

```
在用 OpenSpec 开发 runway 功能时犯了一个错误——没有考虑每个账户的货币，直接简单相加；后来修复时又引入了另一个问题，每次查汇率都走数据库，实际上 Controller 层有缓存。请把这两个错误写入 config.yaml，避免后续变更重蹈覆辙。
```

**关键洞察**：
> **config.yaml 不是一次性配置，而是随着项目演进不断积累的"防错手册"。** 每次犯了新错误，就补充进去，AI 在下一个变更中会主动避开。

---

### 7.4 三个 Feature 实战

#### Feature 1：Runway Analysis（跑道分析）

**需求**：基于当前流动资产和未来月度支出，计算家庭"资金还能撑多久"。

**Propose**：

```
I want to add a new function, runway analysis. I have one example at C:\...\runway-calculation,
please use the same structure. You can get future monthly expenses from the system,
and liquid assets from the system. Please create a proposal.
```

AI 生成了包含 27 个任务的清单，覆盖后端 API、前端页面、测试，其中只有一个后端集成测试是手动任务（用 Swagger UI 验证）。

**Apply 过程中发现的问题**：

*问题一（致命错误）：货币未对齐*

AI 实现的初版直接将所有账户金额相加，忽略了多货币问题——一个 USD 账户和一个 CNY 账户直接求和，结果完全错误。

究其原因：这是一个业务理解问题，而非技术问题。AI 写代码很快，但不会主动"想到"汇率转换的必要性。

修复后发现了第二个问题：

*问题二（性能错误）：每次查汇率都访问数据库*

修复货币问题时，AI 对每一条记录都单独查询数据库汇率，导致生成报告极慢。实际上系统已有带缓存的 `ExchangeRateService`，只需调用一次即可。

两个错误都在提示 AI 后快速修复，但更重要的是：**把这两个错误写入了 config.yaml**，保护后续所有变更不再重犯。

**Runway Analysis 完成后，需求又扩展了**：增加了"排除部分流动资产"和"调整特定支出项"的功能——这在 SDD 中很正常，通过更新 proposal 然后继续 apply 完成。

**数据**：
- 代码新增：约 1,900 行，18 个文件
- 任务完成：26/27（1 个手动）
- 开发时间：约 **2 小时**

---

#### Feature 2：Runway Report 持久化与 PDF 导出

**需求**：每次打开 Runway 页面都会重新计算，无法保存某个时间点的快照。希望能保存并回顾历史报告。

**需求演变过程**（SDD 如何处理需求变更）：

这个 feature 经历了三次需求变更，是测试 SDD 弹性的好案例：

```
初版需求：导出 JSON 文件到本地
↓ 用户觉得 JSON 不友好
修改一：改为导出 PDF 报告
↓ 用户改变主意，不想只有导出本地文件功能
修改二：持久化到后端数据库，新增报告列表页面
```

第三次修改最关键——需求从"纯前端改动"变成了"前后端+新数据库表"的全栈功能。

**关键决策**：OpenSpec 检测到需求大幅变化，果断删除了已生成的 proposal 和 tasks，重新生成。

这是 SDD 的纪律所在：**不要在半成品的 proposal 上打补丁，需求大变就重新提案**。实践证明这个判断是对的——在之前 Vibe Coding 的经验里，在半成品需求上反复修改，反而让 AI 陷入混乱，最终质量更差。

重新 propose 后生成了 34 个任务，覆盖 11 个分类（后端实体/Repository/Service/Controller、前端组件、数据库迁移、后端测试、前端测试）。

**Apply 过程中的问题**：

- **API 路由错误**：Save Report 功能不工作，诊断发现是 Controller 的路由配置不对。同样写入了 config.yaml。
- **PDF 中文乱码**：AI 最初选用的 PDF 库不支持中文，切换了实现方案后解决。
- **Mock 测试失败**：新引入的 Mock 测试框架初始配置有误，根据错误信息修复后通过。

**Archive 后的补充工作**：

测试覆盖仍然偏弱（前端 UI 测试只有 manual），专门做了一次 Vitest + Vue Test Utils 的环境搭建：

```bash
# 安装组件测试框架
npm install -D vitest @vue/test-utils @vitejs/plugin-vue jsdom
# 更新 vite.config.js 配置测试环境
```

**数据**：
- 代码新增：约 1,800 行，25 个文件
- 任务完成：33/34
- 开发时间：约 **38 分钟**（从第二次提案到归档）

---

#### Feature 3：房产投资计算器

**需求**：将一个 Excel 电子表格（"The Brutal Calculator"）转化为原生 Web 计算器，供湾区高净值家庭评估租房投资的税后真实回报。

**Propose**：

```
I added an Excel file under the requirement folder (The Brutal Calculator.xlsx).
Please read the sheet and convert it as a new feature: Property Investment Calculator.
I may add a new group (投资) in the sidebar.
```

读取 Excel 文件花了一些时间（需要安装 xlsx 解析工具），但 AI 完整解析了所有公式逻辑，生成了：
- 8 个任务组，22 个任务
- 覆盖：Vue 组件、公式工具类、路由注册、Sidebar 改动、Vue 组件测试

这是三个 feature 中**唯一纯前端**的变更——无后端改动，无数据库变更。

**Apply 过程中的问题**：

- **公式计算错误**：PMT（月供）和 CUMPRINC（本金还款）的公式实现有误，提示后修正。这类错误属于"AI 对金融公式的理解偏差"，不是代码能力问题。

Apply 完成后，还做了一些 UI 调整（从 2 列布局改为 3 列，调整标签显示等），均顺利完成。

![房产投资计算器截图](./images/property-calculator.png)

*图：房产投资计算器页面 — 左侧 13 个输入字段，右侧五组实时计算结果*

**数据**：
- 代码新增：约 2,400 行，22 个文件
- 任务完成：19/20（第 20 个为 optional 手动测试，已人工验证）
- 开发时间：约 **49 分钟**

---

### 7.5 三个 Feature 的横向对比

| | runway-analysis | runway-report | property-calculator |
|---|---|---|---|
| **代码新增** | ~1,900 行 | ~1,800 行 | ~2,400 行 |
| **任务数** | 27 | 34 | 20 |
| **复杂度** | 前后端，无新数据库表 | 前后端+新数据库表 | 纯前端 |
| **测试覆盖** | 手动后端测试 | 后端自动+手动前端测试 | 前后端自动测试 |
| **致命错误** | 货币未对齐、汇率性能问题 | API 路由错误、PDF 中文乱码 | 金融公式计算偏差 |
| **开发时间** | ~2h | ~38m | ~49m |

**为什么 Feature 2 和 3 比 Feature 1 快这么多？**

不是 AI 变聪明了，而是三个原因：

1. **config.yaml 积累了错误教训**：货币/汇率问题已在 Feature 1 之后写入配置，Feature 2 和 3 没有重犯。
2. **测试框架已搭建**：Feature 1 结束后建立了 Vitest 环境，后续 feature 直接复用。
3. **需求更清晰**：有了第一个 feature 的锻炼，后续 propose 阶段的需求描述更精准，AI 理解偏差更少。

---

### 7.6 SDD vs. Vibe Coding：什么时候用哪个

| 维度 | Vibe Coding | Spec-Driven Development |
|---|---|---|
| **需求清晰度** | 模糊 OK，边做边定 | 需要提前想清楚功能边界 |
| **功能复杂度** | 小功能（< 5 文件） | 中大型功能（跨层、多任务） |
| **跑偏风险** | 高（AI 快速跑偏） | 低（任务清单约束方向） |
| **灵活调整** | 高（随时改方向） | 需要更新 proposal 再继续 |
| **可回溯性** | 依赖 git history | proposal/tasks 留有完整记录 |
| **适合场景** | 原型验证、探索性功能 | 需要交付、有验收标准的功能 |

**实践建议**：

> 用 **Vibe Coding** 验证想法，用 **SDD** 交付功能。

具体判断标准：
- ✅ 修改涉及 3 个以上文件 → 用 SDD
- ✅ 需要前后端同时改动 → 用 SDD
- ✅ 包含数据库变更 → 用 SDD
- ✅ 有明确验收标准 → 用 SDD
- ⚡ 快速 UI 调整、小 bug 修复 → Vibe Coding 足够

---

### 7.7 OpenSpec vs. 其他 SDD 工具

目前主流的三个 AI 工作流工具定位各有不同：

| | **OpenSpec** | **SpecKit** | **Superpowers** |
|---|---|---|---|
| **定位** | 轻量级 CLI，聚焦变更管理 | 重量级规范框架，完整 SDD 体系 | Claude Code Skills 扩展库 |
| **规范方式** | delta spec（只写变化的部分） | 全量 spec（完整规范文档） | 技能化工作流 |
| **学习曲线** | 低（一天上手） | 高（需要理解规范体系） | 低（直接用 Skills） |
| **适合场景** | 中小项目，快速迭代 | 大型项目，需要严格规范管理 | 想增强 Claude Code 能力 |
| **归档机制** | 内置（`archive` 命令） | 内置 | 无 |

我在公司的项目中尝试过类似SpecKit的全规范框架的SDD，在这个项目中用了OpenSpec，而在另一个个人博客的项目中使用了Superpowers，目前我比较喜欢OpenSpec，也在探索
OpenSpec + Superpowers 的组合。

OpenSpec 负责变更管理和规范沉淀，Superpowers 提供日常工作流的能力增强（如 `brainstorming`、`verification-before-completion`）。两者不冲突，可以叠加使用。

---

### 7.8 引入 SDD 的经验总结

**三条核心建议**：

**1. config.yaml 是最重要的投资**

写代码之前，花 30 分钟把项目的技术栈、约定和历史错误整理进 config.yaml。这是对后续所有 feature 的一次性投资，收益随时间递增。

**2. 需求变了就重新 propose，不要在半成品上打补丁**

Feature 2 的三次需求变更证明了这一点。当变更幅度超过原 proposal 的 50% 时，重新 propose 反而更快。AI 在清晰的上下文里工作效率更高，在混乱的上下文里会产生奇怪的决策。

**3. 把每次犯的错误写回 config.yaml**

这是 SDD 与 Vibe Coding 最大的行为差异。Vibe Coding 的错误只在 git history 里留了痕迹，下次可能重犯。SDD 的错误会被提炼成结构化规则，成为项目的"防错基因"。

**量化收益**：

三个 feature 合计：
- 新增代码：约 **6,100 行**
- 完成任务：**78/81**
- 总开发时间：约 **3.5 小时**
- 平均每百行代码：约 **3.5 分钟**

其中 Feature 1（2 小时）包含了大量的工作流摸索和 config.yaml 建设成本。Feature 2 和 3（合计约 87 分钟，约 4,200 行代码）代表了 SDD 成熟后的实际速度。

---

[← 上一章：总结与展望](./06-总结与展望.md) | [返回目录](./00-目录.md)

---

---

## 参考资料

**OpenSpec**

- [OpenSpec 官网](https://github.com/Fission-AI/OpenSpec) 
- [OpenSpec 介绍](https://jimmysong.io/zh/book/ai-handbook/sdd/openspec/) — 完整的工作流说明和 config.yaml 配置参考

**Spec-Driven Development 深度阅读**
- [OpenSpec vs SpecKit 详细对比](https://juejin.cn/post/7605494530017165352) — 掘金文章，深入分析两个工具的设计哲学和适用场景
- [SpecKit vs OpenSpec 对比](https://intent-driven.dev/knowledge/spec-kit-vs-openspec/) — intent-driven.dev 的技术对比

**Superpowers**
- [Superpowers Claude Code Skills](https://github.com/anthropics/claude-code) — 可与 OpenSpec 组合使用的工作流增强技能库

**本系列其他章节**
- [第四章：软件开发方法论在 AI 时代的演进](./04-软件开发方法论演进.md) — Vibe Coding 的方法论背景
- [第六章：总结与展望](./06-总结与展望.md) — Finance 项目整体数据与经验总结

**Finance 项目**
- [GitHub 仓库](https://github.com/austinxyz/finance) — 本文所有实战案例的源码，含完整 CLAUDE.md、Skills 和 openspec 配置

---

**版本历史**：
- v1.0 (2026-03-06): 初稿完成，基于 Finance 项目三个 feature 的实战记录