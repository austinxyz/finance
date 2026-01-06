package com.finance.app.dto.income;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收入分类DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeCategoryDTO {

    private Long id;

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
     * 大类颜色
     */
    private String majorCategoryColor;

    /**
     * 小类ID（可选）
     */
    private Long minorCategoryId;

    /**
     * 小类名称（可选）
     */
    private String minorCategoryName;

    /**
     * 小类中文名称（可选）
     */
    private String minorCategoryChineseName;

    /**
     * 是否启用
     */
    private Boolean isActive;
}
