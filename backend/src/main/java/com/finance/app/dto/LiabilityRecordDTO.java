package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiabilityRecordDTO {
    private Long id;
    private Long accountId;
    private String accountName;
    private LocalDate recordDate;
    private BigDecimal outstandingBalance;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal balanceInBaseCurrency;
    private BigDecimal paymentAmount;
    private BigDecimal principalPayment;
    private BigDecimal interestPayment;
    private String notes;
}
