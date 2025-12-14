package com.finance.app.dto.investment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建投资交易记录请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateInvestmentTransactionRequest {

    /**
     * 资产账户ID
     */
    @NotNull(message = "账户ID不能为空")
    private Long accountId;

    /**
     * 交易期间（YYYY-MM格式）
     */
    @NotNull(message = "交易期间不能为空")
    private String transactionPeriod;

    /**
     * 交易类型：DEPOSIT / WITHDRAWAL
     */
    @NotNull(message = "交易类型不能为空")
    private String transactionType;

    /**
     * 交易金额
     */
    @NotNull(message = "交易金额不能为空")
    @Positive(message = "交易金额必须大于0")
    private BigDecimal amount;

    /**
     * 交易说明
     */
    private String description;
}
