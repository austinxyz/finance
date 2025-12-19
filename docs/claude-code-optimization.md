# Claude Code 优化指南

基于 Shrivu Shankar 的最佳实践对本项目的 CLAUDE.md 和 Skills 进行的优化。

## 优化原则

### 1. CLAUDE.md 是 Guardrails，不是 Manual

**之前的问题**:
- 13KB+ 的详细文档，包含大量"如何做"的说明
- 像是一本操作手册，而不是指导原则
- 每次会话都要加载大量可能用不到的内容

**优化后**:
- 专注于"什么会出错"和"如何避免"
- 使用 ALWAYS/NEVER 格式的明确规则
- 将详细文档移到外部文件，用指针引用
- 从原来的 ~200 行精简到 ~160 行，但信息密度更高

**示例**:
```markdown
# 之前
## 数据库操作
使用 /mysql-exec 技能来执行数据库操作。这个技能支持三种模式...
[详细说明 10+ 行]

# 优化后
**Use `/mysql-exec` skill for all DB operations**:
/mysql-exec path/to/script.sql    # Execute SQL file
/mysql-exec "SELECT ..."          # Quick query

**NEVER run raw `mysql` commands** - The skill handles credentials and connection.
```

### 2. 提供替代方案，而非仅禁止

**之前的问题**:
```markdown
- NEVER use inline styles
- 时序数据记录是核心模式：创建新时间戳记录，而非更新现有值
```

**优化后**:
```markdown
**NEVER use inline styles** - Use Tailwind classes or CSS variables.
See `docs/frontend-best-practices.md` for theme system.

**ALWAYS create new time-series records** - Never UPDATE existing
asset/liability records. Create new records with new timestamps.
```

每个 NEVER 都配对一个 ALWAYS 或具体的替代方案。

### 3. Anti-Patterns 部分

新增的 "Common Anti-Patterns" 章节教会 Claude（和开发者）什么是不好的做法：

```markdown
❌ **Don't**: Create comprehensive documentation in CLAUDE.md for every feature
✅ **Do**: Document what Claude gets wrong. Point to external docs for details.

❌ **Don't**: Write "Never use feature X" without alternatives
✅ **Do**: Write "Never use X, prefer Y because [reason]"
```

这种对比格式让 AI 更容易理解和遵循。

### 4. When Things Go Wrong 章节

**创新点**: 基于实际使用中的常见错误创建快速诊断指南

```markdown
**"No Java compiler available"** → Run `/setup-java` to set JAVA_HOME
**JPA schema mismatch** → Check `backend/.env` credentials. You may be pointing to wrong DB.
**Currency conversion errors** → Verify exchange rate exists for the date. Use `ExchangeRateService.getOrFetchRate()`.
```

这是 Shrivu 提到的"基于 Claude 犯的错误来记录"的实践。

## Skills 优化

### 原则：Skills 应该是简单的 CLI 包装

当前的 4 个 skills 都遵循最佳实践：

1. **setup-java** - 环境配置的确定性工具
2. **mysql-exec** - 数据库操作的统一入口
3. **git-commit-push** - Git 工作流的原子操作
4. **docker-build-push** - 多架构构建的复杂度封装

**关键原则**:
- 每个 skill 只做一件事，但做好
- 封装复杂性，暴露简单接口
- 提供确定性输出，便于 AI 理解

### 新增：/catchup Slash Command

为了配合上下文管理，新增了 `/catchup` 命令：

**目的**: 在 `/clear` 后快速恢复上下文

**实现**: 简单的 slash command（不是 skill，因为够简单）

```markdown
# .claude/commands/catchup.md
Read all files that have been modified in the current git branch
to restore context after `/clear`.
```

**为什么用 Slash Command 而不是 Skill？**
- 简单的提示词包装，不需要外部脚本
- 只用于个人工作流，不需要共享
- 符合 Shrivu 的建议："slash commands 应该是简单的快捷方式"

## Context 管理策略

### 推荐工作流

**简单重启**（推荐用于日常开发）:
```
1. /clear
2. /catchup
3. 继续工作
```

**复杂任务重启**（多天大功能）:
```
1. 让 Claude 记录进度到 docs/wip/feature-name.md
2. /clear
3. 读取 WIP 文档继续
```

**明确反对**:
- ❌ 不使用 `/compact` - 不透明且容易出错
- ❌ 不创建自定义 subagents - 隐藏上下文
- ✅ 用 Task(...) 让主 agent 动态委派

## 对比：优化前后

### Token 使用效率

| 指标 | 优化前 | 优化后 | 改进 |
|------|--------|--------|------|
| CLAUDE.md 大小 | ~13KB | ~8KB | -38% |
| 基线 token 消耗 | ~6500 | ~4000 | -38% |
| 首次加载信息密度 | 中 | 高 | 关键规则前置 |

### 可维护性

**优化前**:
- 新功能 → 在 CLAUDE.md 添加详细说明 → 文件膨胀
- 临时 workaround → 加到 CLAUDE.md → 技术债积累

**优化后**:
- 新功能 → 只记录常见错误和反模式 → 保持精简
- 临时问题 → 修复根因，文档最佳实践 → 持续改进

### 学习曲线

**对开发者**:
- 优化前：需要读完整个文档才能理解项目约定
- 优化后：快速扫描 Guardrails，需要时查阅外部文档

**对 Claude**:
- 优化前：每次会话加载大量可能无关的上下文
- 优化后：高密度关键规则，快速定位问题

## 未来优化方向

基于 Shrivu 的其他建议，可考虑：

1. **Hooks**: 添加 PreToolUse hook 在 git commit 前强制运行测试
   ```bash
   # .claude/hooks/pre-tool-use.sh
   # 检查 /tmp/tests-pass 文件，强制测试通过后才能提交
   ```

2. **CLI 工具替代 MCP**:
   - 如果需要集成外部服务（如 Jira、监控系统）
   - 优先创建简单的 CLI wrapper
   - 避免创建复杂的 MCP server

3. **Planning Mode 集成**:
   - 对于大功能，强制使用内置 Planning Mode
   - 在 CLAUDE.md 中添加："For features requiring >3 files, use Planning Mode"

4. **自我改进循环**:
   ```bash
   # 定期分析 Claude 的错误
   claude --resume <session-id> -p "Summarize errors you encountered"
   # 将常见错误模式加入 CLAUDE.md
   ```

## 参考资料

- [Shrivu Shankar 的 Claude Code 心得](../使用心得/Shrivu的心得.md)
- [Martin Fowler 访谈 - AI 对软件工程的影响](../使用心得/Martin%20Fowler访谈.pdf)
- [Claude Code 官方文档](https://docs.anthropic.com/claude-code)

## 实施检查清单

- [x] 重写 CLAUDE.md 为 guardrails 格式
- [x] 添加 Anti-Patterns 章节
- [x] 添加 When Things Go Wrong 快速诊断
- [x] 创建 /catchup slash command
- [x] 添加 Context Management 指南
- [ ] 实施 pre-commit hook（测试通过才能提交）
- [ ] 添加 Planning Mode 触发规则
- [ ] 建立错误分析和反馈循环
