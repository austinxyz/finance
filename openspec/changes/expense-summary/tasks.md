# Tasks — expense-summary

**说明**：组 1–3 不依赖样本 CSV，可立即开工。
组 4–5 各自阻塞于对应机构的样本文件（见 design.md Open Questions），样本到一份做一组。

测试命令基线：`python -m unittest discover -s tools/expense-import -t tools/expense-import`
（`openspec/config.yaml` 无 `project.test_commands` 段；此命令为 Phase A 已建立并验证可运行的套件，
当前 13 个测试全绿。）

## 1. 配置、目录发现与期间归属

### Contract
- **Spec**:
  - 系统 SHALL 从 `~/finance-data/<YYYY-MM>/` 目录读取当月所有 CSV 文件，按文件表头自动识别所属机构，并从目录名推导记账期间。用户 MUST NOT 需要逐个列出文件名或手动指定期间。
  - 系统 SHALL 按每笔交易自身的日期归属 `expense_period`，而非按文件所在目录或信用卡账单周期。落在目录期间之外的交易 MUST 被过滤，且过滤笔数 MUST 报告给用户。
  - CSV 原始文件 SHALL 存放于仓库外的目录（默认 `~/finance-data/`），路径可配置。仓库中 MUST NOT 出现任何含账号、余额或商户明细的文件。本地产出物 MUST 被 `.gitignore` 排除。
- **Runtime**: `python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_discover.py"` → expected: 目录发现、期间推导、跨期过滤、来源识别失败四类用例全绿，无 import 错误
- **Code**:
  - D5 — 解析器按 `header_signature` 表头识别，不按文件名；用户重命名或换账号不应导致失效。
  - D8 — `sources.toml` 独立于 `rules.toml`：预期来源清单与商户分类规则是不同关注点。
  - 跨期交易必须**报告笔数**而非静默丢弃 —— 用户需要知道它们会在下月目录被处理。
  - 金融数据不进仓库：默认路径在 `~/finance-data/`，产出物写入被 gitignore 的目录。
- **Threshold**: 80

- [ ] 1.0 CONTRACT — write openspec/changes/expense-summary/contracts/group-1.md with the ### Contract block above; confirm all three fields (Spec, Runtime, Code) are non-empty before proceeding
- [ ] 1.1 RED — 写 `test_discover.py`：给定含三份不同表头 CSV 的临时目录，`discover()` 返回三个来源及各自解析器名与交易笔数
- [ ] 1.2 GREEN — 实现 `importer/discover.py` 的目录扫描与表头识别派发（复用 `parsers.parser_for`）
- [ ] 1.3 RED — 测试期间推导：目录名 `2026-08` → 期间 `2026-08`；目录名非法（如 `august`）→ 报错并指明期望格式
- [ ] 1.4 GREEN — 实现期间推导与目录名校验
- [ ] 1.5 RED — 测试跨期过滤：目录 `2026-08` 下的文件含 3 笔 2026-09 交易 → 这 3 笔被排除，且返回值中带「已过滤 3 笔」的计数
- [ ] 1.6 GREEN — 实现按交易日期归属期间 + 过滤计数上报
- [ ] 1.7 RED — 测试失败路径：目录不存在 → 报错并指出期望路径，不创建空目录；某 CSV 表头不匹配任何解析器 → 报错并打印表头与已注册解析器清单
- [ ] 1.8 GREEN — 实现两条失败路径，均以非 0 退出码终止
- [ ] 1.9 RED — 写 `sources.toml` 的加载与校验测试：每项含 `id` / `parser` / 可选 `manual`；缺字段或 `parser` 未注册 → 加载期报错
- [ ] 1.10 GREEN — 实现 `sources.toml` schema 与加载器；新建 `sources.toml` 声明 6 家来源
- [ ] 1.11 确认 `.gitignore` 覆盖工具产出目录，且默认 CSV 根路径指向仓库外（`~/finance-data/`），可经环境变量或命令行覆盖
- [ ] 1.E EVAL — spawn evaluator subagent (haiku); reads contracts/group-1.md + spec + design + group diff; invokes superpowers:requesting-code-review (CRITICAL/HIGH = BLOCK); scores Spec/Runtime/Code; total ≥ 80 → PASS; < 80 → append FIX tasks + retry (max 3 attempts, plateau < 5pt = escalate)

## 2. 三道写入闸门

