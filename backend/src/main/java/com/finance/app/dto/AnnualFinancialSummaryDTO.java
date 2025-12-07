package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 年度财务摘要DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnualFinancialSummaryDTO {

    private Long id;
    private Long familyId;
    private Integer year;
    private LocalDate summaryDate;

    // 总计数据
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal netWorth;

    // 分类明细
    private Map<String, BigDecimal> assetBreakdown;
    private Map<String, BigDecimal> liabilityBreakdown;

    // 货币单位
    private String currency;

    // 同比数据
    private BigDecimal yoyAssetChange;
    private BigDecimal yoyLiabilityChange;
    private BigDecimal yoyNetWorthChange;
    private BigDecimal yoyAssetChangePct;
    private BigDecimal yoyLiabilityChangePct;
    private BigDecimal yoyNetWorthChangePct;

    // 房产相关数据
    private BigDecimal realEstateAssets;             // 房产资产总额
    private BigDecimal realEstateNetWorth;           // 房产净资产（房产资产 - 房贷）
    private BigDecimal nonRealEstateNetWorth;        // 非房产净资产
    private BigDecimal yoyRealEstateNetWorthChange;  // 房产净资产同比变化
    private BigDecimal yoyRealEstateNetWorthChangePct; // 房产净资产同比变化百分比
    private BigDecimal yoyNonRealEstateNetWorthChange; // 非房产净资产同比变化
    private BigDecimal yoyNonRealEstateNetWorthChangePct; // 非房产净资产同比变化百分比
    private BigDecimal realEstateAssetRatio;         // 房产资产占总资产比例
    private BigDecimal realEstateNetWorthRatio;      // 房产净资产占总净资产比例
    private BigDecimal realEstateToNetWorthRatio;    // 房产资产总值占净资产比例

    // 备注
    private String notes;
}
