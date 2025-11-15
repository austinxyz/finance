package com.finance.app.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class FinancialMetricsDTO {
    // 基础指标
    private BigDecimal totalAssets;           // 总资产
    private BigDecimal totalLiabilities;      // 总负债
    private BigDecimal netWorth;              // 净资产

    // 财务比率
    private BigDecimal debtToAssetRatio;      // 资产负债率 (总负债/总资产)
    private BigDecimal liquidityRatio;        // 流动性比率 (现金及现金等价物/总资产)
    private BigDecimal cashAmount;            // 现金及现金等价物金额

    // 变化指标
    private BigDecimal monthlyChange;         // 月度净资产变化
    private BigDecimal monthlyChangeRate;     // 月度净资产变化率
    private BigDecimal yearlyChange;          // 年度净资产变化
    private BigDecimal yearlyChangeRate;      // 年度净资产变化率

    // 时间
    private LocalDate asOfDate;               // 计算日期
    private LocalDate previousMonthDate;      // 上月日期
    private LocalDate previousYearDate;       // 去年同期日期

    // 上期数据
    private BigDecimal previousMonthNetWorth; // 上月净资产
    private BigDecimal previousYearNetWorth;  // 去年同期净资产
}
