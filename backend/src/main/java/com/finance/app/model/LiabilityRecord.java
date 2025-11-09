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
@Table(name = "liability_records")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiabilityRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "outstanding_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal outstandingBalance;

    @Column(length = 10)
    private String currency = "CNY";

    @Column(name = "exchange_rate", precision = 12, scale = 6)
    private BigDecimal exchangeRate = BigDecimal.ONE;

    @Column(name = "balance_in_base_currency", precision = 18, scale = 2)
    private BigDecimal balanceInBaseCurrency;

    @Column(name = "payment_amount", precision = 18, scale = 2)
    private BigDecimal paymentAmount;

    @Column(name = "principal_payment", precision = 18, scale = 2)
    private BigDecimal principalPayment;

    @Column(name = "interest_payment", precision = 18, scale = 2)
    private BigDecimal interestPayment;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private LiabilityAccount account;
}
