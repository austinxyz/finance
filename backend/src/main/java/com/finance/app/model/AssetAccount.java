package com.finance.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_accounts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AssetAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "asset_type_id", nullable = false)
    private Long assetTypeId;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "account_number", length = 100)
    private String accountNumber;

    @Column(length = 100)
    private String institution;

    @Column(length = 10)
    private String currency = "USD";

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "is_primary_residence")
    private Boolean isPrimaryResidence = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "tax_status", length = 20)
    private TaxStatus taxStatus = TaxStatus.TAXABLE;

    /**
     * 关联的负债账户ID（例如：房产关联房贷）
     */
    @Column(name = "linked_liability_account_id")
    private Long linkedLiabilityAccountId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // 关联关系（可选，用于级联查询）
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_type_id", insertable = false, updatable = false)
    private AssetType assetType;

    /**
     * 关联的负债账户（例如：房产关联的房贷）
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_liability_account_id", insertable = false, updatable = false)
    private LiabilityAccount linkedLiabilityAccount;
}
