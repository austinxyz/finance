package com.finance.app.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for AI-enhanced financial advice
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AIAdviceResponseDTO {

    /**
     * AI-generated advice text
     */
    private String advice;

    /**
     * Indicator whether AI API is configured and available
     */
    private boolean aiEnabled;

    /**
     * Any error message if AI generation failed
     */
    private String errorMessage;
}
