package com.finance.app.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinancialMetricsDTO {
    // 核心财务数据（仪表盘显示）
    private BigDecimal currentNetWorth;       // 当前净资产
    private BigDecimal lastYearNetWorth;      // 去年净资产（去年年底）
    private BigDecimal annualExpense;         // 本年度实际支出
    private BigDecimal annualInvestmentReturn; // 本年度投资回报
    private BigDecimal annualWorkIncome;      // 本年度工作收入（计算值）

    // 时间
    private LocalDate asOfDate;               // 计算日期
    private Integer year;                     // 年度

    // 保留字段（兼容性，但不在仪表盘显示）
    @Deprecated
    private BigDecimal totalAssets;           // 总资产（已废弃）
    @Deprecated
    private BigDecimal totalLiabilities;      // 总负债（已废弃）
    @Deprecated
    private BigDecimal netWorth;              // 净资产（已废弃，使用currentNetWorth）
    @Deprecated
    private BigDecimal debtToAssetRatio;      // 资产负债率（已废弃）
    @Deprecated
    private BigDecimal liquidityRatio;        // 流动性比率（已废弃）
    @Deprecated
    private BigDecimal cashAmount;            // 现金及现金等价物金额（已废弃）
    @Deprecated
    private BigDecimal monthlyChange;         // 月度净资产变化（已废弃）
    @Deprecated
    private BigDecimal monthlyChangeRate;     // 月度净资产变化率（已废弃）
    @Deprecated
    private BigDecimal yearlyChange;          // 年度净资产变化（已废弃）
    @Deprecated
    private BigDecimal yearlyChangeRate;      // 年度净资产变化率（已废弃）
    @Deprecated
    private LocalDate previousMonthDate;      // 上月日期（已废弃）
    @Deprecated
    private LocalDate previousYearDate;       // 去年同期日期（已废弃）
    @Deprecated
    private BigDecimal previousMonthNetWorth; // 上月净资产（已废弃）
    @Deprecated
    private BigDecimal previousYearNetWorth;  // 去年同期净资产（已废弃，使用lastYearNetWorth）
}
