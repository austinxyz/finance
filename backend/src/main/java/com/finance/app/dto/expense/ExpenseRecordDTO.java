package com.finance.app.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支出记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseRecordDTO {

    private Long id;

    private Long familyId;

    private Long userId;

    /**
     * 支出期间（YYYY-MM）
     */
    private String expensePeriod;

    /**
     * 支出年份
     */
    private Integer expenseYear;

    /**
     * 支出月份
     */
    private Integer expenseMonth;

    /**
     * 大类ID
     */
    private Long majorCategoryId;

    /**
     * 大类名称
     */
    private String majorCategoryName;

    /**
     * 大类图标
     */
    private String majorCategoryIcon;

    /**
     * 子分类ID
     */
    private Long minorCategoryId;

    /**
     * 子分类名称
     */
    private String minorCategoryName;

    /**
     * 支出金额（原币种）
     */
    private BigDecimal amount;

    /**
     * 货币代码
     */
    private String currency;

    /**
     * 支出类型：FIXED_DAILY / LARGE_IRREGULAR
     */
    private String expenseType;

    /**
     * 说明
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
