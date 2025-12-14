package com.finance.app.dto.investment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量保存投资交易记录请求
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchInvestmentTransactionRequest {

    /**
     * 家庭ID
     */
    @NotNull(message = "家庭ID不能为空")
    private Long familyId;

    /**
     * 交易期间（YYYY-MM格式）
     */
    @NotNull(message = "交易期间不能为空")
    private String transactionPeriod;

    /**
     * 交易记录列表
     */
    @NotNull(message = "交易记录列表不能为空")
    private List<TransactionItem> transactions;

    /**
     * 单个账户的交易项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionItem {

        /**
         * 账户ID
         */
        @NotNull(message = "账户ID不能为空")
        private Long accountId;

        /**
         * 投入金额（可以为0或null）
         */
        private BigDecimal deposits;

        /**
         * 取出金额（可以为0或null）
         */
        private BigDecimal withdrawals;

        /**
         * 交易说明
         */
        private String description;
    }
}
