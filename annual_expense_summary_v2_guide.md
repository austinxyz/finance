# 年度支出汇总系统 V2 - 可配置抽象版本

## 一、设计理念

V2版本将"住房-房贷"、"保险-保险资产"的硬编码关系抽象为可配置的映射关系，使系统更加灵活和可扩展。

### 核心改进

1. **配置化**: 支出类别与资产/负债的关系存储在配置表中，不再硬编码
2. **抽象化**: 通过通用的调整类型(ASSET/LIABILITY)和调整方向(ADD/SUBTRACT)处理所有情况
3. **可扩展**: 轻松添加新的调整规则，无需修改存储过程

## 二、数据模型

### 2.1 配置表 (`expense_category_adjustment_config`)

```sql
id                      -- 主键
major_category_id       -- 支出大类ID
adjustment_type         -- 调整类型: 'ASSET' 或 'LIABILITY'
asset_type_code         -- 资产类型代码 (当adjustment_type='ASSET')
liability_type          -- 负债类型 (当adjustment_type='LIABILITY')
adjustment_direction    -- 调整方向: 'ADD' 或 'SUBTRACT'
description             -- 调整说明
is_active               -- 是否启用
```

### 2.2 汇总表 (`annual_expense_summary`)

```sql
id                      -- 主键
family_id               -- 家庭ID
summary_year            -- 汇总年份
major_category_id       -- 大类ID (0表示总计)
minor_category_id       -- 小类ID (NULL表示大类汇总)
base_expense_amount     -- 基础支出
asset_adjustment        -- 资产调整额
liability_adjustment    -- 负债调整额
adjustment_details      -- 调整详情 (JSON)
actual_expense_amount   -- 实际年度支出
```

## 三、配置规则

### 3.1 调整类型 (`adjustment_type`)

| 类型 | 说明 | 示例 |
|------|------|------|
| ASSET | 关联资产类型 | 保险支出关联保险资产 |
| LIABILITY | 关联负债类型 | 住房支出关联房贷 |

### 3.2 调整方向 (`adjustment_direction`)

| 方向 | 公式 | 适用场景 |
|------|------|----------|
| ADD | 实际支出 = 基础支出 + 调整值 | 负债本金偿还应计入支出 (如房贷、车贷) |
| SUBTRACT | 实际支出 = 基础支出 - 调整值 | 资产增值应扣除 (如保险现金价值增长) |

### 3.3 计算逻辑

**资产调整值计算**:
```
调整值 = 今年12月31日资产价值 - 去年12月31日资产价值
```

**负债调整值计算**:
```
调整值 = 去年12月31日负债余额 - 今年12月31日负债余额
```

## 四、默认配置

系统预置两个默认配置：

### 配置1: 住房支出 + 房贷减少

```sql
支出大类: HOUSING (住)
调整类型: LIABILITY
负债类型: MORTGAGE
调整方向: ADD
说明: 房贷本金偿还计入住房支出
```

**计算示例**:
```
去年底房贷: 1,000,000元
今年底房贷:   950,000元
房贷减少:      50,000元  (正值, 表示负债减少)

基础支出(水电煤物业): 24,000元
实际住房支出: 24,000 + 50,000 = 74,000元
```

### 配置2: 保险支出 - 保险资产增加

```sql
支出大类: INSURANCE (保险)
调整类型: ASSET
资产类型: INSURANCE
调整方向: SUBTRACT
说明: 保险资产增值扣除
```

**计算示例**:
```
去年底保险资产: 70,000元
今年底保险资产: 76,000元
保险资产增加:     6,000元  (正值, 表示资产增加)

基础支出(支付保费): 12,000元
实际保险支出: 12,000 - 6,000 = 6,000元
```

## 五、使用指南

### 5.1 安装

```bash
/opt/homebrew/opt/mysql-client/bin/mysql \
  -h 10.0.0.7 -P 37719 \
  -u austinxu -phelloworld finance \
  < /tmp/create_annual_expense_summary_v2.sql
```

### 5.2 查看当前配置

```sql
SELECT
    major.name AS '支出大类',
    c.adjustment_type AS '调整类型',
    COALESCE(c.asset_type_code, c.liability_type) AS '关联代码',
    c.adjustment_direction AS '调整方向',
    c.description AS '说明',
    c.is_active AS '启用状态'
FROM expense_category_adjustment_config c
LEFT JOIN expense_categories_major major ON c.major_category_id = major.id
ORDER BY c.is_active DESC, major.name;
```

