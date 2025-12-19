# 第二章：Claude Code 概述

[← 上一章：引言](./01-引言.md) | [返回目录](./00-目录.md) | [下一章：项目开发全流程 →](./03-项目开发全流程实战.md)

---

## 2.1 Claude Code 的背景

Claude Code 是 Anthropic 于 2024 年推出的官方 CLI 工具，旨在将 Claude 的强大能力直接带入开发者的命令行环境。

**发展历程**：
- **2024 年初**：Anthropic 发布 Claude 3 系列模型（Opus、Sonnet、Haiku）
- **2024 年中**：推出 Claude Code CLI，支持基础代码生成和文件操作
- **2024 年下半年**：增加 Skills、Hooks、Planning Mode 等高级功能
- **2025 年**：成为主流 AI 编程工具之一，与 Cursor、Cline 形成竞争格局

**核心定位**：
> Claude Code 不是"代码补全工具"，而是"AI 软件工程师助手"。

与 GitHub Copilot 的本质区别：
- **Copilot**：在编辑器中提供实时代码建议（被动）
- **Claude Code**：接受高级任务描述，自主执行完整开发流程（主动）

**技术优势**：
1. **大上下文窗口**：Sonnet 4 支持 200K tokens，足以理解整个中小型项目
2. **工具调用能力**：可执行 bash 命令、读写文件、调用 API、管理 git
3. **可编程扩展**：通过 Skills 和 Hooks 深度定制工作流
4. **MCP 集成**：Model Context Protocol 支持连接外部数据源和工具

---

## 2.2 核心概念速览

在深入实战之前，理解这些核心概念：

