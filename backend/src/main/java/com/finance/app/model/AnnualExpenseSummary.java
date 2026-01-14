package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 年度支出汇总实体
 * 对应表：annual_expense_summary
 * 存储经过资产/负债调整后的年度支出汇总数据
 */
@Entity
@Table(name = "annual_expense_summary",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_year_category",
            columnNames = {"family_id", "summary_year", "major_category_id", "minor_category_id"})
    },
    indexes = {
        @Index(name = "idx_family_year", columnList = "family_id,summary_year"),
        @Index(name = "idx_major_category", columnList = "major_category_id")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnnualExpenseSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 家庭ID
     */
    @Column(name = "family_id", nullable = false)
    private Long familyId;

    /**
     * 记录人ID (可为空)
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * 汇总年份
     */
    @Column(name = "summary_year", nullable = false)
    private Integer summaryYear;

    /**
     * 大类ID (NULL表示总计行)
     */
    @Column(name = "major_category_id", nullable = true)
    private Long majorCategoryId;

    /**
     * 小类ID (NULL表示大类小计)
     */
    @Column(name = "minor_category_id")
    private Long minorCategoryId;

    /**
     * 基础支出金额（未调整）
     */
    @Column(name = "base_expense_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal baseExpenseAmount = BigDecimal.ZERO;

    /**
     * 特殊支出金额（单笔>=10000 USD）
     */
    @Column(name = "special_expense_amount", precision = 18, scale = 2)
    private BigDecimal specialExpenseAmount = BigDecimal.ZERO;

    /**
     * 特殊支出详情 (JSON格式)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "special_expense_details", columnDefinition = "JSON")
    private String specialExpenseDetails;

    /**
     * 资产调整金额
     */
    @Column(name = "asset_adjustment", precision = 18, scale = 2)
    private BigDecimal assetAdjustment = BigDecimal.ZERO;

    /**
     * 负债调整金额
     */
    @Column(name = "liability_adjustment", precision = 18, scale = 2)
    private BigDecimal liabilityAdjustment = BigDecimal.ZERO;

    /**
     * 调整详情 (JSON格式)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "adjustment_details", columnDefinition = "JSON")
    private String adjustmentDetails;

    /**
     * 实际支出金额（调整后）
     */
    @Column(name = "actual_expense_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal actualExpenseAmount;

    /**
     * 货币代码
     */
    @Column(length = 10)
    private String currency = "CNY";

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
