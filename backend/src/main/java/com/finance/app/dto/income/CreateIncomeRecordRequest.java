package com.finance.app.dto.income;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建收入记录请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIncomeRecordRequest {

    @NotNull(message = "家庭ID不能为空")
    private Long familyId;

    /**
     * 关联的资产账户ID（可选）
     */
    private Long assetAccountId;

    /**
     * 收入期间（YYYY-MM格式）
     */
    @NotBlank(message = "收入期间不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "收入期间格式必须为YYYY-MM")
    private String period;

    /**
     * 大类ID
     */
    @NotNull(message = "大类ID不能为空")
    private Long majorCategoryId;

    /**
     * 小类ID（可选）
     */
    private Long minorCategoryId;

    /**
     * 收入金额（税后实际到账）
     */
    @NotNull(message = "收入金额不能为空")
    @DecimalMin(value = "0.01", message = "收入金额必须大于0")
    private BigDecimal amount;

    /**
     * 货币代码（默认USD）
     */
    private String currency = "USD";

    /**
     * 说明（最长500字符）
     */
    @Size(max = 500, message = "说明不能超过500个字符")
    private String description;
}
