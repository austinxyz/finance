# API 文档

## 基础信息

**基础路径:** `http://localhost:8080/api`

**认证方式:** 暂无（开发中）

**数据格式:** JSON

## 资产负债管理

### 资产管理

**基础路径:** `/api/assets`

- `GET /assets` - 获取资产列表
- `GET /assets/{id}` - 获取资产详情
- `POST /assets` - 创建资产记录
- `PUT /assets/{id}` - 更新资产记录
- `DELETE /assets/{id}` - 删除资产记录
- `POST /assets/batch` - 批量保存资产记录

**支持的资产类型:**
- CASH - 现金
- STOCK - 股票
- RETIREMENT - 退休基金
- INSURANCE - 保险
- REAL_ESTATE - 房产
- CRYPTO - 数字货币
- PRECIOUS_METAL - 贵金属
- OTHER - 其他

### 负债管理

**基础路径:** `/api/liabilities`

- `GET /liabilities` - 获取负债列表
- `GET /liabilities/{id}` - 获取负债详情
- `POST /liabilities` - 创建负债记录
- `PUT /liabilities/{id}` - 更新负债记录
- `DELETE /liabilities/{id}` - 删除负债记录
- `POST /liabilities/batch` - 批量保存负债记录

**支持的负债类型:**
- MORTGAGE - 房贷
- AUTO_LOAN - 车贷
- CREDIT_CARD - 信用卡
- PERSONAL_LOAN - 个人借款
- STUDENT_LOAN - 学生贷款
- BUSINESS_LOAN - 商业贷款
- OTHER - 其他

### 数据分析

**基础路径:** `/api/analysis`

- `GET /analysis/allocation` - 资产配置分析
- `GET /analysis/trends` - 趋势分析
- `GET /analysis/tax-status` - 税收状态分析
- `GET /analysis/net-worth` - 净资产计算

## 支出管理

### 支出分类

**基础路径:** `/api/expenses/categories`

- `GET /expenses/categories` - 获取所有分类（大类和小类）
- `GET /expenses/categories/major` - 获取大类列表
- `GET /expenses/categories/minor` - 获取小类列表
- `POST /expenses/categories/minor` - 创建子分类
- `PUT /expenses/categories/minor/{id}` - 更新子分类
- `PUT /expenses/categories/minor/{id}/disable` - 禁用子分类

**预设大类（10个）:**
- 子女 👶 (CHILDREN)
- 衣 👔 (CLOTHING)
- 食 🍜 (FOOD)
- 住 🏠 (HOUSING)
- 行 🚗 (TRANSPORTATION)
- 保险 🛡️ (INSURANCE)
- 人情 🎁 (SOCIAL)
- 娱乐 🎮 (ENTERTAINMENT)
- 医疗 ⚕️ (MEDICAL)
- 其他 📦 (OTHER)

### 支出记录

**基础路径:** `/api/expenses/records`

- `GET /expenses/records` - 获取支出记录列表
- `GET /expenses/records/{id}` - 获取支出记录详情
- `POST /expenses/records` - 创建支出记录
- `PUT /expenses/records/{id}` - 更新支出记录
- `DELETE /expenses/records/{id}` - 删除支出记录
- `POST /expenses/records/batch` - 批量保存支出记录
- `GET /expenses/records/range` - 按时间范围查询记录

**查询参数示例:**
```
GET /expenses/records/range?familyId=1&startDate=2024-01-01&endDate=2024-12-31&currency=USD
```

### 预算管理

**基础路径:** `/api/expense-budgets`

- `GET /expense-budgets` - 获取预算列表
- `GET /expense-budgets/{id}` - 获取预算详情
- `POST /expense-budgets` - 创建预算
- `PUT /expense-budgets/{id}` - 更新预算
- `DELETE /expense-budgets/{id}` - 删除预算
- `POST /expense-budgets/batch` - 批量保存预算

### 支出分析

