## Context

`expense_records` 的数据目前靠 `ExpenseBatchUpdate.vue` 每月手填 32 个小类。
读侧（`ExpenseAnalysisController` 的年度汇总、月度趋势、预算对比）已完整，瓶颈全在人工搬运。

Phase A 已在 `tools/expense-import/` 落地原型：规则引擎（`rules.toml`，tomllib 零依赖）、
`chase_checking` 解析器、`review.csv` 输出、13 个单元测试。
实测 Chase checking 十笔交易中只有 1 笔是真实开支，其余 $14,209 是转账与充值 ——
验证了「跨账户去重」是这个功能的核心难点，而非解析。

本 change 在原型上补齐：目录约定、其余解析器、三道闸门、对账式写库。

**关键约束**：`expense_records` 唯一约束为
`(family_id, expense_period, minor_category_id)`，服务层按
`(familyId, expensePeriod, minorCategoryId, currency)` 查重后 upsert。
**每个小类每期间每币种一行**，不存流水。

## Goals / Non-Goals

**Goals:**

- 一条命令完成「目录 → 分类 → 聚合 → 对账写库」，人工只做核对与补规则。
- 三道 fail-closed 闸门，任何可能导致金额漏计的情况都拒写而非静默通过。
- 重跑幂等，且修正规则后旧值被清除（不只是"不重复写"，而是"库等于本地聚合结果"）。
- 每笔分类可追溯到具名规则。
- 后端与前端零改动。

**Non-Goals:**

- 不存交易流水（`expense_transactions` 表）。
- 不接银行 API，不自动下载。
- 不处理收入、不处理多币种写入。
- 不做任何 UI。

## Decisions

### D1: Python CLI 而非后端能力

**选择**：工具留在 `tools/expense-import/`，通过现有 REST API 写库。后端不加 controller/service/entity。

**理由**：使用频率是每月一次、单人；规则文件手工编辑比做一套规则管理 UI 划算得多；
解析器要随各家 CSV 格式变动频繁调整，放在脚本里改一行即可，放进 Java 要重新部署。

**备选**：
- 后端加 CSV 上传 + 分类 + 预览 API，前端加导入页 —— 工作量数倍，且规则维护反而更笨重。
- 直接写数据库绕过 API —— 会重复实现 upsert 与 `family_id` 推导逻辑，且绕过校验。否决。

### D2: 规则存 TOML 文件，不存数据库

**选择**：`rules.toml`，加载期严格校验（正则合法性、`category` 存在于启用小类、
`EXPENSE` 必带 `category`、`FUNDING` 必带 `funding_target`），任一不满足即失败退出。

**理由**：规则是版本化的配置资产，随代码一起 review 和回滚；机器上无 pyyaml，
`tomllib` 是 Python 3.11+ 标准库，零依赖。规则顺序即优先级，文件形式天然表达顺序。

**备选**：存 DB 并做管理页 —— 与 D1 同理否决。

### D3: 对账式写入（GET + POST + 报告），而非只 POST

**选择**：写入前 `GET /api/expenses/records?period=<期间>` 取远端现状，
diff 后 `POST /api/expenses/records/batch` 写入本次有的，
本次无的**报告给用户**，不删除。

**实施期修正**：原设计为 `DELETE /api/expenses/records/{id}` 清除。
端点确实存在，但后端 `DataProtectionService` 对开启 `is_protected` 的家庭
拒绝一切删除（资产/负债/收入/支出全覆盖），family 1 正是如此。
这是用户主动设的防线，不应为导入便利关闭 —— 改为报告。
报告保留了「不会无声残留」这个核心性质，只是清除动作交回用户。

**理由**：`batchSave` 只 upsert 不删除。若只 POST，第一次因规则错误写入的小类
在规则修正后会永久残留 —— 「重跑幂等」表面成立（重跑结果一致），
实际上库里混着历次错误的沉积。规则会持续演进，这个问题只会越来越严重。

**两个端点均已存在，无需改后端。**
（`DELETE` 端点存在但对本家庭被策略拒绝 —— 端点可用性与策略许可是两件事，
验证时必须分别确认。）

