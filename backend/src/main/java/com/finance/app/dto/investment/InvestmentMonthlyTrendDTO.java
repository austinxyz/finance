package com.finance.app.dto.investment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投资月度趋势DTO（用于年度投资分析页面的月度趋势图）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentMonthlyTrendDTO {

    /**
     * 月份（1-12）
     */
    private Integer month;

    /**
     * 期间（YYYY-MM）
     */
    private String period;

    /**
     * 投入金额
     */
    private Double deposits;

    /**
     * 取出金额
     */
    private Double withdrawals;

    /**
     * 账户总值（月末值，从asset_records获取）
     */
    private Double accountValue;
}
