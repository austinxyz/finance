# Contract — group 2: 三道写入闸门

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
