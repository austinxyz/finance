# 年度汇总分类改进 - 按TYPE分组

## 问题描述

之前的年度汇总数据按照具体的分类名称（name）进行分组,导致:
1. 同一TYPE下的子分类被分开统计（例如"IRA"、"401k"都是RETIREMENT_FUND类型）
2. 与趋势分析页面的分类不一致（趋势分析按TYPE显示）
3. 缺少净资产分类明细

## 解决方案

### 1. 数据库修改

#### 添加新字段
```sql
ALTER TABLE annual_financial_summary
ADD COLUMN net_asset_breakdown JSON COMMENT '净资产分类明细（按净资产分类）' AFTER liability_breakdown;
```

### 2. 存储过程修改

文件: `/database/create_annual_summary_by_type.sql`

主要改动:
- 将资产分类汇总从 `GROUP BY ac.id, ac.name` 改为 `GROUP BY ac.type`
- 将负债分类汇总从 `GROUP BY lc.id, lc.name` 改为 `GROUP BY lc.type`
- 添加净资产分类明细计算逻辑:
  - `REAL_ESTATE_NET`: 房地产净值 = REAL_ESTATE资产 - MORTGAGE负债
  - `RETIREMENT_FUND_NET`: 退休基金净值 = RETIREMENT_FUND资产
  - `LIQUID_NET`: 流动资产净值 = CASH资产 - CREDIT_CARD负债
  - `INVESTMENT_NET`: 投资净值 = (STOCKS + PRECIOUS_METALS + CRYPTOCURRENCY) - (PERSONAL_LOAN + STUDENT_LOAN)
  - `OTHER_NET`: 其他净值 = (INSURANCE + OTHER资产) - (AUTO_LOAN + OTHER负债)

### 3. 后端修改

#### Model层 (AnnualFinancialSummary.java)
```java
@JdbcTypeCode(SqlTypes.JSON)
@Column(name = "net_asset_breakdown", columnDefinition = "JSON")
private Map<String, BigDecimal> netAssetBreakdown;
```

#### DTO层 (AnnualFinancialSummaryDTO.java)
```java
private Map<String, BigDecimal> netAssetBreakdown;
```

#### Service层 (AnnualFinancialSummaryService.java)
- 在 `convertToDTO()` 方法中添加:`dto.setNetAssetBreakdown(entity.getNetAssetBreakdown())`
- 在 `updateEntityFromDTO()` 方法中添加:`entity.setNetAssetBreakdown(dto.getNetAssetBreakdown())`

### 4. 前端修改

文件: `/frontend/src/views/AnnualTrend.vue`

#### 添加分类名称映射函数
```javascript
const getCategoryDisplayName = (categoryType) => {
  const categoryNames = {
    // 资产分类
    'CASH': '现金类',
    'STOCKS': '股票投资',
    'RETIREMENT_FUND': '退休基金',
    'INSURANCE': '保险',
    'REAL_ESTATE': '房地产',
    'CRYPTOCURRENCY': '数字货币',
    'PRECIOUS_METALS': '贵金属',
    'OTHER': '其他',
    // 负债分类
    'MORTGAGE': '房贷',
    'AUTO_LOAN': '车贷',
    'CREDIT_CARD': '信用卡',
    'PERSONAL_LOAN': '个人贷款',
    'STUDENT_LOAN': '学生贷款',
    // 净资产分类
    'REAL_ESTATE_NET': '房地产净值',
    'RETIREMENT_FUND_NET': '退休基金净值',
    'LIQUID_NET': '流动资产净值',
    'INVESTMENT_NET': '投资净值',
    'OTHER_NET': '其他净值'
  }
  return categoryNames[categoryType] || categoryType
}
```

#### 更新显示逻辑
- 资产分类:使用 `getCategoryDisplayName(category)` 显示分类名称
- 负债分类:使用 `getCategoryDisplayName(category)` 显示分类名称
- 净资产分类:使用后端返回的 `summary.netAssetBreakdown` 数据

## 分类对应关系

### 资产分类 (asset_categories.type)
- CASH - 现金类（银行存款、货币基金等）
- STOCKS - 股票投资（国内外股票、基金、ETF、债券）
- RETIREMENT_FUND - 退休基金（401k、IRA、养老保险）
- INSURANCE - 保险（人寿保险、年金保险）
- REAL_ESTATE - 房地产（自住房、投资房、商铺、车位）
- CRYPTOCURRENCY - 数字货币（比特币、以太坊、稳定币）
- PRECIOUS_METALS - 贵金属（黄金、白银）
- OTHER - 其他（汽车、收藏品、珠宝、应收账款）

### 负债分类 (liability_categories.type)
- MORTGAGE - 房贷
- AUTO_LOAN - 车贷
- CREDIT_CARD - 信用卡
- PERSONAL_LOAN - 个人贷款
- STUDENT_LOAN - 学生贷款
- OTHER - 其他负债

### 净资产分类 (net_asset_categories.code)
- REAL_ESTATE_NET - 房地产净值
- RETIREMENT_FUND_NET - 退休基金净值
- LIQUID_NET - 流动资产净值
- INVESTMENT_NET - 投资净值
- OTHER_NET - 其他净值

## 数据对比

