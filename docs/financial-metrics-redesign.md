# 财务指标页面重构设计方案

> **创建日期**: 2026-01-06
> **状态**: 设计中
> **目标**: 整合资产、负债、收入、支出、投资等全维度财务数据，提供更完善的财务分析和健康度评估

---

## 一、现状分析

### 当前实现 (`FinancialMetrics.vue`)

**已有指标**:
- ✅ 基础指标: 总资产、总负债、净资产
- ✅ 财务比率: 资产负债率、流动性比率
- ✅ 变化趋势: 月度变化、年度变化
- ✅ 健康度评估: 负债压力、流动性状况、资产增长

**数据来源** (`AnalysisService.getFinancialMetrics`):
- 资产负债数据: `AssetSummaryDTO`
- 投资回报: `InvestmentAnalysisService` (年度大类收益汇总)
- 年度支出: `ExpenseAnalysisService` (年度汇总)
- 工作收入: **反推计算** = (净资产变化) - (投资回报) + (支出)

**存在问题**:
1. ❌ 缺少直接的收入数据展示
2. ❌ 工作收入靠反推，不够直观
3. ❌ 没有储蓄率等现金流核心指标
4. ❌ 投资收益未独立展示
5. ❌ 缺乏综合评分体系

---

## 二、重构设计

### 2.1 新增指标体系

#### A. 现金流分析模块 (新增)

**核心指标**:
```typescript
// 年度现金流指标
interface CashFlowMetrics {
  // 收入侧
  totalIncome: number           // 总收入
  workIncome: number            // 工资收入 (从income_records统计Salary大类)
  investmentIncome: number      // 投资收益 (从income_records统计Investment大类)
  otherIncome: number           // 其他收入

  // 支出侧
  totalExpense: number          // 总支出
  essentialExpense: number      // 必需支出 (住房、食品、交通、医疗等)
  discretionaryExpense: number  // 可选支出 (娱乐、购物等)

  // 现金流结果
  netCashFlow: number           // 净现金流 = 总收入 - 总支出
  savingsRate: number           // 储蓄率 = 净现金流 / 总收入 * 100%
  expenseRatio: number          // 支出率 = 总支出 / 总收入 * 100%

  // 对比数据
  lastYearIncome: number        // 去年同期收入
  lastYearExpense: number       // 去年同期支出
  incomeGrowthRate: number      // 收入增长率
  expenseGrowthRate: number     // 支出增长率
}
```

**数据来源**:
- `IncomeAnalysisService.getAnnualIncomeSummary()` - 各大类年度收入
- `ExpenseAnalysisService.getAnnualExpenseSummary()` - 各大类年度支出
- 支持按家庭ID和年份筛选

#### B. 投资收益模块 (新增)

**核心指标**:
```typescript
interface InvestmentMetrics {
  totalInvested: number         // 累计投入
  currentValue: number          // 当前市值
  unrealizedGain: number        // 未实现收益
  realizedGain: number          // 已实现收益 (年度)
  totalReturn: number           // 总回报 = 未实现 + 已实现
  returnRate: number            // 收益率 = 总回报 / 累计投入 * 100%

  // 分类数据
  topCategories: Array<{        // 表现最好的前3个投资大类
    name: string
    value: number
    returnRate: number
  }>
}
```

**数据来源**:
- `InvestmentAnalysisService.getAnnualByCategory()` - 年度大类分析
- 整合已实现和未实现收益

#### C. 综合财务健康评分 (新增)

**评分体系** (总分100分):

| 维度 | 权重 | 评分标准 | 数据来源 |
|------|------|---------|---------|
| **资产负债管理** (25分) | 25% | • <30%: 优秀(25分)<br>• 30-50%: 良好(20分)<br>• 50-70%: 一般(15分)<br>• >70%: 需改善(10分) | 资产负债率 |
| **流动性管理** (20分) | 20% | • >20%: 优秀(20分)<br>• 15-20%: 良好(16分)<br>• 10-15%: 一般(12分)<br>• <10%: 不足(8分) | 流动性比率 |
| **储蓄能力** (25分) | 25% | • >30%: 优秀(25分)<br>• 20-30%: 良好(20分)<br>• 10-20%: 一般(15分)<br>• <10%: 偏低(10分) | 储蓄率 |
| **投资收益** (20分) | 20% | • >15%: 优秀(20分)<br>• 10-15%: 良好(16分)<br>• 5-10%: 一般(12分)<br>• <5%: 偏低(8分) | 投资收益率 |
| **资产增长** (10分) | 10% | • >15%: 优秀(10分)<br>• 10-15%: 良好(8分)<br>• 5-10%: 一般(6分)<br>• <5%: 偏低(4分) | 年度净资产增长率 |

