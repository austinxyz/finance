package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "net_asset_category_asset_type_mappings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetAssetCategoryAssetTypeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "net_asset_category_id", nullable = false)
    private Long netAssetCategoryId;  // 净资产类别ID

    @Column(name = "asset_type", nullable = false, length = 50)
    private String assetType;  // 资产类型，如 REAL_ESTATE, CASH 等

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