### Contract
- **Spec**:
  - 系统 SHALL 在存在 `UNKNOWN` 交易时拒绝写入数据库，并列出每笔的日期、金额、描述，退出码非 0。`--allow-unknown` 开关 MAY 覆盖此闸门，将 `UNKNOWN` 归入「其他/未分类」（`minor_category_id = 80`），且 MUST 在控制台明确打印被兜底的笔数与总金额。
  - 系统 SHALL 在存在 `FUNDING` 交易、但目录中缺少其 `funding_target` 对应来源的 CSV 时，拒绝写入并指名缺失来源。`--allow-gaps` 开关 MAY 覆盖。
  - 系统 SHALL 依据配置中声明的预期来源清单，检查目录是否包含每一家的 CSV，缺任何一家即拒绝写入并指名。标记为 `manual` 的来源 MUST 每月提示用户手工补录，且 MUST NOT 被当作已覆盖。`--allow-missing-sources` 开关 MAY 覆盖。
  - 规则文件 MUST NOT 包含兜底规则；未命中任何规则的交易归为 `UNKNOWN`。
- **Runtime**: `python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_gates.py"` → expected: 三道闸门各自的拒写与逃生舱用例、以及 `manual` 来源用例全绿
- **Code**:
  - 闸门是 **fail-closed**：默认拒写，逃生舱必须是显式命令行开关，且触发时在控制台打印被放行的笔数与金额。
  - 闸门 3 存在的唯一理由：信用卡账单漏导在任何 CSV 中都不留痕迹，前两道闸门无法发现，月度总额会静默偏小。删掉它等于留下一个无声的漏计通道。
  - `gates.py` 写成纯函数（输入分类结果 + 来源清单，输出判定），不碰网络与文件系统，可直接单测（D7）。
  - `FUNDING` 规则必须带 `funding_target`，否则闸门 2 无从判断指向哪家 —— 加载期校验，不是运行期。
- **Threshold**: 80

- [ ] 2.0 CONTRACT — write openspec/changes/expense-summary/contracts/group-2.md with the ### Contract block above
- [ ] 2.1 RED — 测试 `rules.toml` 加载期校验新增项：`FUNDING` 规则缺 `funding_target` → `RuleError` 并指明第几条规则与规则名
- [ ] 2.2 GREEN — 在 `classify.py` 的 `Rule` 与 `load_rules` 中加入 `funding_target` 字段与校验；更新现有 `rules.toml` 中的 paypal/venmo 规则
- [ ] 2.3 RED — 写 `test_gates.py`：存在 `UNKNOWN` 时闸门 1 判定为拒写，返回值含每笔的日期/金额/描述
- [ ] 2.4 GREEN — 实现 `importer/gates.py` 的 UNKNOWN 闸门（纯函数）
- [ ] 2.5 RED — 测试 `--allow-unknown` 逃生舱：`UNKNOWN` 被归入 `minor_category_id = 80`，且返回值带被兜底的笔数与总金额供控制台打印
- [ ] 2.6 GREEN — 实现逃生舱路径
- [ ] 2.7 RED — 测试闸门 2：存在 `funding_target = "paypal"` 的 `FUNDING` 交易共 $161.59、但来源集合中无 paypal → 拒写并指名 paypal 与缺口金额；有 paypal 来源时放行
- [ ] 2.8 GREEN — 实现 FUNDING 缺口闸门
- [ ] 2.9 RED — 测试闸门 3：`sources.toml` 声明的来源缺失 → 拒写并指名；标记 `manual` 的来源缺失 → 不拒写但返回提示信息
- [ ] 2.10 GREEN — 实现来源缺失闸门与 `manual` 分支
- [ ] 2.11 RED — 回归测试：`rules.toml` 中不存在兜底规则（任意描述的随机交易仍判为 `UNKNOWN`）
- [ ] 2.12 GREEN — 若测试暴露兜底规则则移除；确认注释说明保留
- [ ] 2.E EVAL — spawn evaluator subagent (haiku); reads contracts/group-2.md + spec + design + group diff; invokes superpowers:requesting-code-review (CRITICAL/HIGH = BLOCK); scores Spec/Runtime/Code; total ≥ 80 → PASS; < 80 → append FIX tasks + retry (max 3 attempts, plateau < 5pt = escalate)

## 3. 对账式写库

