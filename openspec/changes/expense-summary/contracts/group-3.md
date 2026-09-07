# Contract — group 3: 对账式写库

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
