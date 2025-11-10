package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "net_asset_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NetAssetCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;  // 净资产类别名称，如 "房地产净值", "流动资产净值" 等

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;  // 类别代码，如 "REAL_ESTATE_NET", "LIQUID_NET" 等

    @Column(name = "description", length = 500)
    private String description;  // 描述

    @Column(name = "display_order")
    private Integer displayOrder;  // 显示顺序

    @Column(name = "color", length = 20)
    private String color;  // 在图表中显示的颜色

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