**查询结果示例**:
```
支出大类 | 调整类型 | 关联代码 | 调整方向 | 说明 | 启用状态
住       | LIABILITY | MORTGAGE | ADD      | 房贷本金偿还计入住房支出 | 1
保险     | ASSET     | INSURANCE| SUBTRACT | 保险资产增值扣除 | 1
```

### 5.3 添加新配置

#### 示例1: 车贷计入交通支出

```sql
INSERT INTO expense_category_adjustment_config
    (major_category_id, adjustment_type, liability_type, adjustment_direction, description)
SELECT
    id,
    'LIABILITY',
    'AUTO_LOAN',
    'ADD',
    '车贷本金偿还计入交通支出: 实际支出 = 月度交通费 + 车贷减少额'
FROM expense_categories_major
WHERE code = 'TRANSPORTATION';
```

#### 示例2: 股票投资收益扣除娱乐支出

假设有股票资产类型 'STOCKS'，希望股票增值扣除娱乐支出:

```sql
INSERT INTO expense_category_adjustment_config
    (major_category_id, adjustment_type, asset_type_code, adjustment_direction, description)
SELECT
    id,
    'ASSET',
    'STOCKS',
    'SUBTRACT',
    '股票资产增值扣除: 实际支出 = 月度娱乐费 - 股票增值'
FROM expense_categories_major
WHERE code = 'ENTERTAINMENT';
```

### 5.4 禁用/启用配置

```sql
-- 禁用保险调整
UPDATE expense_category_adjustment_config
SET is_active = 0
WHERE major_category_id = (SELECT id FROM expense_categories_major WHERE code = 'INSURANCE')
  AND adjustment_type = 'ASSET';

-- 启用保险调整
UPDATE expense_category_adjustment_config
SET is_active = 1
WHERE major_category_id = (SELECT id FROM expense_categories_major WHERE code = 'INSURANCE')
  AND adjustment_type = 'ASSET';
```

### 5.5 删除配置

```sql
-- 删除车贷配置
DELETE FROM expense_category_adjustment_config
WHERE major_category_id = (SELECT id FROM expense_categories_major WHERE code = 'TRANSPORTATION')
  AND liability_type = 'AUTO_LOAN';
```

### 5.6 调用存储过程计算汇总

```sql
-- 计算2024年度汇总
CALL calculate_annual_expense_summary_v2(1, 2024);

-- 计算2025年度汇总
CALL calculate_annual_expense_summary_v2(1, 2025);
```

### 5.7 查询汇总结果

```sql
-- 查看大类汇总
SELECT
    CASE WHEN major_category_id = 0 THEN '【总计】' ELSE major.name END AS '类别',
    base_expense_amount AS '基础支出',
    asset_adjustment AS '资产调整',
    liability_adjustment AS '负债调整',
    actual_expense_amount AS '实际支出',
    adjustment_details AS '调整详情'
FROM annual_expense_summary aes
LEFT JOIN expense_categories_major major ON aes.major_category_id = major.id
WHERE family_id = 1
  AND summary_year = 2024
  AND minor_category_id IS NULL
ORDER BY
    CASE WHEN major_category_id = 0 THEN 999 ELSE major_category_id END;
```

**查询结果示例**:
```
类别 | 基础支出 | 资产调整 | 负债调整 | 实际支出 | 调整详情
住   | 24,000  | 0       | 50,000  | 74,000  | [{"type":"LIABILITY","code":"MORTGAGE","amount":50000,"direction":"ADD"}]
保险 | 12,000  | 6,000   | 0       | 6,000   | [{"type":"ASSET","code":"INSURANCE","amount":6000,"direction":"SUBTRACT"}]
【总计】| 254,000 | 6,000  | 50,000  | 298,000 | null
```

## 六、配置场景示例

### 场景1: 401K/养老金投入扣除

如果401K投入算作退休金支出，但资产也在增长，应扣除增值部分:

```sql
INSERT INTO expense_category_adjustment_config
    (major_category_id, adjustment_type, asset_type_code, adjustment_direction, description)
SELECT
    21,  -- 假设退休类别ID为21
    'ASSET',
    'RETIREMENT_FUND',
    'SUBTRACT',
    '401K资产增值扣除: 实际支出 = 月度缴纳额 - 资产增值'
WHERE NOT EXISTS (
    SELECT 1 FROM expense_category_adjustment_config
    WHERE adjustment_type = 'ASSET' AND asset_type_code = 'RETIREMENT_FUND'
);
```

### 场景2: 学生贷款计入子女教育支出

