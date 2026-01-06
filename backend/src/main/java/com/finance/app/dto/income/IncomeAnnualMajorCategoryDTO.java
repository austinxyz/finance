package com.finance.app.dto.income;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomeAnnualMajorCategoryDTO {
    private Long majorCategoryId;
    private String majorCategoryName;
    private String majorCategoryChineseName;
    private String majorCategoryIcon;
    private BigDecimal totalAmount;
    private String currency;
}