### 修改前（按name分组）
```json
{
  "assetBreakdown": {
    "IRA": 14000.00,
    "401k": 211872.32,
    "房产": 1301500.00,
    "自住房产": 1800000.00,
    "现金": 9590.00,
    "银行存款": 173718.76,
    "股票": 414103.48
  }
}
```

### 修改后（按type分组）
```json
{
  "assetBreakdown": {
    "CASH": 261111.27,
    "STOCKS": 414103.48,
    "RETIREMENT_FUND": 225872.32,
    "INSURANCE": 21920.00,
    "REAL_ESTATE": 3101500.00,
    "CRYPTOCURRENCY": 0.00,
    "OTHER": 0.00
  },
  "netAssetBreakdown": {
    "REAL_ESTATE_NET": 2183159.65,
    "RETIREMENT_FUND_NET": 225872.32,
    "LIQUID_NET": 260611.27,
    "INVESTMENT_NET": 414103.48,
    "OTHER_NET": -21920.0
  }
}
```

## 执行步骤

1. 添加net_asset_breakdown字段
```bash
/opt/homebrew/opt/mysql-client/bin/mysql -h 10.0.0.7 -P 37719 -u austinxu -phelloworld finance < /Users/yanzxu/claude/finance/database/add_net_asset_breakdown.sql
```

2. 更新存储过程
```bash
/opt/homebrew/opt/mysql-client/bin/mysql -h 10.0.0.7 -P 37719 -u austinxu -phelloworld finance < /Users/yanzxu/claude/finance/database/create_annual_summary_by_type.sql
```

3. 重新计算年度汇总（通过前端"刷新数据"按钮或手动调用存储过程）
```sql
CALL sp_calculate_annual_summary(1, 2024);
CALL sp_calculate_annual_summary(1, 2025);
```

## 优点

1. ✅ 与趋势分析页面的分类保持一致
2. ✅ 同一类型的子分类自动合并统计
3. ✅ 添加了净资产分类明细,更清晰地展示各类净资产构成
4. ✅ 数据来源统一（都从数据库分类表获取）
5. ✅ 易于维护和扩展

## 验证数据

验证存储过程和数据是否正确:
```bash
# 检查net_asset_breakdown字段是否存在
/opt/homebrew/opt/mysql-client/bin/mysql -h 10.0.0.7 -P 37719 -u austinxu -phelloworld finance -e "DESCRIBE annual_financial_summary;"

# 查看最近3年的数据
/opt/homebrew/opt/mysql-client/bin/mysql -h 10.0.0.7 -P 37719 -u austinxu -phelloworld finance -e "SELECT year, asset_breakdown, liability_breakdown, net_asset_breakdown FROM annual_financial_summary WHERE family_id = 1 ORDER BY year DESC LIMIT 3;"

# 手动触发存储过程
/opt/homebrew/opt/mysql-client/bin/mysql -h 10.0.0.7 -P 37719 -u austinxu -phelloworld finance -e "CALL sp_calculate_annual_summary(1, 2024);"
```

## 故障排查

### 问题1: 点击"刷新数据"按钮失败

**可能原因:**
1. 后端服务未启动
2. 前端与后端连接失败
3. 存储过程执行出错

**排查步骤:**

1. 检查后端是否运行:
```bash
# 检查后端日志（在finance目录下）
cd ~/claude/finance
# 后端应该在 http://localhost:8080/api 运行
curl http://localhost:8080/api/annual-summary/family/1/recent?limit=5
```

2. 检查浏览器控制台:
- 打开浏览器开发者工具（F12）
- 查看Console标签是否有错误信息
- 查看Network标签,查看API请求是否成功
- 检查 `/api/annual-summary/family/1/batch-calculate` 请求的响应

3. 检查前端开发服务器:
```bash
# 前端应该在 http://localhost:3000 运行
# 检查是否有代理错误:ECONNREFUSED
```

4. 手动测试API:
```bash
# 测试获取年度汇总API
curl http://localhost:8080/api/annual-summary/family/1/recent?limit=5

# 测试计算单个年份
curl -X POST http://localhost:8080/api/annual-summary/family/1/calculate/2024

# 测试批量计算
curl -X POST http://localhost:8080/api/annual-summary/family/1/batch-calculate \
  -H "Content-Type: application/json" \
  -d '[2024,2025]'
```

### 问题2: 数据显示不正确

**检查数据库数据:**
```sql
-- 查看特定年份的详细数据
SELECT * FROM annual_financial_summary
WHERE family_id = 1 AND year = 2024\G

-- 检查asset_breakdown是否按TYPE分组
SELECT year, JSON_KEYS(asset_breakdown) as asset_types
FROM annual_financial_summary
WHERE family_id = 1
ORDER BY year DESC;
```

**预期结果:**
- asset_breakdown应包含: CASH, STOCKS, RETIREMENT_FUND, INSURANCE, REAL_ESTATE, CRYPTOCURRENCY, PRECIOUS_METALS, OTHER
- liability_breakdown应包含: MORTGAGE, AUTO_LOAN, CREDIT_CARD, PERSONAL_LOAN, STUDENT_LOAN, OTHER
- net_asset_breakdown应包含: REAL_ESTATE_NET, RETIREMENT_FUND_NET, LIQUID_NET, INVESTMENT_NET, OTHER_NET
