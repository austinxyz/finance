package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountTrendDataPointDTO {
    private String date;  // Format: YYYY-MM-DD
    private BigDecimal balance;
    private String accountName;
}
