package com.finance.app.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收入预算实体
 */
@Entity
@Table(name = "income_budgets")
@Data
public class IncomeBudget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "major_category_id", nullable = false)
    private Long majorCategoryId;

    @Column(name = "minor_category_id")
    private Long minorCategoryId;

    @Column(nullable = false)
    private Integer year;

    @Column(name = "budgeted_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal budgetedAmount;

    @Column(nullable = false, length = 10)
    private String currency = "USD";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

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