**基础路径:** `/api/expenses/analysis`

#### 年度支出分析

**1. 年度大类汇总**
```
GET /expenses/analysis/annual/major-categories
```
参数:
- `familyId` - 家庭ID（必填）
- `year` - 年份（必填）
- `currency` - 货币代码（可选，默认USD）

返回示例:
```json
{
  "success": true,
  "data": [
    {
      "majorCategoryId": 4,
      "majorCategoryName": "住",
      "majorCategoryIcon": "🏠",
      "majorCategoryCode": "HOUSING",
      "totalAmount": 57984.70,
      "currency": "USD"
    }
  ]
}
```

**2. 年度小类汇总**
```
GET /expenses/analysis/annual/minor-categories
```
参数:
- `familyId` - 家庭ID（必填）
- `year` - 年份（必填）
- `majorCategoryId` - 大类ID（必填）
- `currency` - 货币代码（可选，默认USD）

**3. 年度月度趋势**
```
GET /expenses/analysis/annual/monthly-trend
```
参数:
- `familyId` - 家庭ID（必填）
- `year` - 年份（必填）
- `majorCategoryId` - 大类ID（可选，不传则查所有）
- `minorCategoryId` - 小类ID（可选）
- `currency` - 货币代码（可选，默认USD）

**4. 年度支出汇总（含资产负债调整）**
```
GET /expenses/analysis/annual/summary
```
参数:
- `familyId` - 家庭ID（必填）
- `year` - 年份（必填）
- `currency` - 货币代码（可选，默认USD）
- `includeTotals` - 是否包含总计行（可选，默认true）

返回示例:
```json
{
  "success": true,
  "data": [
    {
      "summaryYear": 2025,
      "majorCategoryId": 4,
      "majorCategoryName": "住",
      "majorCategoryCode": "HOUSING",
      "baseExpenseAmount": 76914.71,
      "assetAdjustment": 0.00,
      "liabilityAdjustment": 28609.61,
      "actualExpenseAmount": 48305.10,
      "currency": "USD",
      "adjustmentDetails": "[...]"
    },
    {
      "majorCategoryId": 0,
      "majorCategoryName": "总计",
      "majorCategoryCode": "TOTAL",
      "baseExpenseAmount": 197632.36,
      "assetAdjustment": 54191.71,
      "liabilityAdjustment": 28609.61,
      "actualExpenseAmount": 114831.04,
      "currency": "USD"
    }
  ]
}
```

**5. 预算执行分析**
```
GET /expenses/analysis/budget-execution
```
参数:
- `familyId` - 家庭ID（必填）
- `year` - 年份（必填）
- `currency` - 货币代码（可选，默认USD）

## 其他管理

### 家庭管理

**基础路径:** `/api/family`

- `GET /family` - 获取家庭列表
- `GET /family/{id}` - 获取家庭详情
- `POST /family` - 创建家庭
- `PUT /family/{id}` - 更新家庭
- `DELETE /family/{id}` - 删除家庭

### 汇率管理

**基础路径:** `/api/exchange-rates`

- `GET /exchange-rates` - 获取汇率列表
- `GET /exchange-rates/latest` - 获取最新汇率
- `POST /exchange-rates` - 创建汇率记录
- `PUT /exchange-rates/{id}` - 更新汇率记录

**支持的货币:**
- USD - 美元
- CNY - 人民币
- EUR - 欧元
- GBP - 英镑
- JPY - 日元
- AUD - 澳元
- CAD - 加元

### 年度财务汇总

**基础路径:** `/api/annual-summary`

- `GET /annual-summary` - 获取年度财务汇总
- `POST /annual-summary` - 创建年度汇总
- `PUT /annual-summary/{id}` - 更新年度汇总

## 错误码

| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 响应格式

### 成功响应
```json
{
  "success": true,
  "data": { ... }
}
```

### 错误响应
```json
{
  "success": false,
  "message": "错误信息"
}
```
