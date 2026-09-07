# Contract — group 4: 解析器：Chase 信用卡与 BOA checking

- **Spec**:
  - 系统 SHALL 保证同一笔真实消费只被计入一次。信用卡消费 MUST 只从信用卡账单计入，对应的 checking 还款 MUST 判为 `TRANSFER`。
  - 系统 SHALL 从 `~/finance-data/<YYYY-MM>/` 目录读取当月所有 CSV 文件，按文件表头自动识别所属机构。
- **Runtime**: `python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_parsers_*.py"` → expected: 两个解析器的真实样本行用例全绿，金额符号断言通过
- **Code**:
  - **符号约定是最容易犯且后果最大的解析错误**：信用卡账单中消费通常为正数，与 checking 相反。每个解析器在 `parse_row` 内归一为「负数 = 支出」，并用真实样本行断言符号。搞反会让整月开支变成负数。
  - D5 — 通过 `header_signature` 声明表头特征供自动识别；新增机构 = 新增模块 + 注册，管道其余部分不动。
  - 跨账户去重靠规则而非解析器：checking 里的信用卡还款由结构性规则判为 `TRANSFER`，且该规则必须排在所有商户规则之前。
- **Threshold**: 80
