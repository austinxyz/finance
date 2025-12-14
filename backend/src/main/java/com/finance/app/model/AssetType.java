package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 资产大类定义实体
 * 对应数据库表：asset_type
 */
@Entity
@Table(name = "asset_type")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 类型代码（英文大写，如STOCKS）
     * 唯一标识，用于代码中引用
     */
    @Column(name = "type", nullable = false, unique = true, length = 50)
    private String type;

    /**
     * 类型名称（英文）
     */
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /**
     * 中文名称
     */
    @Column(name = "chinese_name", nullable = false, length = 100)
    private String chineseName;

    /**
     * 描述说明
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 图标（emoji或图标类名）
     */
    @Column(name = "icon", length = 50)
    private String icon;

    /**
     * 颜色代码（十六进制，如#3B82F6）
     */
    @Column(name = "color", length = 20)
    private String color;

    /**
     * 是否为投资类型
     * TRUE = 投资类（股票、退休基金、房产等）
     * FALSE = 非投资类（现金、其他等）
     */
    @Column(name = "is_investment", nullable = false)
    private Boolean isInvestment = false;

    /**
     * 显示顺序
     */
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
