package com.finance.app.dto.investment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 投资交易记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentTransactionDTO {

    private Long id;

    /**
     * 资产账户ID
     */
    private Long accountId;

    /**
     * 账户名称
     */
    private String accountName;

    /**
     * 资产分类ID
     */
    private Long categoryId;

    /**
     * 资产分类名称
     */
    private String categoryName;

    /**
     * 资产分类类型
     */
    private String categoryType;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 交易期间（YYYY-MM）
     */
    private String transactionPeriod;

    /**
     * 交易类型：DEPOSIT / WITHDRAWAL
     */
    private String transactionType;

    /**
     * 交易金额
     */
    private BigDecimal amount;

    /**
     * 货币代码（从asset_accounts获取）
     */
    private String currency;

    /**
     * 交易说明
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
}