**评级标准**:
- 90-100分: 优秀 (A+)
- 80-89分: 良好 (A)
- 70-79分: 中等 (B)
- 60-69分: 及格 (C)
- <60分: 需改善 (D)

```typescript
interface HealthScore {
  totalScore: number            // 总分 (0-100)
  grade: string                 // 等级 (A+, A, B, C, D)

  // 各维度得分
  scores: {
    debtManagement: number      // 资产负债管理 (0-25)
    liquidity: number           // 流动性管理 (0-20)
    savings: number             // 储蓄能力 (0-25)
    investment: number          // 投资收益 (0-20)
    growth: number              // 资产增长 (0-10)
  }

  // 改进建议
  recommendations: string[]
}
```

### 2.2 页面布局设计

```
┌─────────────────────────────────────────────────────────────┐
│ 财务指标 - 综合分析                                          │
│ [家庭选择] [日期选择]                                         │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 1. 核心财务指标卡片 (3列网格)                                 │
├─────────────┬─────────────┬─────────────────────────────────┤
│ 总资产      │ 总负债      │ 净资产                          │
│ $1,200,000  │ $300,000    │ $900,000                        │
│ ↑ +5.2%    │ ↓ -2.1%    │ ↑ +8.3%                         │
└─────────────┴─────────────┴─────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 2. 现金流分析 (新增)                                          │
├─────────────┬─────────────┬─────────────┬─────────────────┤
│ 年度总收入   │ 年度总支出   │ 净现金流    │ 储蓄率           │
│ $180,000    │ $72,000     │ +$108,000   │ 60% ⭐          │
│ ↑ +12%     │ ↑ +5%      │             │                  │
└─────────────┴─────────────┴─────────────┴─────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 3. 投资收益概览 (新增)                                        │
├─────────────┬─────────────┬─────────────┬─────────────────┤
│ 累计投入    │ 当前市值     │ 总回报      │ 收益率           │
│ $400,000    │ $520,000    │ +$120,000   │ 30.0%           │
│ 表现最佳: 股票 (+40%), 退休基金 (+25%)                       │
└─────────────┴─────────────┴─────────────┴─────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 4. 财务健康度评分 (新增)                                      │
├─────────────────────────────────────────────────────────────┤
│           综合评分: 85分 (A - 良好)                          │
│                                                              │
│ 资产负债管理  ████████████████████░░  20/25  (良好)         │
│ 流动性管理    ████████████████░░░░░░  16/20  (良好)         │
│ 储蓄能力      █████████████████████░  25/25  (优秀)         │
│ 投资收益      ████████████████░░░░░░  16/20  (良好)         │
│ 资产增长      ████████░░░░░░░░░░░░░░   8/10  (良好)         │
│                                                              │
│ 💡 改进建议:                                                 │
│   1. 流动性比率18%,建议提升至20%以上                         │
│   2. 投资收益率12%,考虑优化投资组合                          │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 5. 财务比率详情 (保留原有)                                    │
├──────────────────────────┬──────────────────────────────────┤
│ 资产负债率 25%  ✅ 优秀   │ 流动性比率 18%  ⚠️ 良好          │
└──────────────────────────┴──────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│ 6. 趋势变化 (保留原有 + 增强)                                 │
├──────────────────────────┬──────────────────────────────────┤
│ 月度变化                  │ 年度变化                          │
│ +$5,000 (+0.6%)          │ +$75,000 (+9.1%)                 │
│                          │                                   │
│ 收入: +$15,000           │ 收入: +$20,000 (+12%)            │
│ 支出: -$10,000           │ 支出: +$3,000 (+4%)              │
└──────────────────────────┴──────────────────────────────────┘
```

### 2.3 后端API设计

#### 新增/增强API

