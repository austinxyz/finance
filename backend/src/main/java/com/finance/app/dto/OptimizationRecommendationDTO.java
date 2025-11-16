package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimizationRecommendationDTO {

    private LocalDate asOfDate;

    // 综合评分
    private Double overallScore;  // 0-100分，财务健康度综合评分
    private String healthLevel;   // EXCELLENT, GOOD, FAIR, POOR

    // 各维度优化建议
    private AssetAllocationOptimization assetAllocationOptimization;
    private DebtManagementOptimization debtManagementOptimization;
    private LiquidityOptimization liquidityOptimization;
    private RiskOptimization riskOptimization;
    private TaxOptimization taxOptimization;

    // 行动计划
    private List<ActionItem> prioritizedActions;

    // 预期效果
    private ExpectedImpact expectedImpact;

    /**
     * 资产配置优化建议
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetAllocationOptimization {
        private String priority;  // HIGH, MEDIUM, LOW
        private String status;    // NEEDS_ATTENTION, ACCEPTABLE, OPTIMAL
        private String summary;
        private Double currentScore;  // 0-100分

        // 当前配置
        private AllocationSnapshot currentAllocation;

        // 建议配置
        private AllocationSnapshot recommendedAllocation;

        // 具体建议
        private List<String> suggestions;

        // 预期收益
        private String expectedBenefit;
    }

    /**
     * 负债管理优化建议
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DebtManagementOptimization {
        private String priority;
        private String status;
        private String summary;
        private Double currentScore;

        // 债务偿还策略
        private String recommendedStrategy;  // AVALANCHE, SNOWBALL, BALANCED

        // 高息债务列表
        private List<HighInterestDebt> highInterestDebts;

        // 具体建议
        private List<String> suggestions;

        // 预期节省（利息）
        private Double expectedSavings;
    }

    /**
     * 流动性优化建议
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiquidityOptimization {
        private String priority;
        private String status;
        private String summary;
        private Double currentScore;

        // 当前流动资金
        private Double currentCash;

        // 建议流动资金
        private Double recommendedCash;

        // 缺口/盈余
        private Double gap;

        // 具体建议
        private List<String> suggestions;
    }

    /**
     * 风险优化建议
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskOptimization {
        private String priority;
        private String status;
        private String summary;
        private Double currentScore;

        // 当前风险评级
        private String currentRiskLevel;

        // 建议风险调整
        private List<RiskAdjustment> adjustments;

        // 具体建议
        private List<String> suggestions;
    }

    /**
     * 税务优化建议
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TaxOptimization {
        private String priority;
        private String status;
        private String summary;
        private Double currentScore;

        // 应税资产占比
        private Double taxablePercentage;

        // 税务优化空间
        private Double optimizationPotential;

        // 具体建议
        private List<String> suggestions;
    }

    /**
     * 配置快照
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AllocationSnapshot {
        private Double cashPercentage;
        private Double stocksPercentage;
        private Double retirementPercentage;
        private Double realEstatePercentage;
        private Double otherPercentage;
    }

    /**
     * 高息债务
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HighInterestDebt {
        private String accountName;
        private Double balance;
        private Double interestRate;
        private String category;
    }

    /**
     * 风险调整建议
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RiskAdjustment {
        private String assetType;
        private Double currentPercentage;
        private Double recommendedPercentage;
        private String reason;
    }

    /**
     * 行动项
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionItem {
        private String category;      // ASSET_ALLOCATION, DEBT, LIQUIDITY, RISK, TAX
        private String priority;      // CRITICAL, HIGH, MEDIUM, LOW
        private String action;
        private String timeframe;     // IMMEDIATE, SHORT_TERM, MEDIUM_TERM, LONG_TERM
        private String expectedImpact;
        private Integer order;        // 执行顺序
    }

    /**
     * 预期效果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpectedImpact {
        // 净资产增长预期
        private Double netWorthIncrease;

        // 风险降低预期
        private Double riskReduction;

        // 年化收益率提升
        private Double returnImprovement;

        // 税务节省
        private Double taxSavings;

        // 总体改善描述
        private String overallImprovement;
    }
}
