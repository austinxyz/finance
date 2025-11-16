package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * 风险评估 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessmentDTO {

    /**
     * 评估日期
     */
    private LocalDate asOfDate;

    /**
     * 整体风险评分 (0-100, 100为最高风险)
     */
    private Double overallRiskScore;

    /**
     * 整体风险等级 (LOW, MEDIUM, HIGH, CRITICAL)
     */
    private String overallRiskLevel;

    /**
     * 资产集中度风险
     */
    private ConcentrationRisk concentrationRisk;

    /**
     * 负债压力评估
     */
    private DebtPressure debtPressure;

    /**
     * 流动性风险
     */
    private LiquidityRisk liquidityRisk;

    /**
     * 市场风险
     */
    private MarketRisk marketRisk;

    /**
     * 综合建议
     */
    private List<String> recommendations;

    /**
     * 资产集中度风险
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConcentrationRisk {
        /**
         * 风险等级
         */
        private String level;

        /**
         * 风险分数
         */
        private Double score;

        /**
         * 最高集中度资产类别
         */
        private String topConcentratedCategory;

        /**
         * 最高集中度百分比
         */
        private Double topConcentrationPercentage;

        /**
         * 赫芬达尔指数 (Herfindahl Index, 0-1, 越接近1越集中)
         */
        private Double herfindahlIndex;

        /**
         * 风险描述
         */
        private String description;

        /**
         * 建议
         */
        private List<String> suggestions;
    }

    /**
     * 负债压力评估
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DebtPressure {
        /**
         * 风险等级
         */
        private String level;

        /**
         * 风险分数
         */
        private Double score;

        /**
         * 资产负债率 (%)
         */
        private Double debtToAssetRatio;

        /**
         * 总负债
         */
        private Double totalLiabilities;

        /**
         * 总资产
         */
        private Double totalAssets;

        /**
         * 高息负债占比 (%)
         */
        private Double highInterestDebtRatio;

        /**
         * 风险描述
         */
        private String description;

        /**
         * 建议
         */
        private List<String> suggestions;
    }

    /**
     * 流动性风险
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LiquidityRisk {
        /**
         * 风险等级
         */
        private String level;

        /**
         * 风险分数
         */
        private Double score;

        /**
         * 流动性比率 (%)
         */
        private Double liquidityRatio;

        /**
         * 现金及现金等价物
         */
        private Double cashAmount;

        /**
         * 总资产
         */
        private Double totalAssets;

        /**
         * 建议紧急储备金
         */
        private Double recommendedEmergencyFund;

        /**
         * 风险描述
         */
        private String description;

        /**
         * 建议
         */
        private List<String> suggestions;
    }

    /**
     * 市场风险
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarketRisk {
        /**
         * 风险等级
         */
        private String level;

        /**
         * 风险分数
         */
        private Double score;

        /**
         * 股票投资占比 (%)
         */
        private Double stockAllocationPercentage;

        /**
         * 数字货币占比 (%)
         */
        private Double cryptoAllocationPercentage;

        /**
         * 高风险资产总占比 (%)
         */
        private Double highRiskAssetsPercentage;

        /**
         * 风险描述
         */
        private String description;

        /**
         * 建议
         */
        private List<String> suggestions;
    }
}
