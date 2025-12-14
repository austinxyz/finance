package com.finance.app.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class AccountMonthlyTrendResponseDTO {
    // 账户年度汇总信息
    private String accountName;
    private String currency;               // 账户货币
    private BigDecimal currentAssets;      // 当前资产（原始货币）
    private BigDecimal lastYearEndAssets;  // 去年年底资产（原始货币）
    private BigDecimal netDeposits;        // 净投入（原始货币）
    private BigDecimal returns;            // 投资回报（原始货币）
    private BigDecimal returnRate;         // 投资回报率

    // 月度趋势数据
    private List<InvestmentMonthlyTrendDTO> monthlyData;
}
