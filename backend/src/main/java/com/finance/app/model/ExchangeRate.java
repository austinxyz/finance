package com.finance.app.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exchange_rates",
       uniqueConstraints = @UniqueConstraint(columnNames = {"currency", "effective_date"}))
@Data
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;  // 货币代码，如 CNY, EUR, GBP

    @Column(name = "rate_to_usd", nullable = false, precision = 18, scale = 8)
    private BigDecimal rateToUsd;  // 对美元的汇率（1单位该货币 = X美元）

    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;  // 生效日期

    @Column(name = "source", length = 100)
    private String source;  // 汇率来源，如 "Manual", "API", "央行"

    @Column(name = "notes", length = 500)
    private String notes;  // 备注

    @Column(name = "is_active")
    private Boolean isActive = true;  // 是否启用

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (isActive == null) {
            isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