**1. 增强现有财务指标API**
```java
// AnalysisController.java
@GetMapping("/metrics")
public ApiResponse<EnhancedFinancialMetricsDTO> getEnhancedFinancialMetrics(
    @RequestParam(required = false) Long userId,
    @RequestParam(required = false) Long familyId,
    @RequestParam(required = false) LocalDate asOfDate
) {
    EnhancedFinancialMetricsDTO metrics = analysisService.getEnhancedFinancialMetrics(
        userId, familyId, asOfDate
    );
    return ApiResponse.success(metrics);
}
```

**2. EnhancedFinancialMetricsDTO结构**
```java
public class EnhancedFinancialMetricsDTO {
    // === 原有字段 (保留向后兼容) ===
    private LocalDate asOfDate;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal netWorth;
    private BigDecimal debtToAssetRatio;
    private BigDecimal liquidityRatio;
    private BigDecimal cashAmount;
    private BigDecimal monthlyChange;
    private BigDecimal monthlyChangeRate;
    private BigDecimal yearlyChange;
    private BigDecimal yearlyChangeRate;

    // === 新增: 现金流指标 ===
    private BigDecimal annualTotalIncome;      // 年度总收入
    private BigDecimal annualWorkIncome;       // 年度工资收入
    private BigDecimal annualInvestmentIncome; // 年度投资收入
    private BigDecimal annualOtherIncome;      // 年度其他收入

    private BigDecimal annualTotalExpense;     // 年度总支出
    private BigDecimal annualEssentialExpense; // 年度必需支出
    private BigDecimal annualDiscretionaryExpense; // 年度可选支出

    private BigDecimal netCashFlow;            // 净现金流
    private BigDecimal savingsRate;            // 储蓄率
    private BigDecimal expenseRatio;           // 支出率

    private BigDecimal lastYearTotalIncome;    // 去年总收入
    private BigDecimal lastYearTotalExpense;   // 去年总支出
    private BigDecimal incomeGrowthRate;       // 收入增长率
    private BigDecimal expenseGrowthRate;      // 支出增长率

    // === 新增: 投资收益指标 ===
    private BigDecimal totalInvested;          // 累计投入
    private BigDecimal currentInvestmentValue; // 投资市值
    private BigDecimal unrealizedGain;         // 未实现收益
    private BigDecimal realizedGain;           // 已实现收益
    private BigDecimal totalInvestmentReturn;  // 总投资回报
    private BigDecimal investmentReturnRate;   // 投资收益率
    private List<TopInvestmentCategory> topCategories; // 表现最佳的投资大类

    // === 新增: 综合财务健康评分 ===
    private HealthScoreDTO healthScore;

    @Data
    public static class TopInvestmentCategory {
        private String categoryName;
        private BigDecimal value;
        private BigDecimal returnRate;
    }

    @Data
    public static class HealthScoreDTO {
        private BigDecimal totalScore;         // 总分 (0-100)
        private String grade;                  // 等级 (A+, A, B, C, D)
        private ScoreBreakdown scores;         // 各维度得分
        private List<String> recommendations;  // 改进建议

        @Data
        public static class ScoreBreakdown {
            private BigDecimal debtManagement;  // 资产负债管理 (0-25)
            private BigDecimal liquidity;       // 流动性管理 (0-20)
            private BigDecimal savings;         // 储蓄能力 (0-25)
            private BigDecimal investment;      // 投资收益 (0-20)
            private BigDecimal growth;          // 资产增长 (0-10)
        }
    }
}
```

### 2.4 计算逻辑

#### A. 现金流指标计算

