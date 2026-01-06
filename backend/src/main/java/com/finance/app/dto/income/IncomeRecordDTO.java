package com.finance.app.dto.income;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收入记录DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeRecordDTO {

    private Long id;

    private Long familyId;

    /**
     * 关联的资产账户ID
     */
    private Long assetAccountId;

    /**
     * 资产账户名称
     */
    private String assetAccountName;

    /**
     * 收入期间（YYYY-MM）
     */
    private String period;

    /**
     * 收入年份
     */
    private Integer year;

    /**
     * 收入月份
     */
    private Integer month;

    /**
     * 大类ID
     */
    private Long majorCategoryId;

    /**
     * 大类名称
     */
    private String majorCategoryName;

    /**
     * 大类中文名称
     */
    private String majorCategoryChineseName;

    /**
     * 大类图标
     */
    private String majorCategoryIcon;

    /**
     * 小类ID
     */
    private Long minorCategoryId;

    /**
     * 小类名称
     */
    private String minorCategoryName;

    /**
     * 小类中文名称
     */
    private String minorCategoryChineseName;

    /**
     * 收入金额（税后实际到账）
     */
    private BigDecimal amount;

    /**
     * 货币代码
     */
    private String currency;

    /**
     * 换算成USD的金额
     */
    private BigDecimal amountUsd;

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
