package com.finance.app.dto.expense;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建子分类请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMinorCategoryRequest {

    /**
     * 所属大类ID
     */
    @NotNull(message = "大类ID不能为空")
    private Long majorCategoryId;

    /**
     * 子分类名称
     */
    @NotBlank(message = "子分类名称不能为空")
    @Size(max = 100, message = "子分类名称不能超过100个字符")
    private String name;

    /**
     * 排序顺序
     */
    private Integer sortOrder = 0;

    /**
     * 说明
     */
    @Size(max = 500, message = "说明不能超过500个字符")
    private String description;

    /**
     * 支出类型（FIXED_DAILY/LARGE_IRREGULAR）
     */
    private String expenseType = "FIXED_DAILY";
}