**备选**：后端加一个「按期间全量替换」端点 —— 更干净，但破坏「后端零改动」，
且该端点对手工录入路径有误删风险。否决。

### D4: 对账严格限定 `currency = "USD"`

**选择**：diff 与删除只作用于远端 `currency == "USD"` 的记录，其他币种一律不碰。

**理由**：`ExpenseBatchUpdate.vue` 有币种选择器，用户可能手工录过 CNY 记录；
后端按 `(family, period, minorCategory, currency)` 去重，
同一小类的 USD 与 CNY 是**彼此独立的两行**。
若对账时不按币种过滤，「远端有本次无」会把手录的 CNY 记录全部删掉 —— 静默数据丢失。

这是本设计中风险最高的一处，spec 里为它单列了三个场景。

**多币种换算说明**：本工具**不做任何币种换算**，因此不涉及 `ExchangeRateService`。
6 家账户均为美国机构、原币即 USD，写入时 `currency` 恒为 `"USD"`。
一旦未来引入非 USD 来源，换算必须在写入前完成，并遵循项目规则
（`ExchangeRateService.getExchangeRate(currency, date)` 逐记录调用，
不得直接使用 `ExchangeRateRepository`）。

### D5: 解析器按表头识别，不按文件名

**选择**：每个解析器声明 `header_signature`（必须出现的列名集合），
扫描目录时读首行匹配。已在 Phase A 实现。

**理由**：各家导出文件名带日期/账号且格式不一，用户重命名或换账号就会失效；
表头是格式的稳定标识。新增机构 = 新增一个模块 + 注册，管道其余部分不动。

### D6: 数据访问是快照语义，不是时间序列

`expense_records` 与 `asset_records` / `liability_records` 不同：
**不是 append-only 时间序列**，而是「每期间每小类一行」的快照，靠 upsert 维护。
因此本工具的写入是「让某期间的快照等于本地聚合结果」，
不适用项目中「永远新建记录、不更新」的时间序列约定。

### D7: 模块边界与可测性

```
run.py                 CLI 编排（薄）
importer/
  models.py            Txn / Classified / Action        [已有]
  classify.py          规则引擎                          [已有，加 funding_target]
  report.py            review.csv / 聚合 / 摘要          [已有]
  discover.py          目录扫描 + 期间推导 + 来源识别     [新]
  gates.py             三道闸门，纯函数                   [新]
  reconcile.py         远端 diff，纯函数                  [新]
  apiclient.py         登录 / GET / POST / DELETE        [新，唯一做 I/O 的模块]
  parsers/             每来源一个模块 + 表头注册表
```

`gates.py` 与 `reconcile.py` 写成纯函数（输入分类结果与远端记录，输出判定与操作清单），
不碰网络与文件系统，可直接单测。
`apiclient.py` 是唯一的 I/O 边界，通过参数注入到写入流程，
测试用 fake client 替换 —— 与后端「依赖注入以便 Mockito 打桩」是同一条原则。

### D8: 预期来源清单单独配置

**选择**：新增 `sources.toml`，声明每家来源的 id、解析器、以及 `manual` 标记。

**理由**：闸门 3 需要知道「本月应该有哪几家」。这个清单会随账户增减变动，
与规则（商户 → 分类）是不同的关注点，混在 `rules.toml` 会让两者都难读。
`manual` 标记用于确认无 CSV 导出的来源（可能是 Robinhood），
使其**显式可见**而非沉默缺失。

## Risks / Trade-offs

- **[对账误删用户手录的其他币种记录]** → 硬性限定只处理 `currency = "USD"`；
  spec 中三个场景覆盖（含"本次聚合为空但远端有 CNY"的极端情形）；
  实现时对该过滤条件写专门的单元测试。这是本 change 最高风险项。

- **[规则错误导致静默错分]** → 无兜底规则；`review.csv` 每行带规则名；
  模糊商户（Amazon、Costco、CVS）的规则强制带 `note` 说明假设，提示逐笔核对。
  仍然依赖用户实际去看 `review.csv` —— 工具无法消除这一步。

