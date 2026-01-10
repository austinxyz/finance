package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "families")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_name", nullable = false, length = 100)
    private String familyName;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @Column(name = "is_protected")
    private Boolean isProtected = false;

    @Column(name = "annual_expenses", precision = 15, scale = 2)
    private BigDecimal annualExpenses;

    @Column(name = "expenses_currency", length = 10)
    private String expensesCurrency = "USD";

    @Column(name = "emergency_fund_months")
    private Integer emergencyFundMonths = 6;

    @Column(name = "financial_goals", columnDefinition = "TEXT")
    private String financialGoals;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
