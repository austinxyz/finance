## Purpose

把每月各家银行/支付平台导出的 CSV，经规则分类与跨账户去重后，聚合成 `expense_records`
所需的「按 `expense_period` × `minor_category_id` 的月度金额」并写入数据库，
取代逐笔手输。写入前先与远端对账，保证重跑后库中状态与本地聚合结果完全一致。

## ADDED Requirements

### Requirement: 目录驱动的来源发现

系统 SHALL 从 `~/finance-data/<YYYY-MM>/` 目录读取当月所有 CSV 文件，
按文件表头自动识别所属机构，并从目录名推导记账期间。
用户 MUST NOT 需要逐个列出文件名或手动指定期间。

#### Scenario: 识别目录下的多家 CSV

- **WHEN** `~/finance-data/2026-08/` 下有 Chase checking、Chase 信用卡、BOA、PayPal 四份 CSV
- **THEN** 系统识别出四个来源，报告每个文件解析出的交易笔数与所用解析器名
- **AND** 记账期间取为 `2026-08`

#### Scenario: 表头无法识别

- **WHEN** 目录下某个 CSV 的表头不匹配任何已注册解析器
- **THEN** 系统报错并打印该文件的表头内容与已注册解析器清单
- **AND** 拒绝继续处理，退出码非 0

#### Scenario: 目录不存在

- **WHEN** 指定期间对应的目录不存在
- **THEN** 系统报错并指出期望的目录路径，不创建空目录、不静默按零开支处理

### Requirement: 交易按交易日期归属期间

系统 SHALL 按每笔交易自身的日期归属 `expense_period`，
而非按文件所在目录或信用卡账单周期。
落在目录期间之外的交易 MUST 被过滤，且过滤笔数 MUST 报告给用户。

#### Scenario: CSV 含跨期交易

- **WHEN** `2026-08/` 目录下的 Chase 文件含 3 笔 2026-09 的交易
- **THEN** 这 3 笔被排除在 2026-08 的聚合之外
- **AND** 控制台报告「已过滤 3 笔非本期交易」，说明它们将在下月目录中处理

### Requirement: 规则驱动的可追溯分类

系统 SHALL 依据 `rules.toml` 中按文件顺序排列的规则对每笔交易分类，
第一条命中的规则生效。每笔交易的分类结果 MUST 携带命中的规则名。
规则文件 MUST NOT 包含兜底规则；未命中任何规则的交易归为 `UNKNOWN`。

分类动作限于：`EXPENSE`（计入统计）、`INCOME`、`TRANSFER`、`FUNDING`、`IGNORE`、`UNKNOWN`。
只有 `EXPENSE` 计入月度聚合。

#### Scenario: 每笔分类可追溯

- **WHEN** 分类完成后生成 `review.csv`
- **THEN** 每一行都含「命中规则」列，值为具名规则或空（对应 `UNKNOWN`）
- **AND** 不存在被静默归类而无规则名的 `EXPENSE` 行

#### Scenario: 规则顺序决定优先级

- **WHEN** `rules.toml` 中「信用卡还款」规则排在商户规则之前
- **AND** 一笔描述为 `Payment to Chase card ending in NNNN` 的交易被处理
- **THEN** 它被判为 `TRANSFER`，不被任何商户规则抢走

#### Scenario: 规则文件非法时加载期失败

- **WHEN** `rules.toml` 中某条 `EXPENSE` 规则缺少 `category`，或 `category` 不在启用小类中，
  或 `desc` 正则非法，或 `FUNDING` 规则缺少 `funding_target`
- **THEN** 系统在加载阶段报错并指明是第几条规则、规则名、以及具体问题
- **AND** 不处理任何交易

### Requirement: 跨账户去重

系统 SHALL 保证同一笔真实消费只被计入一次。
信用卡消费 MUST 只从信用卡账单计入，对应的 checking 还款 MUST 判为 `TRANSFER`。
经 PayPal / Venmo 支付的消费 MUST 只从该平台账单计入，
对应的 checking 充值 MUST 判为 `FUNDING`。

#### Scenario: 信用卡还款不重复计账

- **WHEN** Chase checking 含一笔 `-4028.11` 的 `LOAN_PMT`（还 Chase 信用卡）
- **AND** Chase 信用卡账单含当月各笔消费
- **THEN** checking 的还款判为 `TRANSFER` 不计入
- **AND** 月度开支只反映信用卡账单上的消费明细

