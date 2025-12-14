package com.finance.app.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class InvestmentMonthlyTrendDTO {
    private Integer month;
    private String period;
    private BigDecimal deposits;
    private BigDecimal withdrawals;
}
