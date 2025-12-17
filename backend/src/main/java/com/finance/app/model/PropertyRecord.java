package com.finance.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 房产记录实体
 * 用于记录房产购买的详细信息，与资产账户关联
 * 在年度支出汇总时，购买年份会使用特殊的计算逻辑
 */
@Entity
@Table(name = "property_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PropertyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 关联的房产资产账户ID
     */
    @Column(name = "asset_account_id", nullable = false, unique = true)
    private Long assetAccountId;

    /**
     * 购买日期
     */
    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    /**
     * 首付金额
     */
    @Column(name = "down_payment", nullable = false, precision = 18, scale = 2)
    private BigDecimal downPayment;

    /**
     * 房贷金额（初始贷款金额）
     */
    @Column(name = "mortgage_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal mortgageAmount;

    /**
     * 房产价值（购买时的市场价值）
     */
    @Column(name = "property_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal propertyValue;

    /**
     * 货币
     */
    @Column(nullable = false, length = 10)
    private String currency = "USD";

    /**
     * 备注说明
     */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 关联关系
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_account_id", insertable = false, updatable = false)
    private AssetAccount assetAccount;

    /**
     * 获取购买年份
     */
    @Transient
    public Integer getPurchaseYear() {
        return purchaseDate != null ? purchaseDate.getYear() : null;
    }
}