### Contract
- **Spec**:
  - 系统 SHALL 在写入前调用 `GET /api/expenses/records?period=<期间>` 取得远端现状，与本次聚合结果比对后执行：本次有的小类走 `POST /api/expenses/records/batch`；远端有、本次无（或净额非正）的小类走 `DELETE /api/expenses/records/{id}`。
  - 对账 MUST 只作用于 `currency = "USD"` 的记录。其他币种的记录 MUST NOT 被本工具删除或修改 —— `expense_records` 按 `(family_id, expense_period, minor_category_id, currency)` 去重，同一小类的不同币种是彼此独立的行。
  - 系统 SHALL 保证同一目录连续执行两次写入后，数据库状态一致，且不产生重复行。
  - 系统 SHALL 从环境变量 `FINANCE_API_BASE`、`FINANCE_USERNAME`、`FINANCE_PASSWORD` 读取连接信息，登录换取 JWT 后调用 API。凭据 MUST NOT 出现在仓库中。`family_id` 由后端从 JWT 推导，客户端不指定。
  - 系统 SHALL 在聚合阶段将同一小类内的退款与消费冲抵，取净额。净额 MUST NOT 作为负数或零提交给后端。
- **Runtime**: `python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_reconcile.py"` → expected: diff 逻辑、币种隔离（三个场景）、幂等、凭据缺失、登录失败用例全绿，且不发起任何真实网络请求
- **Code**:
  - **D4 是本 change 最高风险项**：对账必须硬性过滤 `currency == "USD"`。用户经 `ExpenseBatchUpdate.vue` 的币种选择器手录过 CNY 记录，若不过滤，「远端有本次无」会把它们全部删除 —— 静默数据丢失，且无法从工具侧恢复。
  - D3 — 只 POST 不 DELETE 会让历次规则错误在库中永久沉积；「重跑幂等」不等于「库等于本地聚合结果」。
  - D7 — `reconcile.py` 是纯函数（输入本地聚合 + 远端记录列表，输出 to_post / to_delete），不碰网络；`apiclient.py` 是唯一 I/O 边界，通过参数注入，测试用 fake client 替换。
  - 本工具不做任何币种换算，因此不涉及 `ExchangeRateService`：6 家来源原币即 USD，写入时 `currency` 恒为 `"USD"`（D4 末段）。
  - `expense_records` 是快照语义而非 append-only 时间序列（D6），写入即「让该期间的快照等于本地聚合结果」。
  - 非 2xx 响应一律报错退出，不吞异常；写入后打印实际写入/删除的小类清单。
- **Threshold**: 80

- [ ] 3.0 CONTRACT — write openspec/changes/expense-summary/contracts/group-3.md with the ### Contract block above
- [ ] 3.1 RED — 写 `test_reconcile.py`：给定本地聚合 `{68: 259.24}` 与远端 USD 记录 `[{id:5, minorCategoryId:68, amount:100, currency:"USD"}]` → `reconcile()` 返回 to_post 含 68、to_delete 为空
- [ ] 3.2 GREEN — 实现 `importer/reconcile.py` 的纯函数 diff
- [ ] 3.3 RED — 测试清除路径：远端有 `{id:9, minorCategoryId:72, currency:"USD"}` 但本地聚合无 72 → to_delete 含 id 9
- [ ] 3.4 GREEN — 实现「远端有、本地无」的删除判定
- [ ] 3.5 RED — 测试净额非正：本地某小类净额为 0 或负 → 不出现在 to_post；若远端存在其 USD 记录则出现在 to_delete
- [ ] 3.6 GREEN — 实现净额非正的删除转向（衔接聚合阶段的退款冲抵）
- [ ] 3.7 RED — **币种隔离测试（最高风险，三个场景）**：(a) 远端同一小类同时有 USD 与 CNY 记录 → 只更新 USD，CNY 既不更新也不删除；(b) 本地聚合为空且远端只有 CNY 记录 → to_delete 为空；(c) 远端全为 CNY 记录 → 工具不产生任何删除操作
- [ ] 3.8 GREEN — 在 diff 入口硬性过滤 `currency == "USD"`；对该过滤条件加断言与注释说明后果
- [ ] 3.9 RED — 写 `apiclient` 的 fake 与契约测试：登录返回 JWT 后，后续请求携带 `Authorization` 头；请求体不含 `familyId`（由后端从 JWT 推导）
- [ ] 3.10 GREEN — 实现 `importer/apiclient.py`（login / get_records / batch_save / delete_record），依赖注入形式暴露给写入流程
- [ ] 3.11 RED — 测试凭据缺失：未设 `FINANCE_PASSWORD` → 报错指明缺少哪个环境变量，且不发起任何请求；登录返回非成功 → 报告认证失败并以非 0 退出，不执行任何写入或删除
- [ ] 3.12 GREEN — 实现凭据校验与登录失败处理
- [ ] 3.13 RED — 测试默认不写库：未加写入开关时不发起任何 API 调用，仅产出 `review.csv` 与聚合预览，且无需凭据即可完成
- [ ] 3.14 GREEN — 在 `run.py` 中实现写入开关与默认只读路径
- [ ] 3.15 RED — 幂等测试：对同一输入连续执行两次写入流程（fake client 记录调用），第二次结束后的目标状态与第一次相同，且每个 `(period, minorCategoryId, currency)` 组合只有一行
- [ ] 3.16 GREEN — 补齐幂等所需逻辑；写入后打印实际写入/删除的小类清单
- [ ] 3.E EVAL — spawn evaluator subagent (haiku); reads contracts/group-3.md + spec + design + group diff; invokes superpowers:requesting-code-review (CRITICAL/HIGH = BLOCK); scores Spec/Runtime/Code; total ≥ 80 → PASS; < 80 → append FIX tasks + retry (max 3 attempts, plateau < 5pt = escalate)

