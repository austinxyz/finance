package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 年度财务摘要实体类
 * 存储每年的资产、负债、净资产汇总数据
 */
@Entity
@Table(name = "annual_financial_summary")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnualFinancialSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "summary_date", nullable = false)
    private LocalDate summaryDate;

    // 总计数据
    @Column(name = "total_assets", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalAssets = BigDecimal.ZERO;

    @Column(name = "total_liabilities", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalLiabilities = BigDecimal.ZERO;

    @Column(name = "net_worth", nullable = false, precision = 18, scale = 2)
    private BigDecimal netWorth = BigDecimal.ZERO;

    // 资产和负债分类明细（JSON格式）
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "asset_breakdown", columnDefinition = "JSON")
    private Map<String, BigDecimal> assetBreakdown;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "liability_breakdown", columnDefinition = "JSON")
    private Map<String, BigDecimal> liabilityBreakdown;

    // 货币单位
    @Column(length = 10)
    private String currency = "USD";

    // 同比数据
    @Column(name = "yoy_asset_change", precision = 18, scale = 2)
    private BigDecimal yoyAssetChange;

    @Column(name = "yoy_liability_change", precision = 18, scale = 2)
    private BigDecimal yoyLiabilityChange;

    @Column(name = "yoy_net_worth_change", precision = 18, scale = 2)
    private BigDecimal yoyNetWorthChange;

    @Column(name = "yoy_asset_change_pct", precision = 5, scale = 2)
    private BigDecimal yoyAssetChangePct;

    @Column(name = "yoy_liability_change_pct", precision = 5, scale = 2)
    private BigDecimal yoyLiabilityChangePct;

    @Column(name = "yoy_net_worth_change_pct", precision = 5, scale = 2)
    private BigDecimal yoyNetWorthChangePct;

    // 房产相关数据
    @Column(name = "real_estate_assets", precision = 18, scale = 2)
    private BigDecimal realEstateAssets;

    @Column(name = "real_estate_net_worth", precision = 18, scale = 2)
    private BigDecimal realEstateNetWorth;

    @Column(name = "non_real_estate_net_worth", precision = 18, scale = 2)
    private BigDecimal nonRealEstateNetWorth;

    @Column(name = "yoy_real_estate_net_worth_change", precision = 18, scale = 2)
    private BigDecimal yoyRealEstateNetWorthChange;

    @Column(name = "yoy_real_estate_net_worth_change_pct", precision = 5, scale = 2)
    private BigDecimal yoyRealEstateNetWorthChangePct;

    @Column(name = "yoy_non_real_estate_net_worth_change", precision = 18, scale = 2)
    private BigDecimal yoyNonRealEstateNetWorthChange;

    @Column(name = "yoy_non_real_estate_net_worth_change_pct", precision = 5, scale = 2)
    private BigDecimal yoyNonRealEstateNetWorthChangePct;

    @Column(name = "real_estate_asset_ratio", precision = 5, scale = 2)
    private BigDecimal realEstateAssetRatio;

    @Column(name = "real_estate_net_worth_ratio", precision = 5, scale = 2)
    private BigDecimal realEstateNetWorthRatio;

    @Column(name = "real_estate_to_net_worth_ratio", precision = 5, scale = 2)
    private BigDecimal realEstateToNetWorthRatio;

    // 备注
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 关联家庭
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", insertable = false, updatable = false)
    private Family family;
}