```java
// AnalysisService.java
private void calculateCashFlowMetrics(
    EnhancedFinancialMetricsDTO metrics,
    Long familyId,
    Integer year
) {
    // 1. 获取年度收入汇总
    List<AnnualIncomeSummaryDTO> incomeSummary =
        incomeAnalysisService.getAnnualIncomeSummary(familyId, year);

    // 提取总计行
    AnnualIncomeSummaryDTO totalIncome = incomeSummary.stream()
        .filter(item -> item.getMajorCategoryId() == 0L)
        .findFirst()
        .orElse(null);

    if (totalIncome != null) {
        metrics.setAnnualTotalIncome(totalIncome.getActualIncomeAmount());

        // 工资收入 (Salary大类, ID=1)
        metrics.setAnnualWorkIncome(
            incomeSummary.stream()
                .filter(item -> item.getMajorCategoryId() == 1L)
                .map(AnnualIncomeSummaryDTO::getActualIncomeAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO)
        );

        // 投资收入 (Investment大类, ID=3)
        metrics.setAnnualInvestmentIncome(
            incomeSummary.stream()
                .filter(item -> item.getMajorCategoryId() == 3L)
                .map(AnnualIncomeSummaryDTO::getActualIncomeAmount)
                .findFirst()
                .orElse(BigDecimal.ZERO)
        );

        // 其他收入 = 总收入 - 工资 - 投资
        metrics.setAnnualOtherIncome(
            metrics.getAnnualTotalIncome()
                .subtract(metrics.getAnnualWorkIncome())
                .subtract(metrics.getAnnualInvestmentIncome())
        );
    }

    // 2. 获取年度支出汇总 (已有)
    // 3. 计算现金流指标
    metrics.setNetCashFlow(
        metrics.getAnnualTotalIncome().subtract(metrics.getAnnualTotalExpense())
    );

    // 储蓄率 = 净现金流 / 总收入 * 100
    if (metrics.getAnnualTotalIncome().compareTo(BigDecimal.ZERO) > 0) {
        metrics.setSavingsRate(
            metrics.getNetCashFlow()
                .divide(metrics.getAnnualTotalIncome(), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
        );
    }

    // 4. 计算同比增长率
    List<AnnualIncomeSummaryDTO> lastYearIncome =
        incomeAnalysisService.getAnnualIncomeSummary(familyId, year - 1);
    // ... 计算增长率
}
```

#### B. 投资收益指标计算

```java
private void calculateInvestmentMetrics(
    EnhancedFinancialMetricsDTO metrics,
    Long familyId,
    Integer year
) {
    // 1. 获取年度投资大类分析
    List<InvestmentCategoryAnalysisDTO> categoryAnalysis =
        investmentAnalysisService.getAnnualByCategory(familyId, year, "USD");

    // 2. 汇总所有大类数据
    BigDecimal totalInvested = BigDecimal.ZERO;
    BigDecimal totalValue = BigDecimal.ZERO;
    BigDecimal totalReturns = BigDecimal.ZERO;

    for (InvestmentCategoryAnalysisDTO category : categoryAnalysis) {
        totalInvested = totalInvested.add(category.getTotalInvested());
        totalValue = totalValue.add(category.getCurrentValue());
        totalReturns = totalReturns.add(category.getReturns());
    }

    metrics.setTotalInvested(totalInvested);
    metrics.setCurrentInvestmentValue(totalValue);
    metrics.setTotalInvestmentReturn(totalReturns);

    // 收益率
    if (totalInvested.compareTo(BigDecimal.ZERO) > 0) {
        metrics.setInvestmentReturnRate(
            totalReturns.divide(totalInvested, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"))
        );
    }

    // 3. 提取表现最好的前3个大类
    List<TopInvestmentCategory> topCategories = categoryAnalysis.stream()
        .sorted((a, b) -> b.getReturnRate().compareTo(a.getReturnRate()))
        .limit(3)
        .map(category -> {
            TopInvestmentCategory top = new TopInvestmentCategory();
            top.setCategoryName(category.getCategoryName());
            top.setValue(category.getCurrentValue());
            top.setReturnRate(category.getReturnRate());
            return top;
        })
        .collect(Collectors.toList());

    metrics.setTopCategories(topCategories);
}
```

#### C. 综合财务健康评分计算

