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
public class AssetAccountDTO {
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
    private String notes;
    private Boolean isActive;
    private BigDecimal latestAmount;
    private LocalDate latestRecordDate;  // 最近记录日期
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
