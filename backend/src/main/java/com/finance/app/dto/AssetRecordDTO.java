package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AssetRecordDTO {
    private Long id;
    private Long accountId;
    private String accountName;
    private LocalDate recordDate;
    private BigDecimal amount;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private String currency;
    private BigDecimal exchangeRate;
    private BigDecimal amountInBaseCurrency;
    private String notes;
    private String attachmentUrl;
}
