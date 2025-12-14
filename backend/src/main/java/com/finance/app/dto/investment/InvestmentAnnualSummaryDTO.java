package com.finance.app.dto.investment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 年度投资汇总DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentAnnualSummaryDTO {

    /**
     * 年份
     */
    private Integer year;

    /**
     * 货币代码
     */
    private String currency;

    /**
     * 期初总值（上一年12月31日）
     */
    private Double beginningValue;

    /**
     * 期末总值（本年12月31日或最新日期）
     */
    private Double endingValue;

    /**
     * 总投入
     */
    private Double totalDeposits;

    /**
     * 总取出
     */
    private Double totalWithdrawals;

    /**
     * 净投入（总投入 - 总取出）
     */
    private Double netDeposits;

    /**
     * 投资收益（期末总值 - 期初总值 - 净投入）
     */
    private Double investmentReturn;

    /**
     * 投资回报率（%）
     * 计算公式：(投资收益 / 平均资本) × 100
     * 平均资本 = 期初总值 + 净投入 / 2
     */
    private Double returnRate;
}
