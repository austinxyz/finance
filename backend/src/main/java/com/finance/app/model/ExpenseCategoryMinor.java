package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 支出子分类实体
 * 对应表：expense_categories_minor
 */
@Entity
@Table(name = "expense_categories_minor",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_major_name", columnNames = {"major_category_id", "name"})
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategoryMinor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 所属大类ID
     */
    @Column(name = "major_category_id", nullable = false)
    private Long majorCategoryId;

    /**
     * 子分类名称
     */
    @Column(nullable = false, length = 100)
    private String name;

    /**
     * 是否启用
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 是否系统默认分类
     */
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    /**
     * 排序顺序
     */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    /**
     * 说明
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 支出类型（FIXED_DAILY=固定日常，LARGE_IRREGULAR=大额不定期）
     */
    @Column(name = "expense_type", length = 20, nullable = false)
    private String expenseType = "FIXED_DAILY";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /**
     * 关联的大类
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_category_id", insertable = false, updatable = false)
    private ExpenseCategoryMajor majorCategory;
}