```java
private HealthScoreDTO calculateHealthScore(EnhancedFinancialMetricsDTO metrics) {
    HealthScoreDTO healthScore = new HealthScoreDTO();
    ScoreBreakdown scores = new ScoreBreakdown();
    List<String> recommendations = new ArrayList<>();

    // 1. 资产负债管理 (0-25分)
    BigDecimal debtRatio = metrics.getDebtToAssetRatio();
    if (debtRatio.compareTo(new BigDecimal("30")) < 0) {
        scores.setDebtManagement(new BigDecimal("25"));
    } else if (debtRatio.compareTo(new BigDecimal("50")) < 0) {
        scores.setDebtManagement(new BigDecimal("20"));
    } else if (debtRatio.compareTo(new BigDecimal("70")) < 0) {
        scores.setDebtManagement(new BigDecimal("15"));
        recommendations.add("资产负债率偏高，建议加快债务偿还");
    } else {
        scores.setDebtManagement(new BigDecimal("10"));
        recommendations.add("资产负债率过高，需优先处理债务问题");
    }

    // 2. 流动性管理 (0-20分)
    BigDecimal liquidityRatio = metrics.getLiquidityRatio();
    if (liquidityRatio.compareTo(new BigDecimal("20")) >= 0) {
        scores.setLiquidity(new BigDecimal("20"));
    } else if (liquidityRatio.compareTo(new BigDecimal("15")) >= 0) {
        scores.setLiquidity(new BigDecimal("16"));
    } else if (liquidityRatio.compareTo(new BigDecimal("10")) >= 0) {
        scores.setLiquidity(new BigDecimal("12"));
        recommendations.add("流动性比率" + liquidityRatio + "%，建议提升至20%以上");
    } else {
        scores.setLiquidity(new BigDecimal("8"));
        recommendations.add("流动性不足，建议增加应急资金储备");
    }

    // 3. 储蓄能力 (0-25分)
    BigDecimal savingsRate = metrics.getSavingsRate();
    if (savingsRate.compareTo(new BigDecimal("30")) > 0) {
        scores.setSavings(new BigDecimal("25"));
    } else if (savingsRate.compareTo(new BigDecimal("20")) >= 0) {
        scores.setSavings(new BigDecimal("20"));
    } else if (savingsRate.compareTo(new BigDecimal("10")) >= 0) {
        scores.setSavings(new BigDecimal("15"));
        recommendations.add("储蓄率偏低，建议提升至20%以上");
    } else {
        scores.setSavings(new BigDecimal("10"));
        recommendations.add("储蓄率过低，建议控制支出并增加储蓄");
    }

    // 4. 投资收益 (0-20分)
    BigDecimal investmentReturn = metrics.getInvestmentReturnRate();
    if (investmentReturn.compareTo(new BigDecimal("15")) > 0) {
        scores.setInvestment(new BigDecimal("20"));
    } else if (investmentReturn.compareTo(new BigDecimal("10")) >= 0) {
        scores.setInvestment(new BigDecimal("16"));
    } else if (investmentReturn.compareTo(new BigDecimal("5")) >= 0) {
        scores.setInvestment(new BigDecimal("12"));
        recommendations.add("投资收益率" + investmentReturn + "%，考虑优化投资组合");
    } else {
        scores.setInvestment(new BigDecimal("8"));
        recommendations.add("投资收益率偏低，建议重新评估投资策略");
    }

    // 5. 资产增长 (0-10分)
    BigDecimal growthRate = metrics.getYearlyChangeRate();
    if (growthRate.compareTo(new BigDecimal("15")) > 0) {
        scores.setGrowth(new BigDecimal("10"));
    } else if (growthRate.compareTo(new BigDecimal("10")) >= 0) {
        scores.setGrowth(new BigDecimal("8"));
    } else if (growthRate.compareTo(new BigDecimal("5")) >= 0) {
        scores.setGrowth(new BigDecimal("6"));
    } else {
        scores.setGrowth(new BigDecimal("4"));
        recommendations.add("资产增长缓慢，建议优化资产配置");
    }

    // 计算总分
    BigDecimal totalScore = scores.getDebtManagement()
        .add(scores.getLiquidity())
        .add(scores.getSavings())
        .add(scores.getInvestment())
        .add(scores.getGrowth());

    healthScore.setTotalScore(totalScore);
    healthScore.setScores(scores);

    // 确定等级
    if (totalScore.compareTo(new BigDecimal("90")) >= 0) {
        healthScore.setGrade("A+");
    } else if (totalScore.compareTo(new BigDecimal("80")) >= 0) {
        healthScore.setGrade("A");
    } else if (totalScore.compareTo(new BigDecimal("70")) >= 0) {
        healthScore.setGrade("B");
    } else if (totalScore.compareTo(new BigDecimal("60")) >= 0) {
        healthScore.setGrade("C");
    } else {
        healthScore.setGrade("D");
    }

    healthScore.setRecommendations(recommendations);

    return healthScore;
}
```

---

## 三、实施计划

### 阶段1: 后端API开发 (2-3天)