## 4. 解析器：Chase 信用卡与 BOA checking

> **阻塞**：开工前需要 Chase 信用卡与 BOA checking 各一份样本 CSV（任意月份）。

### Contract
- **Spec**:
  - 系统 SHALL 保证同一笔真实消费只被计入一次。信用卡消费 MUST 只从信用卡账单计入，对应的 checking 还款 MUST 判为 `TRANSFER`。
  - 系统 SHALL 从 `~/finance-data/<YYYY-MM>/` 目录读取当月所有 CSV 文件，按文件表头自动识别所属机构。
- **Runtime**: `python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_parsers_*.py"` → expected: 两个解析器的真实样本行用例全绿，金额符号断言通过
- **Code**:
  - **符号约定是最容易犯且后果最大的解析错误**：信用卡账单中消费通常为正数，与 checking 相反。每个解析器在 `parse_row` 内归一为「负数 = 支出」，并用真实样本行断言符号。搞反会让整月开支变成负数。
  - D5 — 通过 `header_signature` 声明表头特征供自动识别；新增机构 = 新增模块 + 注册，管道其余部分不动。
  - 跨账户去重靠规则而非解析器：checking 里的信用卡还款由结构性规则判为 `TRANSFER`，且该规则必须排在所有商户规则之前。
- **Threshold**: 80

- [ ] 4.0 CONTRACT — write openspec/changes/expense-summary/contracts/group-4.md with the ### Contract block above
- [ ] 4.1 取得 Chase 信用卡与 BOA checking 的样本 CSV，记录各自表头、日期格式、金额符号约定到 `tools/expense-import/README.md`
- [ ] 4.2 RED — 写 `test_parsers_chase_card.py`：用真实样本行断言解析出的日期、描述、金额，**并断言一笔消费的 `amount` 为负**
- [ ] 4.3 GREEN — 实现 `importer/parsers/chase_card.py` 并注册；在 `parse_row` 内完成符号归一
- [ ] 4.4 RED — 写 `test_parsers_boa_checking.py`：真实样本行断言字段与符号
- [ ] 4.5 GREEN — 实现 `importer/parsers/boa_checking.py` 并注册
- [ ] 4.6 RED — 跨账户去重测试：同时给入 checking 的信用卡还款行与信用卡账单的消费行 → 还款判为 `TRANSFER`，月度聚合只反映卡账单消费
- [ ] 4.7 GREEN — 补齐/调整结构性规则使其覆盖两家的还款描述格式，确保排在商户规则之前
- [ ] 4.8 用真实样本跑一次完整流程（不写库），核对 `review.csv` 的分类结果与 `UNKNOWN` 比例，按需补商户规则
- [ ] 4.E EVAL — spawn evaluator subagent (haiku); reads contracts/group-4.md + spec + design + group diff; invokes superpowers:requesting-code-review (CRITICAL/HIGH = BLOCK); scores Spec/Runtime/Code; total ≥ 80 → PASS; < 80 → append FIX tasks + retry (max 3 attempts, plateau < 5pt = escalate)

## 5. 解析器：PayPal、Venmo 与 Robinhood

> **阻塞**：开工前需要 PayPal、Venmo 样本 CSV，以及 Robinhood 是否支持 CSV 导出的确认。

### Contract
- **Spec**:
  - 经 PayPal / Venmo 支付的消费 MUST 只从该平台账单计入，对应的 checking 充值 MUST 判为 `FUNDING`。
  - 标记为 `manual` 的来源 MUST 每月提示用户手工补录，且 MUST NOT 被当作已覆盖。
