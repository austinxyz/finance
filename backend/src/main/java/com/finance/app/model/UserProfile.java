package com.finance.app.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "estimated_annual_expenses", precision = 15, scale = 2)
    private BigDecimal estimatedAnnualExpenses = BigDecimal.ZERO;

    @Column(name = "emergency_fund_months")
    private Integer emergencyFundMonths = 6;

    @Column(name = "risk_tolerance", length = 20)
    @Enumerated(EnumType.STRING)
    private RiskTolerance riskTolerance = RiskTolerance.MODERATE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum RiskTolerance {
        CONSERVATIVE,  // 保守型
        MODERATE,      // 稳健型
        AGGRESSIVE     // 进取型
    }
}
