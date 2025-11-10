package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "net_asset_category_liability_type_mappings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetAssetCategoryLiabilityTypeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "net_asset_category_id", nullable = false)
    private Long netAssetCategoryId;  // 净资产类别ID

    @Column(name = "liability_type", nullable = false, length = 50)
    private String liabilityType;  // 负债类型，如 MORTGAGE, CREDIT_CARD 等

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