- **[5 家 CSV 格式未知，工作量不确定]** → 解析器按来源切分为独立任务组，
  样本到一份做一个，不阻塞其余部分；闸门 3 保证未覆盖的来源不会被静默忽略。

- **[各家金额符号约定不一致]** → 拿到样本后实测：Chase 信用卡与 BOA 均为**负数=消费**
  （与 checking 同向，无需翻转）；Robinhood 为**正数=消费**（需翻转）；
  Venmo 用 `+ $45.00` / `- $32.00` 显式符号。**没有"信用卡一律为正"这条规律**，
  每家必须按实际样本确认。每个解析器在 `parse_row` 内归一为「负数=支出」，
  并用格式一致的样本行断言符号。搞反会让整月开支变成负数。

- **[Robinhood 含 Declined 行，不过滤会翻倍]** → 实测样本 8 行中 1 行 `Status = Declined`，
  且金额与同日成功那笔完全相同（重刷）。解析器必须只保留 `Status = Posted`。
  这类"被拒交易也出现在账单里"的情况其他家未见，但新增来源时需逐一确认。

- **[BOA / Venmo 的表头不在第一行]** → BOA 前有 5 行余额摘要，真表头在第 7 行；
  Venmo 前有 2 行账户名，真表头在第 3 行且首列为空。
  `Parser` 基类的表头识别需支持在文件前若干行内定位表头，不能只读第一行。

- **[Venmo 个人转账与消费难以区分]** → 见 Open Questions，需样本才能定策略；
  在拿到样本前该解析器不动工。

- **[凭据泄露]** → 只从环境变量读取，仓库内不存任何凭据；
  CSV 原始文件存仓库外（`~/finance-data/`）；产出物 `.gitignore` 排除。

- **[后端 API 变更导致工具静默失效]** → 工具对非 2xx 响应一律报错退出，
  不吞异常；写入后打印实际写入/删除的小类清单供比对。

## Migration Plan

无部署影响 —— 纯新增的本地工具，不改后端、不改前端、无 schema 变更。

首次使用需：
1. 建目录 `~/finance-data/<YYYY-MM>/`，放入当月 CSV。
2. 设置 `FINANCE_API_BASE` / `FINANCE_USERNAME` / `FINANCE_PASSWORD`。
3. 先不加写入开关跑一次，核对 `review.csv` 并补规则至无 `UNKNOWN`。
4. 加写入开关执行。

回滚：工具本身无状态。若某次写入结果有误，修正 `rules.toml` 后重跑即可 ——
对账机制保证库中状态收敛到本地聚合结果，不需要手工清理。

## Open Questions

全部为**实现期阻塞项**，需在对应任务组开工前解决，不阻塞 tasks.md 生成：

- **5 份样本 CSV**（Chase 信用卡、BOA checking、PayPal、Venmo、Robinhood 信用卡）——
  每个解析器任务组开工前需要对应样本，确定表头、日期格式、金额符号约定。
- **Robinhood 是否有 CSV 导出** —— 若确认只有 PDF，在 `sources.toml` 标记 `manual`，
  跳过该解析器任务组。
- ~~Venmo 个人转账 vs 商户消费的区分策略~~ → **已定**。实测样本 13 笔中 10 笔是收入：
  你垫付网球场地费/聚餐后朋友通过 Venmo 还款。策略为**冲抵对应分类**：
  收入归为该分类的负向金额，由 `aggregate_totals` 已有的退款冲抵逻辑处理，
  使当月该分类反映真实净支出。分类依据 `Note` 字段
  （`tennis|比赛|华运` → 娱乐健身，`hotpot|火锅|dinner|lunch` → 饮食）。
  Note 是自由文本，每月会有新写法落入 UNKNOWN —— Venmo 将是日常补规则的主要来源。

- **[跨月还款导致分类净额为负]** → 若朋友在次月才还款，该分类当月可能净额为负。
  `reconcile` 会将其视为"本次无"并删除远端记录，总额因此偏小。
  当前接受这一偏差（月度口径本就以现金流为准）；若日后成为问题，需引入跨月配对。