```sql
INSERT INTO expense_category_adjustment_config
    (major_category_id, adjustment_type, liability_type, adjustment_direction, description)
SELECT
    id,
    'LIABILITY',
    'STUDENT_LOAN',
    'ADD',
    '学生贷款本金偿还计入子女支出'
FROM expense_categories_major
WHERE code = 'CHILDREN';
```

### 场景3: 房地产增值抵消经营支出

如果有商业地产，增值应扣除经营支出:

```sql
INSERT INTO expense_category_adjustment_config
    (major_category_id, adjustment_type, asset_type_code, adjustment_direction, description)
SELECT
    id,
    'ASSET',
    'REAL_ESTATE',
    'SUBTRACT',
    '商业地产增值扣除经营支出'
FROM expense_categories_major
WHERE code = 'BUSINESS';
```

## 七、架构优势

### 7.1 vs 硬编码版本

| 特性 | V1 (硬编码) | V2 (配置化) |
|------|-------------|-------------|
| 灵活性 | 低 - 修改需改代码 | 高 - 通过SQL配置 |
| 可维护性 | 差 - 逻辑分散在代码 | 好 - 集中在配置表 |
| 可扩展性 | 低 - 每个新规则需改存储过程 | 高 - 新增配置记录即可 |
| 透明度 | 差 - 业务逻辑隐藏在代码 | 好 - 配置一目了然 |
| 测试性 | 困难 - 需重新部署 | 简单 - 修改配置即可 |

### 7.2 数据一致性

- 所有调整逻辑统一通过配置表管理
- 调整详情存储在JSON字段，可追溯
- 支持启用/禁用功能，便于A/B测试

### 7.3 业务适应性

- 不同家庭可配置不同规则 (扩展: 添加family_id到配置表)
- 不同年份可应用不同规则 (扩展: 添加effective_year到配置表)
- 支持多种资产/负债类型组合

## 八、未来扩展方向

### 8.1 支持多条件配置

为配置表添加生效条件:

```sql
ALTER TABLE expense_category_adjustment_config
ADD COLUMN effective_from_year INT COMMENT '生效起始年份',
ADD COLUMN effective_to_year INT COMMENT '生效结束年份',
ADD COLUMN family_id BIGINT COMMENT '特定家庭 (NULL表示全部)';
```

### 8.2 支持复合调整

一个支出类别可关联多个资产/负债:

```sql
-- 住房支出同时关联房贷和房产税资产
INSERT INTO expense_category_adjustment_config ...
-- 保险支出同时关联人寿保险和养老保险资产
INSERT INTO expense_category_adjustment_config ...
```

### 8.3 前端配置界面

提供可视化界面管理配置:

```typescript
interface AdjustmentConfig {
  majorCategoryId: number;
  adjustmentType: 'ASSET' | 'LIABILITY';
  code: string;  // asset_type_code or liability_type
  direction: 'ADD' | 'SUBTRACT';
  description: string;
  isActive: boolean;
}

// API接口
POST /api/expense/adjustment-config
GET /api/expense/adjustment-config
PUT /api/expense/adjustment-config/:id
DELETE /api/expense/adjustment-config/:id
```

## 九、迁移指南

### 从V1迁移到V2

1. 备份V1数据
2. 执行V2脚本创建新表
3. 数据迁移 (如果V1已有数据):

```sql
-- 迁移V1汇总数据到V2格式
INSERT INTO annual_expense_summary (
    family_id, user_id, summary_year,
    major_category_id, minor_category_id,
    base_expense_amount,
    asset_adjustment,
    liability_adjustment,
    actual_expense_amount
)
SELECT
    family_id, user_id, summary_year,
    major_category_id, minor_category_id,
    base_expense_amount,
    COALESCE(-insurance_asset_increase, 0),
    COALESCE(mortgage_decrease, 0),
    actual_expense_amount
FROM old_annual_expense_summary;  -- V1表名
```

4. 验证数据一致性
5. 重新计算汇总验证正确性

## 十、常见问题

### Q1: 如何处理同一类别的多个调整?

目前存储过程每个类别只处理一个调整。如需多个，需修改存储过程支持循环或使用JSON数组合并。

### Q2: 调整详情JSON格式是什么?

```json
[
  {
    "type": "LIABILITY",
    "code": "MORTGAGE",
    "amount": 50000,
    "direction": "ADD"
  }
]
```

### Q3: 如何禁用所有调整?

```sql
UPDATE expense_category_adjustment_config SET is_active = 0;
```

### Q4: 配置表支持并发修改吗?

配置表的修改不会影响已计算的汇总数据。重新调用存储过程会使用最新配置。