#### Scenario: PayPal 充值不重复计账

- **WHEN** Chase checking 含 3 笔 `PAYPAL INST XFER` 共 `-161.59`
- **AND** 同目录含 PayPal 账单，其中有对应的消费明细
- **THEN** checking 的 3 笔判为 `FUNDING` 不计入
- **AND** 月度开支只反映 PayPal 账单上的消费分类

### Requirement: 分类内退款冲抵

系统 SHALL 在聚合阶段将同一小类内的退款（正向金额）与消费冲抵，取净额。
净额 MUST NOT 作为负数或零提交给后端（`@DecimalMin("0.01")` 会拒绝）。

#### Scenario: 退款冲抵后仍为正

- **WHEN** 「食/饮食」当月有消费 `-100.00` 和退款 `+25.00`
- **THEN** 该小类聚合为 `75.00` 并提交

#### Scenario: 退款导致净额非正

- **WHEN** 某小类当月净额为 `0` 或负数
- **THEN** 该小类不出现在提交内容中
- **AND** 若远端已存在该小类的 USD 记录，走对账删除（见「对账式写入」）

### Requirement: 写入闸门 — 未分类交易

系统 SHALL 在存在 `UNKNOWN` 交易时拒绝写入数据库，
并列出每笔的日期、金额、描述，退出码非 0。
`--allow-unknown` 开关 MAY 覆盖此闸门，将 `UNKNOWN` 归入「其他/未分类」
（`minor_category_id = 80`），且 MUST 在控制台明确打印被兜底的笔数与总金额。

#### Scenario: 有未分类交易时拒写

- **WHEN** 当月有 7 笔交易未命中任何规则
- **AND** 用户执行写入命令且未加 `--allow-unknown`
- **THEN** 系统拒绝写入，逐笔列出这 7 笔，提示在 `rules.toml` 补规则
- **AND** 数据库未发生任何变更

#### Scenario: 显式逃生舱

- **WHEN** 用户加 `--allow-unknown` 执行
- **THEN** 这 7 笔归入 `minor_category_id = 80`
- **AND** 控制台打印「7 笔未分类共 $X 已归入 其他/未分类」

### Requirement: 写入闸门 — 充值缺口

系统 SHALL 在存在 `FUNDING` 交易、但目录中缺少其 `funding_target` 对应来源的 CSV 时，
拒绝写入并指名缺失来源。`--allow-gaps` 开关 MAY 覆盖。

#### Scenario: 有 PayPal 充值但缺 PayPal 账单

- **WHEN** Chase checking 含 `funding_target = "paypal"` 的充值共 `$161.59`
- **AND** 目录中没有被识别为 PayPal 来源的文件
- **THEN** 系统拒绝写入，报告「有 PayPal 充值 $161.59 但缺 PayPal CSV，这部分开支会漏计」

### Requirement: 写入闸门 — 来源缺失

系统 SHALL 依据配置中声明的预期来源清单，检查目录是否包含每一家的 CSV，
缺任何一家即拒绝写入并指名。标记为 `manual` 的来源 MUST 每月提示用户手工补录，
且 MUST NOT 被当作已覆盖。`--allow-missing-sources` 开关 MAY 覆盖。

此闸门存在的理由：信用卡账单若漏导，在其他任何 CSV 中都不留痕迹
（不像 PayPal 充值会在 checking 留下记录），前两道闸门都无法发现，
月度总额会静默偏小。

#### Scenario: 信用卡账单漏导

- **WHEN** 预期来源清单含 `robinhood_card`
- **AND** 目录中没有对应文件
- **THEN** 系统拒绝写入并报告「缺少来源 robinhood_card」

#### Scenario: 标记为 manual 的来源

- **WHEN** `robinhood_card` 在配置中标记为 `manual`（确认无 CSV 导出）
- **THEN** 系统不因其缺失而拒写
- **AND** 每次运行提示「robinhood_card 需在 ExpenseBatchUpdate 页手工补录」

### Requirement: 对账式写入

