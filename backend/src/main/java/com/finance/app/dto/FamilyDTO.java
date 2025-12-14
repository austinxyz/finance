package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FamilyDTO {

    private Long id;
    private String familyName;
    private Boolean isDefault;
    private BigDecimal annualExpenses;
    private String expensesCurrency;
    private Integer emergencyFundMonths;
    private String financialGoals;
}
