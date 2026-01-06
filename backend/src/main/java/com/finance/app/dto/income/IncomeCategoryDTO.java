package com.finance.app.dto.income;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 收入分类DTO（包含大类和小类嵌套结构）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeCategoryDTO {

    /**
     * 大类ID
     */
    private Long id;

    /**
     * 大类名称
     */
    private String name;

    /**
     * 大类中文名称
     */
    private String chineseName;

    /**
     * 大类图标
     */
    private String icon;

    /**
     * 大类颜色
     */
    private String color;

    /**
     * 显示顺序
     */
    private Integer displayOrder;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 小类列表
     */
    private List<MinorCategoryDTO> minorCategories;

    /**
     * 小类DTO
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MinorCategoryDTO {
        private Long id;
        private Long majorCategoryId;
        private String name;
        private String chineseName;
        private Boolean isActive;
        private Integer displayOrder;
        private Integer recordCount; // 关联的记录数量
    }
}
