package com.finance.app.dto.investment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 投资账户分析DTO（用于年度投资分析页面的账户分布）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentAccountAnalysisDTO {

    /**
     * 账户ID
     */
    private Long accountId;

    /**
     * 账户名称
     */
    private String accountName;

    /**
     * 资产分类名称
     */
    private String categoryName;

    /**
     * 期初总值
     */
    private Double beginningValue;

    /**
     * 期末总值
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
     * 投资收益
     */
    private Double investmentReturn;

    /**
     * 投资回报率（%）
     */
    private Double returnRate;

    /**
     * 在该大类中的占比（基于净投入）
     */
    private Double percentage;
}
