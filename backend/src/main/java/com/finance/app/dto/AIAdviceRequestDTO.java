package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for AI-enhanced financial advice
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIAdviceRequestDTO {

    /**
     * User's additional context, questions, or specific concerns
     * e.g., "我计划明年买房", "我的风险承受能力较低", "如何优化我的退休规划"
     */
    private String userContext;

    /**
     * Optional user ID to filter data
     */
    private Long userId;

    /**
     * Optional family ID to filter data
     */
    private Long familyId;
}