系统 SHALL 在写入前调用 `GET /api/expenses/records?period=<期间>` 取得远端现状，
与本次聚合结果比对后执行：本次有的小类走 `POST /api/expenses/records/batch`；
远端有、本次无（或净额非正）的小类走 `DELETE /api/expenses/records/{id}`。

对账 MUST 只作用于 `currency = "USD"` 的记录。
其他币种的记录（用户经 `ExpenseBatchUpdate.vue` 的币种选择器手工录入）
MUST NOT 被本工具删除或修改 —— `expense_records` 按
`(family_id, expense_period, minor_category_id, currency)` 去重，
同一小类的不同币种是彼此独立的行。

#### Scenario: 修正规则后清除旧值

- **WHEN** 首次运行因规则错误给「娱乐/娱乐健身」写入 `$500`
- **AND** 用户修正 `rules.toml` 后重跑，该小类本次聚合结果为空
- **THEN** 远端该小类的 USD 记录被 `DELETE` 清除
- **AND** 库中不残留 `$500`

#### Scenario: 不触碰其他币种记录

- **WHEN** 远端 2026-08 的「食/饮食」同时存在一条 `USD` 记录和一条用户手录的 `CNY` 记录
- **AND** 本次聚合结果中「食/饮食」有新的 USD 金额
- **THEN** USD 记录被更新
- **AND** CNY 记录既不被更新也不被删除

#### Scenario: 全部小类都消失时不误删他币种

- **WHEN** 本次聚合结果为空（例如目录中只有转账类交易）
- **AND** 远端该期间存在若干 `CNY` 记录
- **THEN** 系统不删除任何 `CNY` 记录
- **AND** 仅清除本工具此前写入的 `USD` 记录

### Requirement: 重跑幂等

系统 SHALL 保证同一目录连续执行两次写入后，数据库状态一致，且不产生重复行。

#### Scenario: 连跑两次

- **WHEN** 对同一目录连续执行两次写入命令
- **THEN** 第二次执行后 `expense_records` 中该期间的记录与第一次完全相同
- **AND** 每个 `(expense_period, minor_category_id, currency)` 组合仍只有一行

### Requirement: 凭据与鉴权

系统 SHALL 从环境变量 `FINANCE_API_BASE`、`FINANCE_USERNAME`、`FINANCE_PASSWORD`
读取连接信息，登录换取 JWT 后调用 API。凭据 MUST NOT 出现在仓库中。

`family_id` MUST 在批量写入请求中携带。虽然 controller 会用
`AuthHelper.getFamilyIdFromAuth` 覆盖它，但 `@Valid` 在方法体之前执行，
而该字段带 `@NotNull` —— 省略它会在校验阶段被拒（HTTP 400），覆盖逻辑根本没机会运行。
客户端 SHALL 通过 `GET /api/families/default` 取得该值，
该端点使用同一个 `getFamilyIdFromAuth`，因此发送值与服务端替换值必然一致。

#### Scenario: 凭据缺失

- **WHEN** 执行写入命令但未设置 `FINANCE_PASSWORD`
- **THEN** 系统报错指明缺少哪个环境变量
- **AND** 不尝试写入、不静默跳过写入步骤

#### Scenario: 登录失败

- **WHEN** 凭据错误导致登录返回非成功响应
- **THEN** 系统报告认证失败并退出，退出码非 0
- **AND** 不进行任何写入或删除

#### Scenario: 携带 family id

- **WHEN** 客户端发起批量写入
- **THEN** 请求体含 `familyId`，取自 `GET /api/families/default`
- **AND** 未取得该值时拒绝发起写入，而非发送空值触发 400

#### Scenario: 默认不写库

- **WHEN** 用户未加写入开关执行命令
- **THEN** 系统只生成 `review.csv` 与聚合预览，不发起任何 API 调用
- **AND** 无需设置凭据即可完成分类核对

### Requirement: 金融数据不进仓库

CSV 原始文件 SHALL 存放于仓库外的目录（默认 `~/finance-data/`），路径可配置。
仓库中 MUST NOT 出现任何含账号、余额或商户明细的文件。
本地产出物（`review.csv` 等）MUST 被 `.gitignore` 排除。

#### Scenario: 产出物不被提交

- **WHEN** 工具生成 `review.csv` 与聚合预览到输出目录
- **THEN** 这些路径已被 `.gitignore` 覆盖
- **AND** `git status` 中不出现它们
