package com.finance.app.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvestmentCategoryAnalysisDTO {
    private Long categoryId;
    private String categoryName;
    private String categoryIcon;
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal netDeposits;

    // 投资回报相关字段
    private BigDecimal currentAssets;      // 当前资产
    private BigDecimal lastYearEndAssets;  // 去年年底资产
    private BigDecimal returns;            // 投资回报 = 当前资产 - 去年年底资产 - 净投入
    private BigDecimal returnRate;         // 投资回报率
}
