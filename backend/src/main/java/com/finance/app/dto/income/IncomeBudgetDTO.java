package com.finance.app.dto.income;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收入预算DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IncomeBudgetDTO {

    private Long id;

    private Long familyId;

    private Long userId;

    private Long majorCategoryId;

    private String majorCategoryName;

    private String majorCategoryChineseName;

    private Long minorCategoryId;

    private String minorCategoryName;

    private String minorCategoryChineseName;

    private Integer year;

    private BigDecimal budgetedAmount;

    private String currency;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
