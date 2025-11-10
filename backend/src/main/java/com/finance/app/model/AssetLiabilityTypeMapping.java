package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "asset_liability_type_mappings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetLiabilityTypeMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_type", nullable = false, length = 50)
    private String assetType;  // 资产类型，如 REAL_ESTATE, CASH 等

    @Column(name = "liability_type", nullable = false, length = 50)
    private String liabilityType;  // 负债类型，如 MORTGAGE, CREDIT_CARD 等

    @Column(name = "description", length = 200)
    private String description;  // 映射关系描述

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
