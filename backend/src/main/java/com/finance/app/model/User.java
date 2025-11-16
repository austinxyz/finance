package com.finance.app.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_id")
    private Long familyId;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "annual_income", precision = 15, scale = 2)
    private BigDecimal annualIncome;

    @Column(name = "income_currency", length = 10)
    private String incomeCurrency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_tolerance", length = 20)
    private RiskTolerance riskTolerance;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    public enum RiskTolerance {
        CONSERVATIVE,  // 保守型
        MODERATE,      // 稳健型
        AGGRESSIVE     // 激进型
    }
}
