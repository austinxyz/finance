package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RunwayAnalysisDTO {

    private BigDecimal liquidTotal;
    private BigDecimal monthlyBurn;
    private Integer runwayMonths;
    private String depletionDate;
    private Integer expenseMonthsUsed;
    private Boolean assetDataMissing;
    private Boolean expenseDataWarning;
    private LocalDate latestSnapshotDate;
    private List<AccountInfo> accountBreakdown;
    private Map<String, BigDecimal> expenseBreakdown;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountInfo {
        private Long id;
        private String accountName;
        private String accountType;
        private BigDecimal usdValue;
    }
}
