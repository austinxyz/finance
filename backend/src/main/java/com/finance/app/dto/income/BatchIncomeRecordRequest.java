package com.finance.app.dto.income;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 批量保存收入记录请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchIncomeRecordRequest {

    @NotNull(message = "家庭ID不能为空")
    private Long familyId;

    /**
     * 收入期间（YYYY-MM格式）
     */
    @NotBlank(message = "收入期间不能为空")
    @Pattern(regexp = "^\\d{4}-\\d{2}$", message = "收入期间格式必须为YYYY-MM")
    private String period;

    /**
     * 收入记录列表
     */
    @NotEmpty(message = "收入记录列表不能为空")
    private List<IncomeRecordItem> records;

    /**
     * 单条收入记录
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IncomeRecordItem {

        @NotNull(message = "大类ID不能为空")
        private Long majorCategoryId;

        /**
         * 小类ID（可选）
         */
        private Long minorCategoryId;

        /**
         * 关联的资产账户ID（可选）
         */
        private Long assetAccountId;

        @NotNull(message = "收入金额不能为空")
        @DecimalMin(value = "0.01", message = "收入金额必须大于0")
        private BigDecimal amount;

        private String currency = "USD";

        @Size(max = 500, message = "说明不能超过500个字符")
        private String description;
    }
}
