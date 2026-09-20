# Contract — group 5: 解析器：PayPal、Venmo 与 Robinhood

- **Spec**:
  - 经 PayPal / Venmo 支付的消费 MUST 只从该平台账单计入，对应的 checking 充值 MUST 判为 `FUNDING`。
  - 标记为 `manual` 的来源 MUST 每月提示用户手工补录，且 MUST NOT 被当作已覆盖。
- **Runtime**: `python -m unittest discover -s tools/expense-import -t tools/expense-import -p "test_parsers_*.py"` → expected: 已实现平台的样本行用例全绿；Robinhood 若标记 `manual` 则该来源无解析器测试但有 `sources.toml` 配置断言
- **Code**:
  - Venmo 是本 change 规则设计最不确定的一块：既有朋友间转账（可能属「人情」，也可能只是代付后被还款），也有商户消费。需按样本决定是按对方类型区分，还是做「代付-还款」配对抵消。
  - 与闸门 2 的衔接：PayPal/Venmo 解析器落地后，checking 中对应的 `FUNDING` 交易才有配对来源，闸门 2 才会放行。
  - Robinhood 若确认无 CSV 导出，在 `sources.toml` 标记 `manual` 并跳过解析器 —— 关键是每一家都被明确覆盖或明确标记，不能有沉默的空白。
- **Threshold**: 80
