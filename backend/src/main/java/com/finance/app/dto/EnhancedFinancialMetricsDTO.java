package com.finance.app.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 增强的财务指标DTO - 整合资产、负债、收入、支出、投资等全维度数据
 */
@Data
public class EnhancedFinancialMetricsDTO {

    // ==================== 基础信息 ====================
    private LocalDate asOfDate;              // 数据截止日期
    private Integer year;                    // 年份

    // ==================== 原有字段 (向后兼容) ====================
    private BigDecimal totalAssets;          // 总资产
    private BigDecimal totalLiabilities;     // 总负债
    private BigDecimal netWorth;             // 净资产
    private BigDecimal debtToAssetRatio;     // 资产负债率 (%)
    private BigDecimal liquidityRatio;       // 流动性比率 (%)
    private BigDecimal cashAmount;           // 现金金额

    // 月度变化
    private BigDecimal monthlyChange;        // 月度净资产变化
    private BigDecimal monthlyChangeRate;    // 月度变化率 (%)
    private LocalDate previousMonthDate;     // 上月日期
    private BigDecimal previousMonthNetWorth; // 上月净资产

    // 年度变化
    private BigDecimal yearlyChange;         // 年度净资产变化
    private BigDecimal yearlyChangeRate;     // 年度变化率 (%)
    private LocalDate previousYearDate;      // 去年同期日期
    private BigDecimal previousYearNetWorth; // 去年同期净资产

    // ==================== 新增: 现金流指标 ====================
    // 收入侧
    private BigDecimal annualTotalIncome;    // 年度总收入
    private BigDecimal annualWorkIncome;     // 年度工资收入 (Salary大类)
    private BigDecimal annualInvestmentIncome; // 年度投资收入 (Investment大类)
    private BigDecimal annualOtherIncome;    // 年度其他收入

    // 支出侧
    private BigDecimal annualTotalExpense;   // 年度总支出
    private BigDecimal annualEssentialExpense; // 年度必需支出
    private BigDecimal annualDiscretionaryExpense; // 年度可选支出

    // 现金流结果
    private BigDecimal netCashFlow;          // 净现金流 = 总收入 - 总支出
    private BigDecimal savingsRate;          // 储蓄率 (%) = 净现金流 / 总收入 * 100
    private BigDecimal expenseRatio;         // 支出率 (%) = 总支出 / 总收入 * 100

    // 对比数据
    private BigDecimal lastYearTotalIncome;  // 去年总收入
    private BigDecimal lastYearTotalExpense; // 去年总支出
    private BigDecimal incomeGrowthRate;     // 收入增长率 (%)
    private BigDecimal expenseGrowthRate;    // 支出增长率 (%)

    // 月度现金流变化
    private BigDecimal monthlyIncomeChange;  // 月度收入变化
    private BigDecimal monthlyExpenseChange; // 月度支出变化

    // ==================== 新增: 投资收益指标 ====================
    private BigDecimal totalInvested;        // 累计投入
    private BigDecimal currentInvestmentValue; // 投资市值
    private BigDecimal unrealizedGain;       // 未实现收益
    private BigDecimal realizedGain;         // 已实现收益 (年度)
    private BigDecimal totalInvestmentReturn; // 总投资回报 = 未实现 + 已实现
    private BigDecimal investmentReturnRate; // 投资收益率 (%) = 总回报 / 累计投入 * 100
    private List<TopInvestmentCategory> topCategories; // 表现最佳的投资大类

    // ==================== 新增: 综合财务健康评分 ====================
    private HealthScoreDTO healthScore;

    // ==================== 内部类: 顶级投资大类 ====================
    @Data
    public static class TopInvestmentCategory {
        private String categoryName;         // 大类名称
        private BigDecimal value;            // 当前市值
        private BigDecimal returnRate;       // 收益率 (%)
    }

    // ==================== 内部类: 财务健康评分 ====================
    @Data
    public static class HealthScoreDTO {
        private BigDecimal totalScore;       // 总分 (0-100)
        private String grade;                // 等级 (A+, A, B, C, D)
        private ScoreBreakdown scores;       // 各维度得分
        private List<String> recommendations; // 改进建议

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
