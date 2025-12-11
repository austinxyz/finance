package com.finance.app.dto.expense;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 支出分类DTO（包含大类和子分类）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpenseCategoryDTO {

    /**
     * 大类ID
     */
    private Long id;

    /**
     * 大类编码
     */
    private String code;

    /**
     * 大类名称
     */
    private String name;

    /**
     * 图标
     */
    private String icon;

    /**
     * 颜色
     */
    private String color;

    /**
     * 排序顺序
     */
    private Integer sortOrder;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 说明
     */
    private String description;

    /**
     * 子分类列表
     */
    private List<MinorCategoryDTO> minorCategories;

    /**
     * 子分类DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MinorCategoryDTO {
        private Long id;
        private Long majorCategoryId;
        private String name;
        private Boolean isActive;
        private Boolean isDefault;
        private Integer sortOrder;
        private String description;
        private String expenseType; // 支出类型（FIXED_DAILY/LARGE_IRREGULAR）
        private Integer recordCount; // 关联的记录数量
    }
}
