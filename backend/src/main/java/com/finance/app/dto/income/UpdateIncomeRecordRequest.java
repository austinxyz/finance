package com.finance.app.dto.income;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 更新收入记录请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateIncomeRecordRequest {

    /**
     * 关联的资产账户ID（可选）
     */
    private Long assetAccountId;

    /**
     * 收入金额（税后实际到账）
     */
    @NotNull(message = "收入金额不能为空")
    @DecimalMin(value = "0.01", message = "收入金额必须大于0")
    private BigDecimal amount;

    /**
     * 说明（最长500字符）
     */
    @Size(max = 500, message = "说明不能超过500个字符")
    private String description;
}