- **Runtime**: `python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_parsers_*.py"` → expected: 已实现平台的样本行用例全绿；Robinhood 若标记 `manual` 则该来源无解析器测试但有 `sources.toml` 配置断言
- **Code**:
  - Venmo 是本 change 规则设计最不确定的一块：既有朋友间转账（可能属「人情」，也可能只是代付后被还款），也有商户消费。需按样本决定是按对方类型区分，还是做「代付-还款」配对抵消。
  - 与闸门 2 的衔接：PayPal/Venmo 解析器落地后，checking 中对应的 `FUNDING` 交易才有配对来源，闸门 2 才会放行。
  - Robinhood 若确认无 CSV 导出，在 `sources.toml` 标记 `manual` 并跳过解析器 —— 关键是每一家都被明确覆盖或明确标记，不能有沉默的空白。
- **Threshold**: 80

- [ ] 5.0 CONTRACT — write openspec/changes/expense-summary/contracts/group-5.md with the ### Contract block above
- [ ] 5.1 取得 PayPal、Venmo 样本 CSV；确认 Robinhood 信用卡是否支持 CSV 导出，结论记入 README
- [ ] 5.2 RED — 写 `test_parsers_paypal.py`：真实样本行断言字段与符号
- [ ] 5.3 GREEN — 实现 `importer/parsers/paypal.py` 并注册
- [ ] 5.4 RED — PayPal 去重测试：checking 的充值行判为 `FUNDING`，PayPal 账单的消费行计入分类，同一笔钱不重复计
- [ ] 5.5 GREEN — 调整规则使 PayPal 充值与账单消费正确配对
- [ ] 5.6 分析 Venmo 样本，判定个人转账与商户消费的区分策略，把结论与选定方案写入 design.md 的 Open Questions 对应条目
- [ ] 5.7 RED — 写 `test_parsers_venmo.py`：覆盖商户消费、个人转出、个人转入三类样本行
- [ ] 5.8 GREEN — 实现 `importer/parsers/venmo.py` 并注册；按 5.6 的方案实现分类策略
- [ ] 5.9 GREEN — Robinhood 分支：有 CSV 则实现解析器（RED 先行，比照 4.2/4.3 的符号断言）；无 CSV 则在 `sources.toml` 标记 `manual` 并补一条断言该来源被提示而非静默忽略的测试
- [ ] 5.E EVAL — spawn evaluator subagent (haiku); reads contracts/group-5.md + spec + design + group diff; invokes superpowers:requesting-code-review (CRITICAL/HIGH = BLOCK); scores Spec/Runtime/Code; total ≥ 80 → PASS; < 80 → append FIX tasks + retry (max 3 attempts, plateau < 5pt = escalate)

## 6. 验证与收尾

- [ ] 6.1 跑完整 Python 测试套件确认无回归：`python -m unittest discover -s tools/expense-import -t tools/expense-import`
- [ ] 6.2 跑后端测试套件确认未受影响：`cd backend && mvn test`（本 change 不改后端，此步用于确认确实未改动）
- [ ] 6.3 跑前端测试套件确认未受影响：`cd frontend && npm test`（本 change 不改前端，同上）
- [ ] 6.4 端到端冒烟：把 2026-08 各家真实 CSV 放入 `~/finance-data/2026-08/`，先只读跑一次补规则至无 `UNKNOWN`，再写库；打开分析页确认 2026-08 分类开支与 `review.csv` 聚合逐项一致
- [ ] 6.5 幂等冒烟：对同一目录再跑一次写入，确认 `expense_records` 的 2026-08 记录无变化、无重复行
- [ ] 6.6 对账冒烟：故意改一条规则制造错误分类并写入，再改回并重跑，确认错误小类被清除而非残留
- [ ] 6.7 币种安全冒烟：在 `ExpenseBatchUpdate.vue` 手工录一条 2026-08 的 CNY 记录，重跑写入，确认该 CNY 记录未被删除或修改
- [ ] 6.8 确认仓库中无金融数据：`git status` 干净，`git ls-files` 中无 CSV 与产出物，凭据不在仓库内
- [ ] 6.9 更新 `tools/expense-import/README.md`：目录约定、三道闸门与逃生舱、各家 CSV 导出路径、每月操作流程
- [ ] 6.10 Run superpowers:verification-before-completion（跑上述测试命令；确认无调试残留；确认 `.gitignore` 覆盖产出物）