| 概念 | 作用 | 类比 | 官方文档 |
|------|------|------|----------|
| **CLAUDE.md** | AI 的项目宪法，定义规则和约束 | README + 编码规范 | [CLAUDE.md 指南](https://docs.claude.ai/docs/claude-code/claude-md) |
| **Skills** | 封装复杂操作的可执行脚本 | npm scripts | [Skills 文档](https://docs.claude.ai/docs/claude-code/skills) |
| **Slash Commands** | 快捷提示词 | IDE 的代码片段 | [Commands 文档](https://docs.claude.ai/docs/claude-code/slash-commands) |
| **Hooks** | 拦截和验证 AI 行为 | Git hooks | [Hooks 文档](https://docs.claude.ai/docs/claude-code/hooks) |
| **Planning Mode** | 大型任务的计划审批流程 | 技术设计评审 | [Planning Mode](https://docs.claude.ai/docs/claude-code/planning-mode) |
| **Subagents** | 委派子任务给专门的 AI | 微服务架构 | [Agents 文档](https://docs.claude.ai/docs/claude-code/agents) |
| **MCP** | Model Context Protocol，连接外部工具 | API 网关 | [MCP 规范](https://modelcontextprotocol.io/) |

**核心工作流**：

```
用户提需求 → Claude 读取 CLAUDE.md → 调用 Hooks 验证
          ↓
    执行 Skills（自动化操作）
          ↓
    生成代码 → 运行测试 → 提交 git
          ↓
    返回结果 → 用户审查
```

---

## 2.3 工具对比：Claude Code vs. 竞品

| 特性 | Claude Code | Cursor | Cline | GitHub Copilot | Gemini CLI |
|------|-------------|--------|-------|----------------|------------|
| **代码生成** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| **命令执行** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐ | ⭐⭐⭐⭐ |
| **上下文理解** | ⭐⭐⭐⭐⭐ (200K) | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| **可编程性** | ⭐⭐⭐⭐⭐ (SDK) | ⭐⭐⭐ | ⭐⭐ | ⭐⭐ | ⭐⭐ |
| **安全性控制** | ⭐⭐⭐⭐⭐ (Hooks) | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐ |
| **UI 友好度** | ⭐⭐ (纯CLI) | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐ |
| **学习曲线** | 中等 | 低 | 中等 | 低 | 中等 |
| **MCP 支持** | ✅ 原生 | ❌ | ✅ 集成 | ❌ | ❌ |

**实际使用体验对比**（基于 Finance 项目）：

**Claude Code**：
- ✅ **优势**：完全的 CLI 自主性，一旦习惯后效率极高，可深度定制
- ✅ **优势**：Skills 和 Hooks 构建强大工作流（如 `/mysql-exec`、`/git-commit-push`）
- ❌ **劣势**：初期不习惯纯命令行界面，缺少可视化错误提示
- **适合**：熟悉命令行的开发者，需要高度自动化的项目

**Cline（VS Code 集成）**：
- ✅ **优势**：界面友好，错误提示直观，与 IDE 无缝集成
- ✅ **优势**：支持 MCP，可连接外部工具和数据源
- ❌ **劣势**：自定义能力弱于 Claude Code，依赖 VS Code
- **适合**：习惯 IDE 的开发者，重视可视化反馈

**Cursor**：
- ✅ **优势**：开箱即用，学习成本最低，UI 设计优秀
- ❌ **劣势**：可编程性较弱，难以构建复杂自动化流程
- **适合**：个人项目、快速原型、AI 编程新手

**选择建议**：
- **个人项目/快速原型** → Cursor（UI 友好，开箱即用）
- **企业级项目/定制需求** → Claude Code（可编程、可审计、深度自动化）
- **团队协作/需要可视化** → Cline（VS Code 集成，界面友好）
- **纯代码补全** → GitHub Copilot（轻量级，集成在编辑器中）

---

## 2.4 为什么选择 Claude Code

在众多 AI 编程工具中，我最终选择 Claude Code 作为 Finance 项目的主力工具，核心原因：

**1. 完整的 CLI 自主性**

Claude Code 不仅能生成代码，还能：
- 执行 bash 命令（运行测试、构建项目、启动服务）
- 管理 git（暂存、提交、推送、创建分支）
- 操作数据库（通过 Skills 封装 MySQL 操作）
- 构建和部署（Docker 镜像、Kubernetes 配置）

**实际案例**：
```bash
You: 增加支出预算功能，包括后端 API、前端页面、数据库迁移

Claude:
1. [创建数据库迁移脚本]
2. [执行 /mysql-exec database/add_budget_table.sql]
3. [生成 ExpenseBudget.java、BudgetService.java、BudgetController.java]
4. [运行 ./mvnw test 验证后端]
5. [创建 BudgetManagementView.vue]
6. [运行 npm run build 验证前端]
7. [执行 /git-commit-push "feat: add expense budget management"]

Done! 功能已完成并推送到 GitHub。
```

**2. 强大的上下文理解**

- **200K token 窗口**（Sonnet 4）：可以"看到"整个 Finance 项目
- **智能上下文选择**：自动读取相关文件（通过 CLAUDE.md 的 `@mentions`）
- **会话记忆**：跨多个请求保持上下文一致性

**实际数据**：
- Finance 项目有 127 个 Java 文件和 33 个 Vue 组件
- Claude Code 能够理解跨层依赖（如前端调用哪个后端 API，数据库表结构）
- 生成的代码保持命名和架构一致性（参考 CLAUDE.md 中的设计原则）

**3. 可编程性（Skills & Hooks）**

通过 Skills 封装复杂操作，通过 Hooks 控制 AI 行为。

**我的 Skills 示例**：
- `/setup-java`：配置 Java 17 环境 + 加载数据库凭据
- `/mysql-exec`：执行 SQL 文件/查询，自动读取 `.env` 凭据
- `/git-commit-push`：暂存 → 提交（Conventional Commits）→ 推送
- `/docker-build-push`：多架构镜像构建（amd64/arm64）

**Hooks 示例**（防止 AI 在测试未通过时提交代码）：
```bash
# .claude/hooks/pre-tool-use.sh
if [[ "$TOOL_NAME" == "Bash" ]] && [[ "$COMMAND" =~ "git commit" ]]; then
  if [ ! -f /tmp/tests-passed ]; then
    echo "❌ Tests have not passed. Run tests first."
    exit 1
  fi
fi
```

**4. 安全性与可控性**

- **权限管理**：可配置 AI 能否执行某些命令（如 `rm -rf`）
- **Hook 拦截**：在 AI 执行操作前验证（如提交前必须跑测试）
- **审计日志**：所有操作记录在 `.claude/logs/` 中
- **沙盒模式**：可在隔离环境中测试 AI 行为

**实际效果**：
在 Finance 项目的 71 次提交中，没有一次因 AI 误操作导致代码丢失或破坏。

**5. 实际数据验证**

在 Finance 项目中，**100% 的代码都是由 Claude Code 生成的**，包括：
- 127 个 Java 后端文件（Controllers、Services、Repositories、Models）
- 33 个 Vue 组件（完整的前端界面）
- 25 张数据库表及所有迁移脚本和存储过程
- Docker 容器化配置和 CI/CD 流程

**项目规模**：
- **后端代码**：15,748 行 (Java)
- **前端代码**：25,131 行 (Vue/JavaScript/TypeScript，33 个组件)
- **数据库**：25 张表，11 个迁移脚本
- **总代码量**：约 4.1 万行
- **开发周期**：2025年11月 - 至今
- **净开发时间**：约 20 小时（周末和晚上的碎片时间）
- **效率提升**：相比传统开发方式，估计提升 4-5 倍

**选择 Claude Code 的决策因素**：
- ✅ 项目需要高度自动化（后端 + 前端 + 数据库 + 部署）
- ✅ 我熟悉命令行操作，不依赖 IDE
- ✅ 需要可编程性（Skills/Hooks）构建标准化工作流
- ✅ 重视安全性和可审计性（企业级要求）

**如果你满足以下条件，也推荐使用 Claude Code**：
- 熟悉 bash/zsh 等命令行工具
- 项目需要跨层操作（代码 + 数据库 + 部署）
- 希望构建可复用的自动化工作流
- 重视代码质量和安全控制

---

## 2.5 快速上手

**安装 Claude Code**：
```bash
# macOS/Linux
npm install -g @anthropic/claude-code

# 配置 API Key
export ANTHROPIC_API_KEY=your-api-key

# 启动
claude
```

**第一个任务**：
```bash
You: 创建一个简单的 Express.js 服务器，监听 3000 端口，返回 Hello World

Claude: [生成 server.js、package.json、运行 npm install、启动服务器]

You: 访问 http://localhost:3000 验证

Claude: [提供测试命令 curl http://localhost:3000]
```

**推荐学习路径**：
1. 阅读官方文档：https://docs.claude.ai/docs/claude-code
2. 尝试简单任务（生成代码、运行测试）
3. 学习 CLAUDE.md 配置（第三章详解）
4. 创建第一个 Skill（第三章有实例）
5. 配置 Hooks 控制 AI 行为

**下一章预告**：
第三章将深入 Finance 项目的开发全流程，展示如何使用 Claude Code 从零构建一个生产级全栈应用。

---

[← 上一章：引言](./01-引言.md) | [返回目录](./00-目录.md) | [下一章：项目开发全流程 →](./03-项目开发全流程实战.md)
