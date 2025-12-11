package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 支出大类实体
 * 对应表：expense_categories_major
 */
@Entity
@Table(name = "expense_categories_major")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategoryMajor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 大类编码（CHILDREN, CLOTHING, FOOD等）
     */
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    /**
     * 大类名称
     */
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * 图标（emoji或图标类名）
     */
    @Column(length = 50)
    private String icon;

    /**
     * 颜色代码
     */
    @Column(length = 20)
    private String color;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /**
     * 是否启用
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 说明
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 关联的子分类列表
     */
    @OneToMany(mappedBy = "majorCategory", fetch = FetchType.LAZY)
    private List<ExpenseCategoryMinor> minorCategories;
}
