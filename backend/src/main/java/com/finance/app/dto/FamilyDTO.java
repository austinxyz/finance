package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    private String description;
    private LocalDateTime createdAt;
    private List<UserDTO> members;
}
