package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetSummaryDTO {
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal netWorth;
    private Map<String, BigDecimal> assetsByCategory;  // 按分类汇总
    private Map<String, BigDecimal> assetsByType;      // 按类型汇总
    private LocalDate actualDate;                       // 实际使用的最早数据日期（用于按比例折算变化率）
}