**任务清单**:
- [ ] 创建 `EnhancedFinancialMetricsDTO` 及内部类
- [ ] 实现 `calculateCashFlowMetrics()` - 现金流指标计算
- [ ] 实现 `calculateInvestmentMetrics()` - 投资收益指标计算
- [ ] 实现 `calculateHealthScore()` - 综合评分计算
- [ ] 整合到 `getEnhancedFinancialMetrics()` 主方法
- [ ] 添加单元测试
- [ ] 更新 Swagger API 文档

**依赖服务**:
- ✅ `IncomeAnalysisService` (已存在)
- ✅ `ExpenseAnalysisService` (已存在)
- ✅ `InvestmentAnalysisService` (已存在)

### 阶段2: 前端页面重构 (3-4天)

**任务清单**:
- [ ] 创建新的组件结构
  - [ ] `CashFlowMetrics.vue` - 现金流卡片组件
  - [ ] `InvestmentMetrics.vue` - 投资收益卡片组件
  - [ ] `HealthScoreCard.vue` - 健康评分卡片组件
  - [ ] `FinancialRatios.vue` - 财务比率组件 (拆分现有代码)
- [ ] 重构 `FinancialMetrics.vue` 主页面
  - [ ] 整合新组件
  - [ ] 优化布局 (响应式网格)
  - [ ] 添加加载状态和错误处理
- [ ] 数据可视化增强
  - [ ] 健康评分进度条
  - [ ] 现金流对比图表
  - [ ] 投资收益趋势图
- [ ] 样式优化 (Tailwind CSS)

### 阶段3: 测试与优化 (1-2天)

**任务清单**:
- [ ] 前后端集成测试
- [ ] 边界情况测试 (无数据、零值等)
- [ ] 性能优化 (API响应时间、前端渲染)
- [ ] 移动端适配测试
- [ ] 用户体验优化

---

## 四、技术要点

### 4.1 必需支出 vs 可选支出分类

**方案**: 扩展 `expense_categories_major` 表

```sql
ALTER TABLE expense_categories_major
ADD COLUMN is_essential BOOLEAN DEFAULT TRUE COMMENT '是否为必需支出';

-- 更新分类
UPDATE expense_categories_major SET is_essential = TRUE
WHERE id IN (1, 2, 3, 4, 6); -- 住房、食品、交通、医疗、保险

UPDATE expense_categories_major SET is_essential = FALSE
WHERE id IN (5, 7, 8, 9, 10); -- 娱乐、购物、旅游、教育、其他
```

### 4.2 缓存策略

由于财务指标计算涉及多个服务调用，考虑添加缓存：

```java
@Cacheable(value = "financialMetrics", key = "#familyId + '_' + #asOfDate")
public EnhancedFinancialMetricsDTO getEnhancedFinancialMetrics(
    Long userId, Long familyId, LocalDate asOfDate
) {
    // ...
}
```

### 4.3 异步计算

对于复杂计算，考虑使用 `CompletableFuture` 并行执行：

```java
CompletableFuture<Void> incomeFuture = CompletableFuture.runAsync(
    () -> calculateCashFlowMetrics(metrics, familyId, year)
);

CompletableFuture<Void> investmentFuture = CompletableFuture.runAsync(
    () -> calculateInvestmentMetrics(metrics, familyId, year)
);

CompletableFuture.allOf(incomeFuture, investmentFuture).join();
```

---

## 五、后续扩展方向

### 5.1 财务目标追踪
- 设定储蓄目标、投资收益目标
- 显示目标达成进度
- 预测达成时间

### 5.2 历史对比
- 多年度财务健康评分趋势
- 各指标历史走势图
- 季度环比分析

### 5.3 同行基准对比
- 按年龄段/收入水平的平均值对比
- 财务健康度排名
- 改进空间提示

---

## 六、风险与注意事项

**数据准确性**:
- ⚠️ 收入数据依赖 `income_records` 完整性
- ⚠️ 投资收益需确保 `investment_transactions` 记录准确
- ⚠️ 必需支出分类需人工审核

**性能考虑**:
- ⚠️ 多服务调用可能导致响应变慢 (建议加缓存)
- ⚠️ 大量历史数据计算需优化查询

**向后兼容**:
- ✅ 保留所有原有字段，确保现有调用不受影响
- ✅ API路径可以是新的 `/analysis/metrics/enhanced`

---

**文档版本**: v1.0
**最后更新**: 2026-01-06
**下一步**: 开始后端API开发
