package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;
    private Long familyId;
    private String username;
    private String email;
    private String fullName;
    private LocalDate birthDate;
    private BigDecimal annualIncome;
    private String incomeCurrency;
    private String riskTolerance;
    private String notes;
    private Boolean isActive;
}
