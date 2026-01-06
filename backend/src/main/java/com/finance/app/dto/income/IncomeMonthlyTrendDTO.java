package com.finance.app.dto.income;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomeMonthlyTrendDTO {
    private Integer month;
    private String period;
    private BigDecimal amount;
    private String currency;
}
