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
 * 投资交易记录实体
 * 对应表：investment_transactions
 */
@Entity
@Table(name = "investment_transactions",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_account_period_type",
            columnNames = {"account_id", "transaction_period", "transaction_type"})
    },
    indexes = {
        @Index(name = "idx_account_period", columnList = "account_id, transaction_period"),
        @Index(name = "idx_period", columnList = "transaction_period"),
        @Index(name = "idx_transaction_type", columnList = "transaction_type")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 资产账户ID
     */
    @Column(name = "account_id", nullable = false)
    private Long accountId;

    /**
     * 交易期间（YYYY-MM格式，如：2024-01）
     */
    @Column(name = "transaction_period", nullable = false, length = 7)
    private String transactionPeriod;

    /**
     * 交易类型
     * DEPOSIT: 投入（买入、追加投资）
     * WITHDRAWAL: 取出（卖出、提取资金）
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private TransactionType transactionType;

    /**
     * 交易金额（正数，方向由transactionType决定）
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    /**
     * 交易说明
     */
    @Column(length = 500)
    private String description;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ============ 关联关系 ============

    /**
     * 关联的资产账户
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", insertable = false, updatable = false)
    private AssetAccount account;

    /**
     * 交易类型枚举
     */
    public enum TransactionType {
        /**
         * 投入（买入、追加投资）
         */
        DEPOSIT,

        /**
         * 取出（卖出、提取资金）
         */
        WITHDRAWAL
    }
}
