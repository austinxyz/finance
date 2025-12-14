package com.finance.app.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvestmentAccountAnalysisDTO {
    private Long accountId;
    private String accountName;
    private String categoryName;
    private String userName;               // 账户所属用户名
    private String currency;               // 账户货币
    private BigDecimal totalDeposits;
    private BigDecimal totalWithdrawals;
    private BigDecimal netDeposits;

    // 投资回报相关字段（原始货币金额）
    private BigDecimal currentAssets;      // 当前资产
    private BigDecimal lastYearEndAssets;  // 去年年底资产
    private BigDecimal returns;            // 投资回报 = 当前资产 - 去年年底资产 - 净投入
    private BigDecimal returnRate;         // 投资回报率

    // USD转换后的金额（用于总计计算）
    private BigDecimal currentAssetsUsd;
    private BigDecimal lastYearEndAssetsUsd;
    private BigDecimal netDepositsUsd;
    private BigDecimal returnsUsd;
}
