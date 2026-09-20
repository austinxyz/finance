# Contract — group 1: 配置、目录发现与期间归属

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
