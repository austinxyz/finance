package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支出记录实体
 * 对应表：expense_records
 */
@Entity
@Table(name = "expense_records",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_period_category",
            columnNames = {"family_id", "expense_period", "minor_category_id"})
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 家庭ID
     */
    @Column(name = "family_id", nullable = false)
    private Long familyId;

    /**
     * 记录人ID
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // ============ 期间与分类 ============

    /**
     * 支出年份
     */
    @Column(name = "expense_year", nullable = false)
    private Integer expenseYear;

    /**
     * 支出月份
     */
    @Column(name = "expense_month", nullable = false)
    private Integer expenseMonth;

    /**
     * 支出期间（YYYY-MM格式）
     */
    @Column(name = "expense_period", nullable = false, length = 7)
    private String expensePeriod;

    /**
     * 大类ID
     */
    @Column(name = "major_category_id", nullable = false)
    private Long majorCategoryId;

    /**
     * 子分类ID
     */
    @Column(name = "minor_category_id", nullable = false)
    private Long minorCategoryId;

    // ============ 金额 ============

    /**
     * 支出金额（原币种）
     */
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /**
     * 货币代码
     */
    @Column(nullable = false, length = 10)
    private String currency = "USD";

    // ============ 类型与说明 ============

    /**
     * 支出类型：FIXED_DAILY(固定日常), LARGE_IRREGULAR(大额不定期)
     */
    @Column(name = "expense_type", nullable = false, length = 20)
    private String expenseType;

    /**
     * 说明
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    // ============ 审计字段 ============

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============ 关联关系 ============

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", insertable = false, updatable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "major_category_id", insertable = false, updatable = false)
    private ExpenseCategoryMajor majorCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "minor_category_id", insertable = false, updatable = false)
    private ExpenseCategoryMinor minorCategory;
}
