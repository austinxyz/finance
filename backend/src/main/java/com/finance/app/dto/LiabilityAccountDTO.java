package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LiabilityAccountDTO {
    private Long id;
    private Long userId;
    private String userName;  // 用户名称
    private Long categoryId;
    private String categoryName;
    private String categoryType;
    private String accountName;
    private String accountNumber;
    private String institution;
    private String currency;
    private BigDecimal interestRate;
    private BigDecimal originalAmount;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal monthlyPayment;
    private String notes;
    private Boolean isActive;
    private BigDecimal latestBalance;  // 最近记录余额（原币种）
    private BigDecimal latestBalanceInBaseCurrency;  // 最近记录余额（基准货币）
    private LocalDate latestRecordDate;  // 最近记录日期
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
