package com.finance.app.dto.expense;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 创建支出记录请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateExpenseRecordRequest {

    @NotNull(message = "家庭ID不能为空")
    private Long familyId;

    /**
     * 记录人ID（可选）
     */
    private Long userId;

    /**
     * 支出期间（YYYY-MM格式）
     */
    @NotBlank(message = "支出期间不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "支出期间格式必须为YYYY-MM")
    private String expensePeriod;

    /**
     * 子分类ID
     */
    @NotNull(message = "子分类ID不能为空")
    private Long minorCategoryId;

    /**
     * 支出金额
     */
    @NotNull(message = "支出金额不能为空")
    @DecimalMin(value = "0.01", message = "支出金额必须大于0")
    private BigDecimal amount;

    /**
     * 货币代码（默认CNY）
     */
    private String currency = "CNY";

    /**
     * 支出类型：FIXED_DAILY / LARGE_IRREGULAR
     */
    @NotBlank(message = "支出类型不能为空")
    @Pattern(regexp = "^(FIXED_DAILY|LARGE_IRREGULAR)$",
        message = "支出类型必须为FIXED_DAILY或LARGE_IRREGULAR")
    private String expenseType;

    /**
     * 说明（最长200字符）
     */
    @Size(max = 200, message = "说明不能超过200个字符")
    private String description;
}
